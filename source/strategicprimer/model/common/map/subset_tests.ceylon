import ceylon.test {
    assertTrue,
    test,
    assertFalse,
    assertEquals,
    assertNotEquals
}

import strategicprimer.model.common.map {
    Subsettable,
    TileFixture,
    PlayerImpl,
    IFixture,
    Player,
    Point,
    River,
    TileType,
    IPlayerCollection,
    IMutablePlayerCollection,
    PlayerCollection,
    MapDimensionsImpl,
    IMutableMapNG,
    IMapNG,
    SPMapNG
}
import strategicprimer.model.common.map.fixtures {
    TextFixture
}
import strategicprimer.model.common.map.fixtures.mobile {
    Worker,
    Unit,
    AnimalImpl,
    AnimalTracks
}
import strategicprimer.model.common.map.fixtures.mobile.worker {
    Job
}
import strategicprimer.model.common.map.fixtures.resources {
    CacheFixture
}
import strategicprimer.model.common.map.fixtures.terrain {
    Forest
}
import strategicprimer.model.common.map.fixtures.towns {
    TownSize,
    FortressImpl,
    IFortress,
    IMutableFortress,
    AbstractTown,
    TownStatus,
    Fortification,
    Town,
    City,
    CommunityStats
}
import lovelace.util.common {
    enumeratedParameter
}

"A collection of tests of the subset-checking features."
object subsetTests {
    """Assert that [[two]] is a "strict subset," by our loose definition, of
       [[one]]."""
    void assertIsSubset<SpecificType,GeneralType=SpecificType>(
                SpecificType&GeneralType one, SpecificType&GeneralType two,
                String message)
            given SpecificType satisfies Subsettable<GeneralType>
            given GeneralType satisfies Object =>
                assertTrue(one.isSubset(two, noop), message);
    """Assert that [[two]] is *not* a "strict subset," even by our loose
       definition, of [[one]]."""
    void assertNotSubset<SpecificType,GeneralType=SpecificType>(
                SpecificType&GeneralType one, SpecificType&GeneralType two,
                String message)
            given SpecificType satisfies Subsettable<GeneralType>
            given GeneralType satisfies Object =>
                assertFalse(one.isSubset(two, noop), message);

    "A test of [[PlayerCollection]]'s subset feature"
    test
    shared void testPlayerCollectionSubset() {
	// TODO: Make a way to initialize one declaratively
        IMutablePlayerCollection firstCollection = PlayerCollection();
        firstCollection.add(PlayerImpl(1, "one"));
        IMutablePlayerCollection secondCollection = PlayerCollection();
        secondCollection.add(PlayerImpl(1, "one"));
        secondCollection.add(PlayerImpl(2, "two"));
        IPlayerCollection zero = PlayerCollection();
        assertIsSubset(zero, zero, "Empty is subset of self");
        assertIsSubset(firstCollection, zero, "Empty is subset of one");
        assertIsSubset(secondCollection, zero, "Empty is subset of two");
        assertNotSubset(zero, firstCollection, "One is not subset of empty");
        assertIsSubset<IPlayerCollection, {Player*}>(firstCollection, firstCollection,
            "One is subset of self");
        assertIsSubset<IPlayerCollection, {Player*}>(secondCollection, firstCollection,
            "One is subset of two");
        assertNotSubset(zero, secondCollection, "Two is not subset of empty");
        assertNotSubset<IPlayerCollection, {Player*}>(firstCollection, secondCollection,
            "Two is not subset of one");
        assertIsSubset<IPlayerCollection, {Player*}>(secondCollection, secondCollection,
            "Two is subset of self");
    }

    "Assert that neither of two [[fortresses|IFortress]] is a subset of the
     other."
    void requireMatching(IFortress one, IFortress two, String what) {
        assertNotSubset<IFortress, IFixture>(one, two,
            "Subset requires ``what``, first test");
        assertNotSubset<IFortress, IFixture>(two, one,
            "Subset requires ``what``, second test");
    }

