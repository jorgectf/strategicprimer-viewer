import ceylon.collection {
    MutableList,
    ArrayList
}

import lovelace.util.common {
    todo,
    matchingValue,
    singletonRandom
}

import strategicprimer.model.common.map {
    River,
    TileType,
    TileFixture,
    Direction,
    HasOwner
}

import strategicprimer.model.common.map.fixtures.mobile.worker {
    WorkerStats,
    ISkill
}

import strategicprimer.model.common.map.fixtures.mobile {
    IUnit,
    IWorker
}

import strategicprimer.model.common.map.fixtures.terrain {
    Hill,
    Forest
}

import strategicprimer.model.common.map.fixtures.towns {
    ITownFixture
}

import ceylon.random {
    randomize
}

"""An encapsulation of the very basic movement model we currently use:

   - It is possible to move from a land tile to a land tile, and from one
     [[ocean|TileType.ocean]] tile to another, but not from land to water or
     vice versa.
   - Each unit has a certain number of "movement points" per turn, and each
     action it takes costs MP; how many MP it takes to move depends on the
     terrain type, whether the tile is mountainous, whether it has any forests,
     and whether there are any rivers going in approximately the same
     direction.
   - Units notice nearby [["fixtures"|TileFixture]] in the map (only on the
     tiles they visit, for now) as they move, some automatically and some with
     a probability dependent on the kind of fixture and on the unit's members'
     Perception score."""
shared object simpleMovementModel {
    "Whether land movement is possible on the given terrain."
    shared Boolean landMovementPossible(TileType terrain) => TileType.ocean != terrain;

    "Whether rivers in either the source or the destination will speed travel in the given
     direction." // "X in Y" means "Y.contains(X)"
    shared Boolean riversSpeedTravel(Direction direction,
            {River*} source, {River*} dest) {
        Boolean recurse(Direction partial) =>
                riversSpeedTravel(partial, source, dest);
        switch (direction)
        case (Direction.north) { return River.north in source || River.south in dest; }
        case (Direction.northeast) {
            return recurse(Direction.north) || recurse(Direction.east);
        }
        case (Direction.east) { return River.east in source || River.west in dest; }
        case (Direction.southeast) {
            return recurse(Direction.south) || recurse(Direction.east);
        }
        case (Direction.south) { return River.south in source || River.north in dest; }
        case (Direction.southwest) {
            return recurse(Direction.south) || recurse(Direction.west);
        }
        case (Direction.west) { return River.west in source || River.east in dest; }
        case (Direction.northwest) {
            return recurse(Direction.north) || recurse(Direction.west);
        }
        case (Direction.nowhere) { return false; }
    }

    "Get the cost of movement in the given conditions."
    // FIXME: Reduce cost when roads present (TODO: rebalance base costs?)
    shared Integer movementCost(
            """The terrain being traversed. Null if "not visible.""""
            TileType? terrain,
            "Whether the location is forested"
            Boolean forest,
            "Whether the location is mountainous"
            Boolean mountain,
            "Whether the location has a river that reduces cost"
            Boolean river,
            "The fixtures at the location"
            {TileFixture*} fixtures) {
        if (exists terrain) {
            if (TileType.ocean == terrain) {
                return runtime.maxArraySize;
            } else if (TileType.jungle == terrain || TileType.swamp == terrain) {
                return (river) then 4 else 6;
            } else if (forest || mountain || !fixtures.narrow<Hill>().empty ||
                    !fixtures.narrow<Forest>().filter(not(Forest.rows)).empty ||
		    TileType.desert == terrain) {
                return (river) then 2 else 3;
            } else {
                assert (TileType.steppe == terrain || TileType.plains == terrain ||
                    TileType.tundra == terrain);
                return (river) then 1 else 2;
            }
        } else {
            return runtime.maxArraySize;
        }
    }

    "Check whether a unit moving at the given relative speed might notice the given
     fixture. Units do not notice themselves and do not notice null fixtures."
    todo("We now check DCs on Events, but ignore relevant skills other than Perception.
          And now a lot more things have DCs for which those other skills are relevant.")
    shared Boolean shouldSometimesNotice(
            "The moving unit"
            HasOwner unit,
            "How fast the unit is moving"
            Speed speed,
            "The fixture the unit might be noticing"
            TileFixture? fixture) {
        if (exists fixture) {
            if (unit == fixture) {
                return false;
            } else {
                Integer perception;
                if (is IUnit unit) {
                    perception = highestPerception(unit);
                } else {
                    perception = 0;
                }
                return (perception + speed.perceptionModifier + 15) >= fixture.dc;
            }
        } else {
            return false;
        }
    }

    "Get the highest Perception score of any member of the unit"
    todo("This does not properly handle the unusual case of a very unobservant unit")
    Integer highestPerception(IUnit unit) =>
        unit.narrow<IWorker>().map(getPerception).max(increasing) else 0;
    "Get a worker's Perception score."
    Integer getPerception(IWorker worker) {
        Integer ability;
        if (exists stats = worker.stats) {
            ability = WorkerStats.getModifier(stats.wisdom);
        } else {
            ability = 0;
        }
        Integer ranks = worker.flatMap(identity)
            .filter(compose(matchingValue("perception", String.lowercased), ISkill.name))
            .map(ISkill.level).reduce(plus) else 0;
        return ability + (ranks * 2);
    }

    "Whether the unit should always notice the given fixture. A null fixture is never
     noticed."
    todo("""Very-observant units should "always" notice some things that others might
            "sometimes" notice.""")
    shared Boolean shouldAlwaysNotice(HasOwner unit, TileFixture? fixture) {
        if (is ITownFixture fixture) {
            return fixture.owner == unit.owner;
        } else {
            return fixture is Hill|Forest;
        }
    }

    "Choose what the mover should in fact find from the list of things he or she might
     find. Since some callers need to have a list of Pairs instead of TileFixtures, we
     take a function for getting the fixtures out of the list."
    shared {Element*} selectNoticed<Element>({Element*} possibilities,
            TileFixture(Element) getter, IUnit mover, Speed speed) {
        {Element*} local = randomize(possibilities);
        variable Integer perception = highestPerception(mover) + speed.perceptionModifier;
        MutableList<Element> retval = ArrayList<Element>();
        for (item in local) {
            Integer dc = getter(item).dc;
            if (singletonRandom.nextElement(1..20) + perception >= dc) {
                retval.add(item);
                perception -= 5;
            }
        }
        return retval.sequence();
    }
}
