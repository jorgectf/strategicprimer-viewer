import ceylon.collection {
    MutableList,
    ArrayList,
    ListMutator,
    MutableMap,
	naturalOrderTreeMap
}
import ceylon.test {
    test,
    assertEquals
}

import java.nio.file {
    JPath=Path
}

import strategicprimer.model.map {
    Point,
    Player,
    TileFixture,
    IMutableMapNG,
    PlayerImpl,
    SPMapNG,
    MapDimensionsImpl,
    PlayerCollection
}
import strategicprimer.model.map.fixtures.mobile {
    ProxyFor,
    ProxyUnit,
    IUnit,
    Unit,
	AnimalImpl
}
import strategicprimer.model.map.fixtures.terrain {
    Oasis,
    Hill,
    Forest
}
import strategicprimer.model.map.fixtures.towns {
    TownSize,
    Fortress
}
import strategicprimer.drivers.common {
    SimpleMultiMapModel,
    IDriverModel
}
import ceylon.logging {
    Logger,
    logger
}
import ceylon.random {
    randomize
}
import lovelace.util.common {
	anythingEqual,
	matchingValue,
	matchingPredicate,
	comparingOn
}
Logger log = logger(`module strategicprimer.drivers.worker.common`);
"A model to underlie the advancement GUI, etc."
shared class WorkerModel extends SimpleMultiMapModel satisfies IWorkerModel {
	static {Anything*} flatten(Anything fixture) {
		if (is Fortress fixture) {
			return fixture;
		} else {
			return Singleton(fixture);
		}
	}
    variable Player? currentPlayerImpl = null;
    shared new (IMutableMapNG map, JPath? file)
            extends SimpleMultiMapModel(map, file) {}
    shared new copyConstructor(IDriverModel model)
            extends SimpleMultiMapModel.copyConstructor(model) {}
    shared actual Player currentPlayer {
        if (exists temp = currentPlayerImpl) {
            return temp;
        } else {
            for ([localMap, _] in allMaps) {
                Player temp = localMap.currentPlayer;
                if (!getUnits(temp).empty) {
                    currentPlayerImpl = temp;
                    return temp;
                }
            } else {
                currentPlayerImpl = map.currentPlayer;
                return map.currentPlayer;
            }
        }
    }
    assign currentPlayer {
        currentPlayerImpl = currentPlayer;
    }
    "Flatten and filter the stream to include only units, and only those owned by the
     given player."
    {IUnit*} getUnitsImpl({Anything*} iter, Player player) =>
            iter.flatMap(flatten).narrow<IUnit>()
	            .filter(matchingPredicate(matchingValue(player.playerId, Player.playerId), IUnit.owner));
    "All the players in all the maps."
    shared actual {Player*} players => allMaps.map(Tuple.first).flatMap(IMutableMapNG.players).distinct;
    "Get all the given player's units, or only those of a specified kind."
    shared actual {IUnit*} getUnits(Player player, String? kind) {
        if (exists kind) {
            return getUnits(player).filter(matchingValue(kind, IUnit.kind));
        } else if (subordinateMaps.empty) {
            // Just in case I missed something in the proxy implementation, make sure
            // things work correctly when there's only one map.
            return getUnitsImpl(map.locations.flatMap(map.fixtures.get), player)
                .sort(comparingOn(IUnit.name, comparingOn(String.lowercased, increasing<String>)));
        } else {
            value temp = allMaps.map(Tuple.first)
                    .flatMap((indivMap) => indivMap.locations.flatMap(
//                        (point) => getUnitsImpl(indivMap.fixtures[point], player)));
                        (point) => getUnitsImpl(indivMap.fixtures.get(point), player)));
            MutableMap<Integer, IUnit&ProxyFor<IUnit>> tempMap =
                    naturalOrderTreeMap<Integer, IUnit&ProxyFor<IUnit>>([]);
            for (unit in temp) {
                Integer key = unit.id;
                ProxyFor<IUnit> proxy;
                if (exists item = tempMap[key]) {
                    proxy = item;
                } else {
                    value newProxy = ProxyUnit.fromParallelMaps(key);
                    tempMap[key] = newProxy;
                    proxy = newProxy;
                }
                proxy.addProxied(unit);
            }
            return tempMap.items.sort(comparingOn(IUnit.name,
                comparingOn(String.lowercased, increasing<String>)));
        }
    }
    """All the "kinds" of units the given player has."""
    shared actual {String*} getUnitKinds(Player player) =>
            getUnits(player).map(IUnit.kind).distinct
                .sort(comparingOn(String.lowercased, increasing<String>));
    "Add the given unit at the given location in all maps."
    void addUnitAtLocation(IUnit unit, Point location) {
        void impl(IMutableMapNG map) {
//            for (fixture in map.fixtures[location]) { // TODO: syntax sugar once compiler bug fixed
            for (fixture in map.fixtures.get(location)) { // TODO: Can this be condensed using Iterable.narrow().any() and matchingValue()?
                if (is Fortress fixture, fixture.owner == unit.owner) {
                    fixture.addMember(unit.copy(false));
                    return;
                }
            } else {
                map.addFixture(location, unit.copy(false));
            }
        }
        if (subordinateMaps.empty) {
            impl(map);
        } else {
            allMaps.map(Tuple.first).each(impl);
        }
    }
    "Add a unit to all the maps, at the location of its owner's HQ in the main map."
    shared actual void addUnit(IUnit unit) {
        variable [Fortress, Point]? temp = null;
        for (point in map.locations) {
            //            for (fixture in map.fixtures[point].narrow<Fortress>().filter(matchingPredicate(matchingValue(unit.owner.playerId, Player.playerId), Fortress.owner))) { // TODO: syntax sugar once compiler bug fixed
            for (fixture in map.fixtures.get(point).narrow<Fortress>().filter(matchingPredicate(matchingValue(unit.owner.playerId, Player.playerId), Fortress.owner))) {
                if ("HQ" == fixture.name) {
                    addUnitAtLocation(unit, point);
                    return;
                } else if (!temp exists) {
                    temp = [fixture, point];
                }
            }
        } else {
            if (exists [fortress, loc] = temp) {
                log.info("Added unit at fortress ``fortress.name``, not HQ");
                addUnitAtLocation(unit, loc);
                return;
            } else if (!unit.owner.independent) {
                log.warn("No suitable location found for unit ``unit.name``, owned by ``unit.owner``");
            }
        }
    }
    "Get a unit by its owner and ID."
    shared actual IUnit? getUnitByID(Player owner, Integer id) =>
            getUnits(owner).find(matchingValue(id, IUnit.id));
}
object workerModelTests {
	"Helper method: Flatten any proxies in the list by replacing them with what they are
	 proxies for."
	{T*} filterProxies<T>(T* list) given T satisfies Object {
	    MutableList<T> retval = ArrayList<T>();
	    for (item in list) {
	        if (is ProxyFor<out T> item) {
	            retval.addAll(item.proxied);
	        } else {
	            retval.add(item);
	        }
	    }
	    return retval.sequence();
	}