    "A test of [[a fortress's|IFortress]] subset feature."
    test
    shared void testFortressSubset() {
        Integer fortId = 1;
        IFortress firstFort = FortressImpl(PlayerImpl(1, "one"), "fOne", fortId,
            TownSize.small);
        requireMatching(firstFort,
            FortressImpl(PlayerImpl(2, "two"), "fOne", fortId, TownSize.small), "same owner");
        requireMatching(firstFort,
            FortressImpl(PlayerImpl(1, "one"), "fTwo", fortId, TownSize.small), "same name");
        requireMatching(firstFort,
            FortressImpl(PlayerImpl(1, "one"), "fOne", 3, TownSize.small),
            "identity or ID equality");
        IMutableFortress fifthFort = FortressImpl(PlayerImpl(1, "one"), "fOne", fortId,
            TownSize.small);
        fifthFort.addMember(Unit(PlayerImpl(2, "two"), "unit_type", "unit_name", 4));
        assertIsSubset<IFortress, IFixture>(fifthFort, firstFort,
            "Fortress without is a subset of fortress with unit");
        assertNotSubset<IFortress, IFixture>(firstFort, fifthFort,
            "Fortress with is not a subset of fortress without unit");
        IFortress sixthFort = FortressImpl(PlayerImpl(1, "one"), "unknown", fortId,
            TownSize.small);
        assertIsSubset<IFortress, IFixture>(firstFort, sixthFort,
            """Fortress named "unknown" can be subset""");
        assertNotSubset<IFortress, IFixture>(sixthFort, firstFort,
            """"unknown" is not commutative""");
        IFortress seventhFort = FortressImpl(PlayerImpl(1, "one"), "unknown", fortId,
            TownSize.medium);
        assertNotSubset<IFortress, IFixture>(sixthFort, seventhFort,
            "Different size breaks Fortress subset");
        assertNotSubset<IFortress, IFixture>(seventhFort, sixthFort,
            "Different size breaks Fortress subset");
    }

    "Create a map with the given terrain."
    IMutableMapNG createMap(<Point->TileType>* terrain) {
        IMutableMapNG retval = SPMapNG(MapDimensionsImpl(2, 2, 2),
            PlayerCollection(), -1);
        for (point->type in terrain) {
            retval.baseTerrain[point] = type;
        }
        return retval;
    }

    "Test the [[IMapNG]] subset feature"
    test
    shared void testMapSubset() {
        IMutableMapNG firstMap = createMap(Point(0, 0)->TileType.jungle);
        IMutableMapNG secondMap = createMap(Point(0, 0)->TileType.jungle,
            Point(1, 1)->TileType.ocean);
        IMapNG zero = createMap();
        assertIsSubset(zero, zero, "None is a subset of itself");
        assertIsSubset(firstMap, zero, "None is a subset of one");
        assertIsSubset(secondMap, zero, "None is a subset of one");
        assertNotSubset(zero, firstMap, "One is not a subset of none");
        assertIsSubset(firstMap, firstMap, "One is a subset of itself");
        assertIsSubset(secondMap, firstMap, "One is a subset of two");
        assertNotSubset(zero, secondMap, "Two is not a subset of none");
        assertNotSubset(firstMap, secondMap, "Two is not a subset of one");
        assertIsSubset(secondMap, secondMap, "Two is a subset of itself");
        firstMap.baseTerrain[Point(1, 1)] = TileType.plains;
        assertNotSubset(secondMap, firstMap,
            "Corresponding but non-matching tile breaks subset");
        assertNotSubset(firstMap, secondMap,
            "Corresponding but non-matching tile breaks subset");
        secondMap.baseTerrain[Point(1, 1)] = TileType.plains;
        assertIsSubset(secondMap, firstMap, "Subset again after resetting terrain");
        firstMap.addFixture(Point(1, 1), CacheFixture("category", "contents", 3));
        assertIsSubset(secondMap, firstMap, "Subset calculation ignores caches");
        firstMap.addFixture(Point(1, 1), TextFixture("text", -1));
        assertIsSubset(secondMap, firstMap, "Subset calculation ignores text fixtures");
        firstMap.addFixture(Point(1, 1), AnimalTracks("animal"));
        assertIsSubset(secondMap, firstMap, "Subset calculation ignores animal tracks");
        firstMap.addFixture(Point(1, 1), AnimalImpl("animal", true, "status", 7));
        assertNotSubset(secondMap, firstMap,
            "Subset calculation does not ignore other fixtures");
    }

