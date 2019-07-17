import ceylon.collection {
    MutableList
}

import lovelace.util.common {
    DRMap=DelayedRemovalMap
}

import strategicprimer.model.common.map {
    Player,
    IFixture,
    Point,
    MapDimensions,
    IMapNG
}
import strategicprimer.model.common.map.fixtures.explorable {
    Cave,
    Portal,
    Battlefield
}

"A report generator for caves, battlefields, and portals."
shared class ExplorableReportGenerator(
        Comparison([Point, IFixture], [Point, IFixture]) comp, Player currentPlayer,
        MapDimensions dimensions, Point? hq = null)
        extends AbstractReportGenerator<Battlefield|Cave|Portal>(comp, dimensions, hq) {
    "Produces a more verbose sub-report on a cave, battlefield, or portal."
    shared actual void produceSingle(DRMap<Integer, [Point, IFixture]> fixtures,
            IMapNG map, Anything(String) ostream, Battlefield|Cave|Portal item,
            Point loc) {
        switch (item)
        case (is Cave) {
            fixtures.remove(item.id);
            ostream("Caves beneath ");
        }
        case (is Battlefield) {
            fixtures.remove(item.id);
            ostream("Signs of a long-ago battle on ");
        }
        case (is Portal) {
            fixtures.remove(item.id);
            ostream("A portal to another world at ");
        }
        if (loc.valid) {
            ostream(loc.string + distanceString(loc));
        } else {
            ostream("an unknown location");
        }
    }

    "Produces the report on all caves, battlefields, and portals."
    shared actual void produce(DRMap<Integer, [Point, IFixture]> fixtures,
            IMapNG map, Anything(String) ostream) {
        MutableList<Point> portals = PointList("Portals to other worlds: ");
        MutableList<Point> battles = PointList(
            "Signs of long-ago battles on the following tiles:");
        MutableList<Point> caves = PointList("Caves beneath the following tiles: ");
        for ([loc, item] in fixtures.items.narrow<[Point, Portal|Battlefield|Cave]>()
                .sort(pairComparator)) {
            switch (item)
            case (is Portal) {
                portals.add(loc);
            }
            case (is Battlefield) {
                battles.add(loc);
            }
            case (is Cave) {
                caves.add(loc);
            }
            fixtures.remove(item.id);
        }
        if (!caves.empty || !battles.empty || !portals.empty) {
            ostream("<h4>Caves, Battlefields, and Portals</h4>
                     <ul>");
            // N.b. Sugaring Iterable<Anything> to {Anything*} won't compile
            for (list in [ caves, battles, portals ]
                    .filter(not(Iterable<Anything>.empty))) {
                ostream("<li>``list``</li>");
            }
            ostream("</ul>``operatingSystem.newline``");
        }
    }
}
