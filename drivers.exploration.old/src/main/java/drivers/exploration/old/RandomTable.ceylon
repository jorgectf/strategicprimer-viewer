import strategicprimer.model.common.map {
    TileType,
    Point,
    MapDimensions,
    TileFixture
}
import lovelace.util.common {
    singletonRandom
}

"An [[EncounterTable]] where the event is selected at random."
class RandomTable([Integer, String]* items) satisfies EncounterTable {
    [Integer, String][] table =
            items.sort(byIncreasing<[Integer, String], Integer>(Tuple.first));

    "Get the first item in the table whose numeric value is above the given value."
    String lowestMatch(Integer val) {
        for ([num, string] in table) {
            if (val >= num) {
                return string;
            }
        }
        assert (exists retval = table.last);
        return retval.rest.first;
    }

    shared actual String generateEvent(Point point, TileType? terrain,
            Boolean mountainous, {TileFixture*} fixtures, MapDimensions dimensions) =>
                lowestMatch(singletonRandom.nextInteger(100));

    shared actual Set<String> allEvents => set(table.map(Tuple.rest).map(Tuple.first));

    shared actual String string = "RandomTable of ``table.size`` items";
}