    "Test map subset calculations to ensure that off-by-one errors are caught."
    test
    shared void testMapOffByOne() {
        IMutableMapNG baseMap = SPMapNG(MapDimensionsImpl(2, 2, 2),
            PlayerCollection(), -1);
        for (point in baseMap.locations) {
            baseMap.baseTerrain[point] = TileType.plains;
        }
        baseMap.addFixture(Point(1, 1), Forest("elm", false, 1));
        baseMap.addFixture(Point(1, 1), AnimalImpl("skunk", false, "wild", 2));
        baseMap.addRivers(Point(1, 1), River.east);

        IMutableMapNG testMap = SPMapNG(MapDimensionsImpl(2, 2, 2),
            PlayerCollection(), -1);
        for (point in testMap.locations) {
            testMap.baseTerrain[point] = TileType.plains;
        }
        Forest forest = Forest("elm", false, 1);
        TileFixture animal = AnimalImpl("skunk", false, "wild", 2);
        for (point in testMap.locations) {
            assertIsSubset(baseMap, testMap,
                "Subset invariant before attempt using ``point``");
            Anything(IMapNG, IMapNG, String) testMethod;
            String result;
            if (Point(1, 1) == point) {
                testMethod = assertIsSubset<IMapNG>;
                result = "Subset holds when fixture(s) placed correctly";
            } else {
                testMethod = assertNotSubset<IMapNG>;
                result = "Subset fails when fixture(s) off by one";
            }
            testMap.addFixture(point, forest);
            testMethod(baseMap, testMap, result);
            testMap.removeFixture(point, forest);
            testMap.addFixture(point, animal);
            testMethod(baseMap, testMap, result);
            testMap.removeFixture(point, animal);
            testMap.addRivers(point, River.east);
            testMethod(baseMap, testMap, result);
            testMap.removeRivers(point, River.east);
            assertIsSubset(baseMap, testMap,
                "Subset invariant after attempt using ``point``");
        }
    }

    "Test interaction between isSubset() and copy()."
    test
    shared void testSubsetsAndCopy() {
        IMutableMapNG firstMap = SPMapNG(MapDimensionsImpl(2, 2, 2),
            PlayerCollection(), -1);
        firstMap.baseTerrain[Point(0, 0)] = TileType.jungle;
        IMapNG zero = SPMapNG(MapDimensionsImpl(2, 2, 2), PlayerCollection(), -1);
        assertIsSubset(firstMap, zero, "zero is a subset of one before copy");
        IMutableMapNG secondMap =
                SPMapNG(MapDimensionsImpl(2, 2, 2), PlayerCollection(), -1);
        secondMap.baseTerrain[Point(0, 0)] = TileType.jungle;
        firstMap.baseTerrain[Point(1, 1)] = TileType.plains;
        secondMap.baseTerrain[Point(1, 1)] = TileType.plains;
        firstMap.addFixture(Point(1, 1), CacheFixture("category", "contents", 3));
        firstMap.addFixture(Point(1, 1), TextFixture("text", -1));
        firstMap.addFixture(Point(1, 1), AnimalTracks("animal"));
        firstMap.addFixture(Point(0, 0),
            Fortification(TownStatus.burned, TownSize.large, 15, "fortification", 6,
                PlayerImpl(0, "")));
        assertEquals(firstMap.copy(false, null), firstMap, "Cloned map equals original");
        IMapNG clone = firstMap.copy(true, null);
        assertIsSubset(clone, zero, "unfilled map is still a subset of zeroed clone");
        // DCs, the only thing zeroed out in *map* copy() at the moment, are ignored by
        // equals().
    //    for (fixture in clone.fixtures[Point(0, 0)].narrow<AbstractTown>()) { // TODO: syntax sugar once compiler bug fixed
        for (fixture in clone.fixtures.get(Point(0, 0)).narrow<AbstractTown>()) {
            assertEquals(fixture.dc, 0, "Copied map didn't copy DCs");
        }
        Unit uOne = Unit(PlayerImpl(0, ""), "type", "name", 7);
        uOne.addMember(Worker("worker", "dwarf", 8, Job("job", 1)));
        assertEquals(uOne.copy(false), uOne, "clone equals original");
        assertNotEquals(uOne.copy(true), uOne, "zeroed clone doesn't equal original");
        assertIsSubset(uOne, uOne.copy(true), "zeroed clone is subset of original");
    }

