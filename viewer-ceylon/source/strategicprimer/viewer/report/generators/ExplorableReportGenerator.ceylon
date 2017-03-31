import ceylon.collection {
    ArrayList,
    MutableMap,
    MutableList,
    HashMap
}
import ceylon.language.meta {
    type
}
import ceylon.language.meta.model {
    Type
}

import java.lang {
    IllegalArgumentException
}

import lovelace.util.common {
    todo,
    DelayedRemovalMap
}

import model.map {
    Point
}

import strategicprimer.viewer.model {
    DistanceComparator
}
import strategicprimer.viewer.model.map {
    IMapNG,
    invalidPoint,
    IFixture,
    Player
}
import strategicprimer.viewer.model.map.fixtures.explorable {
    ExplorableFixture,
    Cave,
    Portal,
    AdventureFixture,
    Battlefield
}
import strategicprimer.viewer.report.nodes {
    IReportNode,
    SimpleReportNode,
    ListReportNode,
    SectionListReportNode,
    emptyReportNode,
    ComplexReportNode
}
"A report generator for caves, battlefields, adventure hooks, and portals."
todo("Use union type instead of interface, here and elsewhere")
shared class ExplorableReportGenerator(Comparison([Point, IFixture], [Point, IFixture]) comp,
        Player currentPlayer, Point hq = invalidPoint)
        extends AbstractReportGenerator<ExplorableFixture>(comp, DistanceComparator(hq)) {
    "Produces a more verbose sub-report on a cave, battlefield, portal, or adventure
     hook, or the report on all such."
    shared actual void produce(DelayedRemovalMap<Integer, [Point, IFixture]> fixtures,
            IMapNG map, Anything(String) ostream, [ExplorableFixture, Point]? entry) {
        if (exists entry) {
            ExplorableFixture item = entry.first;
            Point loc = entry.rest.first;
            if (is Cave item) {
                fixtures.remove(item.id);
                ostream("Caves beneath ``loc````distCalculator.distanceString(loc)``");
            } else if (is Battlefield item) {
                fixtures.remove(item.id);
                ostream("Signs of a long-ago battle on ``loc````distCalculator
                    .distanceString(loc)``");
            } else if (is AdventureFixture item) {
                fixtures.remove(item.id);
                ostream("``item.briefDescription`` at ``loc``: ``item
                    .fullDescription`` ``distCalculator.distanceString(loc)``");
                if (!item.owner.independent) {
                    String player;
                    if (item.owner == currentPlayer) {
                        player = "you";
                    } else {
                        player = "another player";
                    }
                    ostream(" (already investigated by ``player``)");
                }
            } else if (is Portal item) {
                fixtures.remove(item.id);
                ostream("A portal to another world at ``loc`` ``distCalculator
                    .distanceString(loc)``");
            } else {
                throw IllegalArgumentException("Unexpected ExplorableFixture type");
            }
        } else {
            MutableList<[Point, IFixture]> values =
                    ArrayList<[Point, IFixture]> { *fixtures.items
                        .sort(pairComparator) };
            MutableList<Point> portals = PointList("Portals to other worlds: ");
            MutableList<Point> battles = PointList(
                "Signs of long-ago battles on the following tiles:");
            MutableList<Point> caves = PointList("Caves beneath the following tiles: ");
            HeadedMap<AdventureFixture, Point>&
            MutableMap<AdventureFixture, Point> adventures =
                    HeadedMapImpl<AdventureFixture, Point>(
                        "<h4>Possible Adventures</h4>");
            Map<Type<IFixture>, Anything([Point, IFixture])> collectors =
                    HashMap<Type<IFixture>, Anything([Point, IFixture])> {
                        entries = { `Portal`->(([Point, IFixture] pair) =>
                        portals.add(pair.first)),
                            `Battlefield`->(([Point, IFixture] pair) =>
                            battles.add(pair.first)),
                            `Cave`->(([Point, IFixture] pair) =>
                            caves.add(pair.first)),
                            `AdventureFixture`->(([Point, IFixture] pair) {
                                assert (is AdventureFixture fixture = pair.rest.first);
                                adventures.put(fixture, pair.first);
                            })};
                    };
            for ([loc, item] in values) {
                if (exists collector = collectors.get(type(item))) {
                    collector([loc, item]);
                    fixtures.remove(item.id);
                }
            }
            if (!caves.empty || !battles.empty || !portals.empty) {
                ostream("<h4>Caves, Battlefields, and Portals</h4>
                         <ul>
                         ``caves````battles````portals``</ul>
                         ");
            }
            writeMap(ostream, adventures,
                        (AdventureFixture key->Point val, formatter) => produce(fixtures, map,
                    formatter, [key, val]));
        }
    }
    "Produces a more verbose sub-report on a cave or battlefield, or the report section on
     all such."
    shared actual IReportNode produceRIR(DelayedRemovalMap<Integer, [Point, IFixture]> fixtures,
            IMapNG map, [ExplorableFixture, Point]? entry) {
        if (exists entry) {
            ExplorableFixture item = entry.first;
            Point loc = entry.rest.first;
            if (is Cave item) {
                fixtures.remove(item.id);
                return SimpleReportNode("Caves beneath ``loc`` ``distCalculator.distanceString(loc)``",
                    loc);
            } else if (is Battlefield item) {
                fixtures.remove(item.id);
                return SimpleReportNode(
                    "Signs of a long-ago battle on ``loc`` ``distCalculator
                        .distanceString(loc)``", loc);
            } else if (is AdventureFixture item) {
                fixtures.remove(item.id);
                if (item.owner.independent) {
                    return SimpleReportNode("``item.briefDescription`` at ``loc``: ``item
                        .fullDescription`` ``distCalculator.distanceString(loc)``",
                        loc);
                } else if (currentPlayer == item.owner) {
                    return SimpleReportNode("``item.briefDescription`` at ``loc``: ``item
                        .fullDescription`` ``distCalculator
                        .distanceString(loc)`` (already investigated by you)",
                        loc);
                } else {
                    return SimpleReportNode("``item.briefDescription`` at ``loc``: ``item
                        .fullDescription`` ``distCalculator
                        .distanceString(
                        loc)`` (already investigated by another player)",
                        loc);
                }
            } else if (is Portal item) {
                fixtures.remove(item.id);
                return SimpleReportNode("A portal to another world at ``loc`` ``distCalculator
                    .distanceString(loc)``",
                    loc);
            } else {
                throw IllegalArgumentException("Unexpected ExplorableFixture type");
            }
        } else {
            MutableList<[Point, IFixture]> values =
                    ArrayList<[Point, IFixture]> { *fixtures.items
                        .sort(pairComparator) };
            IReportNode portals = ListReportNode("Portals");
            IReportNode battles = ListReportNode("Battlefields");
            IReportNode caves = ListReportNode("Caves");
            IReportNode adventures = SectionListReportNode(4, "Possible Adventures");
            Map<Type<IFixture>, IReportNode> nodes =
                    HashMap<Type<IFixture>, IReportNode> {
                        entries = { `Portal`->portals, `Battlefield`->battles, `Cave`->caves,
                            `AdventureFixture`->adventures };
                    };
            for ([loc, item] in values) {
                if (is ExplorableFixture fixture = item,
                    exists node = nodes.get(type(fixture))) {
                    node.appendNode(produceRIR(fixtures, map, [fixture, loc]));
                }
            }
            IReportNode retval = SectionListReportNode(4,
                "Caves, Battlefields, and Portals");
            retval.addIfNonEmpty(caves, battles, portals);
            if (retval.childCount == 0) {
                if (adventures.childCount == 0) {
                    return emptyReportNode;
                } else {
                    return adventures;
                }
            } else if (adventures.childCount == 0) {
                return retval;
            } else {
                IReportNode real = ComplexReportNode();
                real.appendNode(retval);
                real.appendNode(adventures);
                return real;
            }
        }
    }
}
