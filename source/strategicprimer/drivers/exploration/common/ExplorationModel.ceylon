import ceylon.collection {
    ArrayList,
    MutableList
}
import ceylon.numeric.float {
    ceiling
}
import java.nio.file {
    JPath=Path
}

import strategicprimer.drivers.common {
    SimpleMultiMapModel,
    IDriverModel,
    SelectionChangeListener
}
import strategicprimer.model.map {
    Point,
    Player,
    IMutableMapNG,
    IMapNG,
    TileFixture,
    invalidPoint,
    pointFactory,
    IFixture,
    HasOwner,
    MapDimensions,
    TileType
}
import strategicprimer.model.map.fixtures {
    Ground,
    MineralFixture
}
import strategicprimer.model.map.fixtures.mobile {
    IUnit,
    Animal,
	MobileFixture
}
import strategicprimer.model.map.fixtures.resources {
    Grove,
    Meadow,
    Mine,
    MineralVein
}
import strategicprimer.model.map.fixtures.terrain {
    Forest
}
import strategicprimer.model.map.fixtures.towns {
    Village,
    Fortress
}
import ceylon.random {
    randomize
}
import lovelace.util.common {
	matchingPredicate,
	matchingValue
}
"A model for exploration drivers."
shared class ExplorationModel extends SimpleMultiMapModel satisfies IExplorationModel {
    """A fixture is "diggable" if it is a [[MineralFixture]] or a [[Mine]]."""
    static Boolean isDiggable(TileFixture fixture) => fixture is MineralFixture|Mine;
    """Check whether two fixtures are "equal enough" for the purposes of updating a map
       after digging. This method is needed because equals() in
       [[strategicprimer.model.map.fixtures.resources::StoneDeposit]] and
       [[strategicprimer.model.map.fixtures.resources::MineralVein]] compares DCs."""
    static Boolean areDiggablesEqual(IFixture firstFixture, IFixture secondFixture) =>
            firstFixture == secondFixture || firstFixture.copy(true) == secondFixture
                .copy(true);
    "If a unit's motion could be observed by someone allied to another (non-independent)
     player (which at present means the unit is moving *to* a tile two or fewer tiles away
     from the watcher), print a message saying so to stdout."
    static void checkAllNearbyWatchers(IMapNG map, IUnit unit, Point dest) {
        MapDimensions dimensions = map.dimensions;
        String description;
        if (unit.owner.independent) {
            description = "``unit.shortDescription`` (ID #``unit.id``)";
        } else {
            description = unit.shortDescription;
        }
        for (point in surroundingPointIterable(dest, dimensions).distinct) {
//            for (fixture in map.fixtures[point]) { // TODO: syntax sugar once compiler bug fixed
            for (fixture in map.fixtures.get(point).narrow<HasOwner>()) {
                if (!fixture.owner.independent, fixture.owner != unit.owner) {
                    process.writeLine("Motion of ``description`` to ``dest`` could be observed by ``
                            fixture.shortDescription`` at ``point``");
                }
            }
        }
    }
    "Remove a unit from a location, even if it's in a fortress."
    static void removeImpl(IMutableMapNG map, Point point, IUnit unit) {
        variable Boolean outside = false;
//        for (fixture in map.fixtures[point]) { // TODO: syntax sugar once compiler bug fixed
        for (fixture in map.fixtures.get(point)) {
            if (unit == fixture) {
                outside = true;
                break;
            } else if (is Fortress fixture, exists item = fixture.find(unit.equals)) {
                fixture.removeMember(item);
                return;
            }
        }
        if (outside) {
            map.removeFixture(point, unit);
        }
    }
    "Ensure that a given map has at least terrain information for the specified location."
    static void ensureTerrain(IMapNG mainMap, IMutableMapNG map, Point point) {
        if (!map.baseTerrain[point] exists) {
            map.baseTerrain[point] = mainMap.baseTerrain[point];
        }
//        if (mainMap.mountainous[point]) { // TODO: syntax sugar once compiler bug fixed
        if (mainMap.mountainous.get(point)) {
            map.mountainous[point] = true;
        }
        //map.addRivers(point, *mainMap.rivers[point]); // TODO: syntax sugar
        map.addRivers(point, *mainMap.rivers.get(point));
    }
    "Whether the given fixture is contained in the given stream."
    static Boolean doesStreamContainFixture({IFixture*} stream, IFixture fixture) {
        for (member in stream) {
            if (member == fixture) {
                return true;
            } else if (is {IFixture*} member, doesStreamContainFixture(member, fixture)) {
                return true;
            }
        }
        return false;
    }
    "Whether the given fixture is at the given location in the given map."
    static Boolean doesLocationHaveFixture(IMapNG map, Point point, TileFixture fixture) {
        if (exists fixtures = map.fixtures[point]) {
            for (member in fixtures) {
                if (member == fixture) {
                    return true;
                } else if (is {IFixture*} member, doesStreamContainFixture(member, fixture)) {
                    return true;
                }
            }
        }
        return false;
    }
    """A "plus one" method with a configurable, low "overflow"."""
    static Integer increment(
            "The number to increment"
            Integer number,
            "The maximum number we want to return"
            Integer max) => if (number >= max) then 0 else number + 1;
    """A "minus one" method that "underflows" after 0 to a configurable, low value."""
    static Integer decrement(
            "The number to decrement"
            Integer number,
            """The number to "underflow" to"""
            Integer max) => if (number <= 0) then max else number - 1;
    MutableList<MovementCostListener> mcListeners = ArrayList<MovementCostListener>();
    MutableList<SelectionChangeListener> scListeners =
            ArrayList<SelectionChangeListener>();
    "The currently selected unit and its location."
    variable [Point, IUnit?] selection = [invalidPoint, null];
    shared new (IMutableMapNG map, JPath? file)
            extends SimpleMultiMapModel(map, file) {}
    shared new copyConstructor(IDriverModel model)
            extends SimpleMultiMapModel.copyConstructor(model) {}
    "All the players shared by all the maps."
    shared actual {Player*} playerChoices => allMaps.map(Tuple.first).map(IMapNG.players).map(set)
                .fold(set(map.players))((one, two) => one.intersection(two));
    "Collect all the units in the main map belonging to the specified player."
    shared actual {IUnit*} getUnits(Player player) {
//        return map.locations.flatMap((point) => map.fixtures[point]) // TODO: syntax sugar once compiler bug fixed
        return map.locations.flatMap((point) => map.fixtures.get(point))
            .flatMap((element) {
                if (is Fortress element) {
                    return element;
                } else {
                    return Singleton(element);
                }
            }).narrow<IUnit>().filter(matchingValue(player, HasOwner.owner));
    }
    "Tell listeners that the selected point changed."
    void fireSelectionChange(Point old, Point newSelection) {
        for (listener in scListeners) {
            listener.selectedPointChanged(old, newSelection);
        }
    }
    "Tell listeners to deduct a cost from their movement-point totals."
    void fireMovementCost(Integer cost) {
        for (listener in mcListeners) {
            listener.deduct(cost);
        }
    }
    "Get the location one tile in the given direction from the given point."
    shared actual Point getDestination(Point point, Direction direction) {
        MapDimensions dims = mapDimensions;
        Integer maxColumn = dims.columns - 1;
        Integer maxRow = dims.rows - 1;
        Integer row = point.row;
        Integer column = point.column;
        switch (direction)
        case (Direction.east) { return pointFactory(row, increment(column, maxColumn)); }
        case (Direction.north) { return pointFactory(decrement(row, maxRow), column); }
        case (Direction.northeast) {
            return pointFactory(decrement(row, maxRow), increment(column, maxColumn));
        }
        case (Direction.northwest) {
            return pointFactory(decrement(row, maxRow), decrement(column, maxColumn));
        }
        case (Direction.south) { return pointFactory(increment(row, maxRow), column); }
        case (Direction.southeast) {
            return pointFactory(increment(row, maxRow), increment(column, maxColumn));
        }
        case (Direction.southwest) {
            return pointFactory(increment(row, maxRow), decrement(column, maxColumn));
        }
        case (Direction.west) { return pointFactory(row, decrement(column, maxColumn)); }
        case (Direction.nowhere) { return point; }
    }
    void fixMovedUnits(Point base) {
		{<Point->TileFixture>*} localFind(IMapNG mapParam, TileFixture target) => {
				for (point in mapParam.locations)
					for (fixture in mapParam.fixtures.get(point).filter(target.equals)) // TODO: syntax sugar once bug fixed
						point->target
			};
        // TODO: Unit vision range
        {Point*} points = surroundingPointIterable(base, map.dimensions, 2);
        for ([submap, file] in subordinateMaps) {
            for (point in points) {
                for (fixture in submap.fixtures.get(point).narrow<MobileFixture>()) { // TODO: syntax sugar once bug fixed
                    if (is Animal fixture, fixture.traces) {
                        continue;
                    }
                    for (innerPoint->match in localFind(submap, fixture)) {
                        if (innerPoint != point, !map.fixtures.get(innerPoint).contains(match)) {// TODO: syntax sugar
                            submap.removeFixture(innerPoint, match);
                        }
                    }
                }
            }
        }
    }
    "Move the currently selected unit from its current location one tile in the specified
     direction. Moves the unit in all maps where the unit *was* in that tile, copying
     terrain information if the tile didn't exist in a subordinate map. If movement in the
     specified direction is impossible, we update all subordinate maps with the terrain
     information showing that, then re-throw the exception; callers should deduct a
     minimal MP cost (though we notify listeners of that cost). We return the cost of the
     move in MP, which we also tell listeners about."
    throws(`class TraversalImpossibleException`,
        "if movement in the specified direction is impossible")
    shared actual Integer move(
            "The direction to move"
            Direction direction,
            "How hastily the explorer is moving"
            Speed speed) {
        [Point, IUnit?] local = selection;
        Point point = local.first;
        assert (exists unit = local.rest.first);
        Point dest = getDestination(point, direction);
        if (exists terrain = map.baseTerrain[dest], exists startingTerrain = map.baseTerrain[point],
                    ((simpleMovementModel.landMovementPossible(terrain) && startingTerrain != TileType.ocean) ||
                        (startingTerrain == TileType.ocean && terrain == TileType.ocean))) {
            Integer base;
            if (dest == point) {
                base = 1;
            } else {
//                {TileFixture*} fixtures = map.fixtures[dest]; // TODO: syntax sugar once compiler bug fixed
                {TileFixture*} fixtures = map.fixtures.get(dest);
//                base = movementCost(map.baseTerrain[dest],
                base = simpleMovementModel.movementCost(map.baseTerrain.get(dest),
                    map.fixtures[dest]?.narrow<Forest>()?.first exists,
//                    map.mountainous[dest], riversSpeedTravel(direction, map.rivers[point],
                    map.mountainous.get(dest), simpleMovementModel.riversSpeedTravel(direction, map.rivers.get(point),
//                        map.rivers[dest]), fixtures);
                        map.rivers.get(dest)), fixtures);
            }
            Integer retval = (ceiling(base * speed.mpMultiplier) + 0.1).integer;
            removeImpl(map, point, unit);
            map.addFixture(dest, unit);
            for ([subMap, subFile] in subordinateMaps) {
                if (doesLocationHaveFixture(subMap, point, unit)) {
                    ensureTerrain(map, subMap, dest);
                    removeImpl(subMap, point, unit);
                    subMap.addFixture(dest, unit);
                }
            }
            selection = [dest, unit];
            fireSelectionChange(point, dest);
            fireMovementCost(retval);
            checkAllNearbyWatchers(map, unit, dest);
            fixMovedUnits(dest);
            return retval;
        } else {
            if (!map.baseTerrain[point] exists) {
                log.trace("Started outside explored territory in main map");
            } else if (!map.baseTerrain[dest] exists) {
                log.trace("Main map doesn't have terrain for destination");
            } else {
                assert (exists terrain = map.baseTerrain[dest], exists startingTerrain = map.baseTerrain[point]);
                if ((simpleMovementModel.landMovementPossible(terrain) && startingTerrain == TileType.ocean)) {
                    log.trace("Starting in ocean, trying to get to ``terrain``");
                } else if (startingTerrain == TileType.ocean, terrain != TileType.ocean) {
                    log.trace("Land movement not possible, starting in ocean, trying to get to ``terrain``");
                } else if (startingTerrain != TileType.ocean, terrain == TileType.ocean) {
                    log.trace("Starting in ``startingTerrain``, trying to get to ocean");
                } else {
                    log.trace("Unknown reason for movement-impossible condition");
                }
            }
            for (pair in subordinateMaps) {
                ensureTerrain(map, pair.first, dest);
            }
            fireMovementCost(1);
            throw TraversalImpossibleException();
        }
    }
    """Search the main map for the given fixture. Returns the first location found (search
       order is not defined) containing a fixture "equal to" the specified one."""
    shared actual Point find(TileFixture fixture) {
        for (point in map.locations) {
            if (doesLocationHaveFixture(map, point, fixture)) {
                return point;
            }
        } else {
            return invalidPoint;
        }
    }
    "The currently selected unit."
    shared actual IUnit? selectedUnit => selection.rest.first;
    "Select the given unit."
    assign selectedUnit {
        Point oldLoc = selection.first;
        Point loc;
        if (exists selectedUnit) {
            log.trace("Setting a newly selected unit");
            loc = find(selectedUnit);
            if (loc.valid) {
                log.trace("Found at ``loc``");
            } else {
                log.trace("Not found using our 'find' method");
            }
        } else {
            log.trace("Unsetting currently-selected-unit property");
            loc = invalidPoint;
        }
        selection = [loc, selectedUnit];
        fireSelectionChange(oldLoc, loc);
    }
    "The location of the currently selected unit."
    shared actual Point selectedUnitLocation => selection.first;
    "Add a selection-change listener."
    shared actual void addSelectionChangeListener(SelectionChangeListener listener) =>
            scListeners.add(listener);
    "Remove a selection-change listener."
    shared actual void removeSelectionChangeListener(SelectionChangeListener listener) =>
            scListeners.remove(listener);
    "Add a movement-cost listener."
    shared actual void addMovementCostListener(MovementCostListener listener) =>
            mcListeners.add(listener);
    "Remove a movement-cost listener."
    shared actual void removeMovementCostListener(MovementCostListener listener) =>
            mcListeners.remove(listener);
    "If there is a currently selected unit, make any independent villages at its location
     change to be owned by the owner of the currently selected unit. This costs MP."
    shared actual void swearVillages() {
        [Point, IUnit?] localSelection = selection;
        Point currentPoint = localSelection.first;
        if (exists unit = localSelection.rest.first) {
            Player owner = unit.owner;
            {Village*} villages = allMaps.map((pair) => pair.first)
//                .flatMap((world) => world.fixtures[currentPoint]) // TODO: syntax sugar once compiler bug fixed
                .flatMap((world) => world.fixtures.get(currentPoint))
                .narrow<Village>().filter(matchingPredicate(Player.independent, Village.owner));
            if (!villages.empty) {
                variable Boolean subordinate = false;
                for (village in villages) {
                    village.owner = owner;
                    for (pair in allMaps) {
                        pair.first.addFixture(currentPoint, village.copy(subordinate));
                        subordinate = true;
                    }
                }
                IMapNG mainMap = map;
                {Point*} surroundingPoints =
                        surroundingPointIterable(currentPoint, mapDimensions, 1);
                for (point in surroundingPoints) {
                    for (pair in subordinateMaps) {
                        ensureTerrain(mainMap, pair.first, point);
                        Forest? subForest =
                                pair.first.fixtures[point]?.narrow<Forest>()?.first;
                        if (exists forest = map.fixtures[point]?.narrow<Forest>()?.first,
                                !subForest exists) {
                            pair.first.addFixture(point, forest);
                        }
                    }
                }
                {[Point, TileFixture]*} surroundingFixtures = surroundingPoints
//                            .flatMap((point) => mainMap.fixtures[point] // TODO: syntax sugar once compiler bug fixed
                            .flatMap((point) => mainMap.fixtures.get(point)
                                .map((fixture) => [point, fixture]));
                [Point, TileFixture]? vegetation = surroundingFixtures
                        .narrow<[Point, Meadow|Grove]>().first;
                [Point, TileFixture]? animal = surroundingFixtures
                        .narrow<[Point, Animal]>().first;
                for (pair in subordinateMaps) {
                    if (exists vegetation) {
                        pair.first.addFixture(vegetation.first,
                            vegetation.rest.first.copy(true));
                    }
                    if (exists animal) {
                        pair.first.addFixture(animal.first, animal.rest.first.copy(true));
                    }
                }
            }
            fireMovementCost(5);
        }
    }
    "If there is a currently selected unit, change one [[Ground]],
     [[strategicprimer.model.map.fixtures.resources::StoneDeposit]], or [[MineralVein]] at
     the location of that unit from unexposed to exposed (and discover it). This costs
     MP."
    shared actual void dig() {
        Point currentPoint = selection.first;
        if (currentPoint.valid) {
            IMutableMapNG mainMap = map;
            variable {TileFixture*} diggables =
//                    mainMap.fixtures[currentPoint].filter(isDiggable); // TODO: syntax sugar once compiler bug fixed
                    mainMap.fixtures.get(currentPoint).filter(isDiggable);
            if (diggables.empty) {
                return;
            }
            variable Integer i = 0;
            variable Boolean first = true;
            while (first || (i < 4 && !diggables.first is Ground)) {
                diggables = randomize(diggables);
                first = false;
                i++;
            }
            assert (exists oldFixture = diggables.first);
            TileFixture newFixture = oldFixture.copy(false);
            if (is Ground newFixture) {
                newFixture.exposed = true;
            } else if (is MineralVein newFixture) {
                newFixture.exposed = true;
            }
            void addToMap(IMutableMapNG map, Boolean condition) {
//                if (map.fixtures[currentPoint] // TODO: syntax sugar once compiler bug fixed
                if (map.fixtures.get(currentPoint)
                        .any((fixture) => areDiggablesEqual(fixture, oldFixture))) {
                    map.removeFixture(currentPoint, oldFixture);
                }
                map.addFixture(currentPoint, newFixture.copy(condition));
            }
            variable Boolean subsequent = false;
            for (pair in allMaps) {
                addToMap(pair.first, subsequent);
                subsequent = true;
            }
            fireMovementCost(4);
        }
    }
}