    "Test [[AbstractTown]] subset calculations, specifically in the [[Town]]
     instantiation."
    test
    shared void testTownSubsets(enumeratedParameter(`class TownSize`) TownSize size,
            enumeratedParameter(`class TownStatus`) TownStatus status) {
        TownSize differentSize;
        TownStatus differentStatus;
        Player playerOne = PlayerImpl(0, "playerOne");
        Player playerTwo = PlayerImpl(1, "playerTwo");
        Player independent = PlayerImpl(2, "independent");
        if (size == TownSize.small) {
            differentSize = TownSize.large;
        } else {
            differentSize = TownSize.small;
        }
        if (status == TownStatus.active) {
            differentStatus = TownStatus.ruined;
        } else {
            differentStatus = TownStatus.active;
        }
        assertNotSubset(Town(status, size, -1, "townName", 0, playerOne),
            Town(status, size, -1, "townName", 1, playerOne),
            "Different IDs break Town subsets");
        assertNotSubset(Town(status, size, -1, "nameOne", 2, playerOne),
            Town(status, size, -1, "nameTwo", 2, playerOne),
            "Different names breaks Town subsets");
        assertIsSubset(Town(status, size, -1, "townName", 3, playerOne),
            Town(status, size, -1, "unknown", 3, playerOne),
            """Town with "unknown" name is still subset""");
        assertNotSubset(Town(status, size, -1, "unknown", 3, playerOne),
            Town(status, size, -1, "townName", 3, playerOne),
            """Name of "unknown" doesn't work in reverse""");
        assertNotSubset(Town(status, size, -1, "townName", 4, playerOne),
            City(status, size, -1, "townName", 4, playerOne),
            "City not a subset of town");
        assertNotSubset(Town(status, size, -1, "townName", 5, playerOne),
            Fortification(status, size, -1, "townName", 5, playerOne),
            "Fortification not a subset of town");
        assertNotSubset(Town(status, size, -1, "townName", 6, playerOne),
            Town(differentStatus, size, -1, "townName", 6, playerOne),
            "Different status breaks subset");
        assertIsSubset(Town(status, size, 5, "townName", 7, playerOne),
            Town(status, size, 10, "townName", 7, playerOne),
            "Different DC doesn't break subset");
        assertNotSubset(Town(status, size, -1, "townName", 8, playerOne),
            Town(status, differentSize, -1, "townName", 8, playerOne),
            "Different size breaks subset");
        assertNotSubset(Town(status, size, -1, "townName", 9, playerOne),
            Town(status, size, -1, "townName", 9, playerTwo),
            "Different owner breaks subset");
        assertIsSubset(Town(status, size, -1, "townName", 10, playerOne),
            Town(status, size, -1, "townName", 10, independent),
            "Still a subset if they think independently owned");
        assertNotSubset(Town(status, size, -1, "townName", 11, independent),
            Town(status, size, -1, "townName", 11, playerOne),
            "Owned is not a subset of independently owned");
        Town first = Town(status, size, -1, "townName", 12, playerOne);
        Town second = Town(status, size, -1, "townName", 12, playerOne);
        first.population = CommunityStats(8);
        assertIsSubset(first, second, "Missing population detils doesn't break subset");
        assertNotSubset(second, first,
            "Having population details when we don't does break subset");
        second.population = CommunityStats(10);
        assertNotSubset(first, second,
            "Having non-subset population details breaks subset");
    }

    "Test [[AbstractTown]] subset calculations, specifically in the [[City]]
     instantiation."
    test
    shared void testCitySubsets(enumeratedParameter(`class TownSize`) TownSize size,
            enumeratedParameter(`class TownStatus`) TownStatus status) {
        TownSize differentSize;
        TownStatus differentStatus;
        Player playerOne = PlayerImpl(0, "playerOne");
        Player playerTwo = PlayerImpl(1, "playerTwo");
        Player independent = PlayerImpl(2, "independent");
        if (size == TownSize.small) {
            differentSize = TownSize.large;
        } else {
            differentSize = TownSize.small;
        }
        if (status == TownStatus.active) {
            differentStatus = TownStatus.ruined;
        } else {
            differentStatus = TownStatus.active;
        }
        assertNotSubset(City(status, size, -1, "townName", 0, playerOne),
            City(status, size, -1, "townName", 1, playerOne),
            "Different IDs break City subsets");
        assertNotSubset(City(status, size, -1, "nameOne", 2, playerOne),
            City(status, size, -1, "nameTwo", 2, playerOne),
            "Different names breaks City subsets");
        assertIsSubset(City(status, size, -1, "townName", 3, playerOne),
            City(status, size, -1, "unknown", 3, playerOne),
            """City with "unknown" name is still subset""");
        assertNotSubset(City(status, size, -1, "unknown", 3, playerOne),
            City(status, size, -1, "townName", 3, playerOne),
            """Name of "unknown" doesn't work in reverse""");
        assertNotSubset(City(status, size, -1, "townName", 4, playerOne),
            Town(status, size, -1, "townName", 4, playerOne),
            "Town not a subset of City");
        assertNotSubset(City(status, size, -1, "townName", 5, playerOne),
            Fortification(status, size, -1, "townName", 5, playerOne),
            "Fortification not a subset of City");
        assertNotSubset(City(status, size, -1, "townName", 6, playerOne),
            City(differentStatus, size, -1, "townName", 6, playerOne),
            "Different status breaks subset");
        assertIsSubset(City(status, size, 5, "townName", 7, playerOne),
            City(status, size, 10, "townName", 7, playerOne),
            "Different DC doesn't break subset");
        assertNotSubset(City(status, size, -1, "townName", 8, playerOne),
            City(status, differentSize, -1, "townName", 8, playerOne),
            "Different size breaks subset");
        assertNotSubset(City(status, size, -1, "townName", 9, playerOne),
            City(status, size, -1, "townName", 9, playerTwo),
            "Different owner breaks subset");
        assertIsSubset(City(status, size, -1, "townName", 10, playerOne),
            City(status, size, -1, "townName", 10, independent),
            "Still a subset if they think independently owned");
        assertNotSubset(City(status, size, -1, "townName", 11, independent),
            City(status, size, -1, "townName", 11, playerOne),
            "Owned is not a subset of independently owned");
        City first = City(status, size, -1, "townName", 12, playerOne);
        City second = City(status, size, -1, "townName", 12, playerOne);
        first.population = CommunityStats(8);
        assertIsSubset(first, second, "Missing population detils doesn't break subset");
        assertNotSubset(second, first,
            "Having population details when we don't does break subset");
        second.population = CommunityStats(10);
        assertNotSubset(first, second,
            "Having non-subset population details breaks subset");
    }