	"Helper method: Add an item to multiple lists at once."
	void addItem<T>(T item, ListMutator<T>* lists) {
		for (list in lists) {
			list.add(item);
		}
	}

	test
	shared void testGetUnits() {
	    MutableList<TileFixture> fixtures = ArrayList<TileFixture>();
	    fixtures.add(Oasis(14));
	    fixtures.add(AnimalImpl("animal", false, "wild", 1));
	    MutableList<IUnit> listOne = ArrayList<IUnit>();
	    Player playerOne = PlayerImpl(0, "player1");
	    addItem(Unit(playerOne, "one", "unitOne", 2), fixtures, listOne);
	    MutableList<IUnit> listTwo = ArrayList<IUnit>();
	    Player playerTwo = PlayerImpl(1, "player2");
	    addItem(Unit(playerTwo, "two", "unitTwo", 3), fixtures, listTwo);
	    Player playerThree = PlayerImpl(2, "player3");
	    Player playerFour = PlayerImpl(3, "player4");
	    Fortress fort = Fortress(playerFour, "fort", 4, TownSize.small);
	    IUnit unit = Unit(playerThree, "three", "unitThree", 5);
	    fort.addMember(unit);
	    MutableList<IUnit> listThree = ArrayList<IUnit>();
	    listThree.add(unit);
	    fixtures.add(fort);
	    fixtures.add(Forest("forest", false, 10));
	    fixtures.add(Hill(7));
	    addItem(Unit(playerOne, "four", "unitFour", 6), fixtures, listOne);
	    fixtures.add(Oasis(8));
	    value shuffled = randomize(fixtures);
	    IMutableMapNG map = SPMapNG(MapDimensionsImpl(3, 3, 2), PlayerCollection(), -1);
	    for ([point, fixture] in zipPairs(map.locations, shuffled)) {
	        map.addFixture(point, fixture);
	    }
	    IWorkerModel model = WorkerModel(map, null);
	    Boolean iterableEquality<T>(Anything one, Anything two) given T satisfies Object {
	        if (is {T*} one, is {T*} two) {
	            return one.containsEvery(two) &&two.containsEvery(one);
	        } else {
	            return anythingEqual(one, two);
	        }
	    }
	    assertEquals(filterProxies(*model.getUnits(playerOne)),
	        listOne, "Got all units for player 1", iterableEquality<IUnit>);
	    assertEquals(filterProxies(*model.getUnits(playerTwo)), listTwo,
	        "Got all units for player 2", iterableEquality<IUnit>);
	    assertEquals(filterProxies(*model.getUnits(playerThree)), listThree,
	        "Got all units for player 3", iterableEquality<IUnit>);
	}
}