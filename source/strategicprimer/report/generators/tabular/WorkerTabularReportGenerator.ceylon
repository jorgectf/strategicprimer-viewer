import lovelace.util.common {
    DelayedRemovalMap,
    comparingOn
}

import strategicprimer.model.common {
    DistanceComparator
}
import strategicprimer.model.common.map {
    IFixture,
    MapDimensions,
    Point
}
import strategicprimer.model.common.map.fixtures.mobile {
    IWorker
}
import strategicprimer.model.common.map.fixtures.mobile.worker {
    WorkerStats
}

"A report generator for workers. We do not cover Jobs or Skills; see
 [[SkillTabularReportGenerator]] for that."
shared class WorkerTabularReportGenerator(Point hq, MapDimensions dimensions)
        extends AbstractTableGenerator<IWorker>()
        satisfies ITableGenerator<IWorker> {
    "The header row of the table."
    shared actual [String+] headerRow = ["Distance", "Location", "Name", "Race", "HP",
        "Max HP", "Str",
        "Dex", "Con", "Int", "Wis", "Cha"];

    "The file-name to (by default) write this table to."
    shared actual String tableName = "workers";

    "Create a GUI table row representing a worker."
    shared actual {{String+}+} produce(
            DelayedRemovalMap<Integer, [Point, IFixture]> fixtures, IWorker item,
            Integer key, Point loc, Map<Integer, Integer> parentMap) {
        fixtures.remove(key);
        if (exists stats = item.stats) {
            return [[distanceString(loc, hq, dimensions), loc.string, item.name,
                item.race, stats.hitPoints.string, stats.maxHitPoints.string,
                *stats.array.map(WorkerStats.getModifierString)]];
        } else {
            return [[distanceString(loc, hq, dimensions), loc.string, item.name,
                item.race, *["---"].cycled.take(9)]];
        }
    }

    "Compare two worker-location pairs."
    shared actual Comparison comparePairs([Point, IWorker] one,
            [Point, IWorker] two) =>
        comparing(comparingOn(pairPoint, DistanceComparator(hq, dimensions).compare),
            byIncreasing(compose(IWorker.name, pairFixture)))(one, two);
}