    "Test [[AbstractTown]] subset calculations, specifically in the [[Fortification]]
     instantiation."
    test
    shared void testFortificationSubsets(
            enumeratedParameter(`class TownSize`) TownSize size,
            enumeratedParameter(`class TownStatus`) TownStatus status) {
        TownSize differentSize;
        TownStatus differentStatus;
        Player playerOne = PlayerImpl(0, "playerOne");
        Player playerTwo = PlayerImpl(1, "playerTwo");
        Player independent = PlayerImpl(2, "independent");
        if (size == TownSize.small) {
            differentSize = TownSize.large;
        } else {
            differentSize = TownSize.small;
        }
        if (status == TownStatus.active) {
            differentStatus = TownStatus.ruined;
        } else {
            differentStatus = TownStatus.active;
        }
        assertNotSubset(Fortification(status, size, -1, "townName", 0, playerOne),
            Fortification(status, size, -1, "townName", 1, playerOne),
            "Different IDs break Fortification subsets");
        assertNotSubset(Fortification(status, size, -1, "nameOne", 2, playerOne),
            Fortification(status, size, -1, "nameTwo", 2, playerOne),
            "Different names breaks Fortification subsets");
        assertIsSubset(Fortification(status, size, -1, "townName", 3, playerOne),
            Fortification(status, size, -1, "unknown", 3, playerOne),
            """Fortification with "unknown" name is still subset""");
        assertNotSubset(Fortification(status, size, -1, "unknown", 3, playerOne),
            Fortification(status, size, -1, "townName", 3, playerOne),
            """Name of "unknown" doesn't work in reverse""");
        assertNotSubset(Fortification(status, size, -1, "townName", 4, playerOne),
            Town(status, size, -1, "townName", 4, playerOne),
            "Town not a subset of Fortification");
        assertNotSubset(Fortification(status, size, -1, "townName", 5, playerOne),
            City(status, size, -1, "townName", 5, playerOne),
            "City not a subset of Fortification");
        assertNotSubset(Fortification(status, size, -1, "townName", 6, playerOne),
            Fortification(differentStatus, size, -1, "townName", 6, playerOne),
            "Different status breaks subset");
        assertIsSubset(Fortification(status, size, 5, "townName", 7, playerOne),
            Fortification(status, size, 10, "townName", 7, playerOne),
            "Different DC doesn't break subset");
        assertNotSubset(Fortification(status, size, -1, "townName", 8, playerOne),
            Fortification(status, differentSize, -1, "townName", 8, playerOne),
            "Different size breaks subset");
        assertNotSubset(Fortification(status, size, -1, "townName", 9, playerOne),
            Fortification(status, size, -1, "townName", 9, playerTwo),
            "Different owner breaks subset");
        assertIsSubset(Fortification(status, size, -1, "townName", 10, playerOne),
            Fortification(status, size, -1, "townName", 10, independent),
            "Still a subset if they think independently owned");
        assertNotSubset(Fortification(status, size, -1, "townName", 11, independent),
            Fortification(status, size, -1, "townName", 11, playerOne),
            "Owned is not a subset of independently owned");
        Fortification first = Fortification(status, size, -1, "townName", 12, playerOne);
        Fortification second = Fortification(status, size, -1, "townName", 12, playerOne);
        first.population = CommunityStats(8);
        assertIsSubset(first, second, "Missing population detils doesn't break subset");
        assertNotSubset(second, first,
            "Having population details when we don't does break subset");
        second.population = CommunityStats(10);
        assertNotSubset(first, second,
            "Having non-subset population details breaks subset");
    }
}
