import ceylon.collection {
    MutableList,
    MutableMap,
    ArrayList,
    HashMap
}

import lovelace.util.common {
    DelayedRemovalMap
}

import strategicprimer.model.map {
    Point,
    Player,
    IFixture,
    River,
    IMapNG,
    invalidPoint,
    MapDimensions
}
import strategicprimer.model.map.fixtures {
    Implement,
    ResourcePile,
    FortressMember
}
import strategicprimer.model.map.fixtures.mobile {
    IUnit
}
import strategicprimer.model.map.fixtures.terrain {
    Oasis,
    Hill,
    Forest
}
import strategicprimer.model.map.fixtures.towns {
    Fortress
}
import strategicprimer.report {
    IReportNode
}
import strategicprimer.report.nodes {
    SimpleReportNode,
    SectionListReportNode,
    ListReportNode,
    SectionReportNode,
    emptyReportNode,
    ComplexReportNode
}
"A report generator for fortresses."
shared class FortressReportGenerator(
        Comparison([Point, IFixture], [Point, IFixture]) comp, Player currentPlayer,
        MapDimensions dimensions, Integer currentTurn, Point hq = invalidPoint)
        extends AbstractReportGenerator<Fortress>(comp, dimensions, hq) {
    IReportGenerator<IUnit> urg =
            UnitReportGenerator(comp, currentPlayer, dimensions, currentTurn, hq);
    IReportGenerator<FortressMember> memberReportGenerator =
            FortressMemberReportGenerator(comp, currentPlayer, dimensions, currentTurn,
                hq);
    String terrain(IMapNG map, Point point,
            DelayedRemovalMap<Integer, [Point, IFixture]> fixtures) {
        StringBuilder builder = StringBuilder();
        builder.append("Surrounding terrain: ``map.baseTerrain[point] else "Unknown"``");
        variable Boolean unforested = true;
//        if (map.mountainous[point]) { // TODO: syntax sugar once compiler bug fixed
        if (map.mountainous.get(point)) {
            builder.append(", mountainous");
        }
//        for (fixture in map.fixtures[point]) {
        for (fixture in map.fixtures.get(point)) {
            if (unforested, is Forest fixture) {
                unforested = false;
                builder.append(", forested with ``fixture.kind``");
                fixtures.remove(fixture.id);
            } else if (is Hill fixture) {
                builder.append(", hilly");
                fixtures.remove(fixture.id);
            } else if (is Oasis fixture) {
                builder.append(", with a nearby oasis");
                fixtures.remove(fixture.id);
            }
        }
        return builder.string;
    }
    "Write HTML representing a collection of rivers."
    void riversToString(Anything(String) formatter, River* rivers) {
        if (rivers.contains(River.lake)) {
            formatter("""<li>There is a nearby lake.</li>
                         """);
        }
        value temp = rivers.filter((river) => river != River.lake);
        if (exists first = temp.first) {
            formatter("<li>There is a river on the tile, ");
            formatter("flowing through the following borders: ");
            formatter(first.description);
            for (river in temp.rest) {
                formatter(", ``river.description``");
            }
            formatter("""</li>
                         """);
        }
    }
    "Add nodes representing rivers to a parent."
    void riversToNode(Point loc, IReportNode parent, River* rivers) {
        if (rivers.contains(River.lake)) {
            parent.appendNode(SimpleReportNode("There is a nearby lake.", loc));
        }
        value temp = rivers.filter((river) => river != River.lake);
        if (exists first = temp.first) {
            StringBuilder builder = StringBuilder();
            builder.append(
                "There is a river on the tile, flowing through the following borders: ");
            builder.append(first.description);
            for (river in temp.rest) {
                builder.append(", ``river.description``");
            }
            parent.appendNode(SimpleReportNode(builder.string, loc));
        }
    }
    "Produces a sub-report on a fortress, or all fortresses. All fixtures referred to in
     this report are removed from the collection."
    shared actual void produce(DelayedRemovalMap<Integer, [Point, IFixture]> fixtures,
            IMapNG map, Anything(String) ostream, [Fortress, Point]? entry) {
        if (exists entry) {
            Fortress item = entry.first;
            Point loc = entry.rest.first;
            ostream("<h5>Fortress ``item.name`` belonging to ``
                    (item.owner == currentPlayer) then "you" else item.owner.string``</h5>
                     <ul>
                     <li>Located at ``loc`` ``distCalculator.distanceString(loc)``</li>
                     <li>``terrain(map, loc, fixtures)``</li>
                     ");
//            riversToString(ostream, *map.rivers[loc]); // TODO: syntax sugar once compiler bug fixed
            riversToString(ostream, *map.rivers.get(loc));
            MutableList<IUnit> units = ArrayList<IUnit>();
            MutableList<Implement> equipment = ArrayList<Implement>();
            MutableMap<String, MutableList<ResourcePile>> resources =
                    HashMap<String, MutableList<ResourcePile>>();
            MutableList<FortressMember> contents = ArrayList<FortressMember>();
            for (member in item) {
                if (is IUnit member) {
                    units.add(member);
                } else if (is Implement member) {
                    equipment.add(member);
                } else if (is ResourcePile member) {
                    String kind = member.kind;
                    if (exists list = resources[kind]) {
                        list.add(member);
                    } else {
                        MutableList<ResourcePile> list = ArrayList<ResourcePile>();
                        resources[kind] = list;
                        list.add(member);
                    }
                } else {
                    contents.add(member);
                }
                fixtures.remove(item.id);
            }
            if (!units.empty) {
                ostream("""<li>Units on the tile:<ul>
                           """);
                for (unit in units) {
                    ostream("<li>");
                    urg.produce(fixtures, map, ostream, [unit, loc]);
                    ostream("""</li>
                               """);
                }
                ostream("""</ul></li>
                           """);
            }
            if (!equipment.empty) {
                ostream("""<li>Equipment:<ul>
                           """);
                for (implement in equipment) {
                    ostream("<li>");
                    memberReportGenerator.produce(fixtures, map, ostream, [implement,
                        loc]);
                    ostream("""</li>
                               """);
                }
                ostream("""</ul></li>
                           """);
            }
            if (!resources.empty) {
                ostream("""<li>Resources:<ul>
                           """);
                for (kind->list in resources) {
                    ostream("<li>``kind``
                             <ul>
                             ");
                    for (pile in list) {
                        ostream("<li>");
                        memberReportGenerator.produce(fixtures, map, ostream, [pile,
                            loc]);
                        ostream("""</li>
                                   """);
                    }
                    ostream("""</ul>
                               </li>
                               """);
                }
                ostream("""</ul></li>
                           """);
            }
            if (!contents.empty) {
                ostream("""<li>Other fortress contents:<ul>
                           """);
                for (member in contents) {
                    ostream("<li>");
                    memberReportGenerator.produce(fixtures, map, ostream, [member, loc]);
                    ostream("""</li>
                               """);
                }
                ostream("""</ul></li>
                           """);
            }
            ostream("</ul>\n");
            fixtures.remove(item.id);
        } else {
            MutableMap<Fortress, Point> ours = HashMap<Fortress, Point>();
            MutableMap<Fortress, Point> others = HashMap<Fortress, Point>();
            MutableList<[Point, IFixture]> values =
                    ArrayList<[Point, IFixture]> { *fixtures.items
                        .sort(pairComparator) };
            for ([loc, item] in values) {
                if (is Fortress fort = item) {
                    if (currentPlayer == fort.owner) {
                        ours[fort] = loc;
                    } else {
                        others[fort] = loc;
                    }
                }
            }
            if (!ours.empty) {
                ostream("""<h4>Your fortresses in the map:</h4>
                       """);
                for (fort->loc in ours) {
                    produce(fixtures, map, ostream, [fort, loc]);
                }
            }
            if (!others.empty) {
                ostream("""<h4>Other fortresses in the map:</h4>
                       """);
                for (fort->loc in others) {
                    produce(fixtures, map, ostream, [fort, loc]);
                }
            }
        }
    }
    "Produces a sub-report on a fortress, or all fortresses. All fixtures referred to in
     this report are removed from the collection."
    shared actual IReportNode produceRIR(
            DelayedRemovalMap<Integer, [Point, IFixture]> fixtures,
            IMapNG map, [Fortress, Point]? entry) {
        if (exists entry) {
            Fortress item = entry.first;
            Point loc = entry.rest.first;
            IReportNode retval = SectionListReportNode(5,
                "Fortress ``item.name`` belonging to ``
                (item.owner == currentPlayer) then "you" else item.owner.string``", loc);
            retval.appendNode(SimpleReportNode("Located at ``loc`` ``distCalculator
                .distanceString(loc)``", loc));
            // This is a no-op if no rivers, so avoid an if
//            riversToNode(loc, retval, *map.rivers[loc]); // TODO: syntax sugar once compiler bug fixed
            riversToNode(loc, retval, *map.rivers.get(loc));
            IReportNode units = ListReportNode("Units on the tile:");
            IReportNode resources = ListReportNode("Resources:", loc);
            MutableMap<String,IReportNode> resourceKinds = HashMap<String,IReportNode>();
            IReportNode equipment = ListReportNode("Equipment:", loc);
            IReportNode contents = ListReportNode("Other Contents of Fortress:", loc);
            for (member in item) {
                if (is IUnit member) {
                    units.appendNode(urg.produceRIR(fixtures, map, [member, loc]));
                } else if (is Implement member) {
                    equipment.appendNode(memberReportGenerator.produceRIR(fixtures, map,
                        [member, loc]));
                } else if (is ResourcePile member) {
                    IReportNode node;
                    if (exists temp = resourceKinds[member.kind]) {
                        node = temp;
                    } else {
                        node = ListReportNode("``member.kind``:");
                        resourceKinds[member.kind] = node;
                    }
                    node.appendNode(memberReportGenerator.produceRIR(fixtures, map,
                        [member, loc]));
                } else {
                    contents.appendNode(memberReportGenerator.produceRIR(fixtures, map,
                        [member, loc]));
                }
            }
            for (node in resourceKinds.items) {
                resources.addIfNonEmpty(node);
            }
            retval.addIfNonEmpty(units, resources, equipment, contents);
            fixtures.remove(item.id);
            return retval;
        } else {
            MutableList<[Point, IFixture]> values =
                    ArrayList<[Point, IFixture]> { *fixtures.items
                        .sort(pairComparator) };
            IReportNode foreign = SectionReportNode(4, "Foreign fortresses in the map:");
            IReportNode ours = SectionReportNode(4, "Your fortresses in the map:");
            for ([loc, item] in values) {
                if (is Fortress fort = item) {
                    if (currentPlayer == fort.owner) {
                        ours.appendNode(produceRIR(fixtures, map, [fort,
                            loc]));
                    } else {
                        foreign.appendNode(produceRIR(fixtures, map, [fort,
                            loc]));
                    }
                }
            }
            if (ours.childCount == 0) {
                if (foreign.childCount == 0) {
                    return emptyReportNode;
                } else {
                    return foreign;
                }
            } else if (foreign.childCount == 0) {
                return ours;
            } else {
                IReportNode retval = ComplexReportNode();
                retval.appendNode(ours);
                retval.appendNode(foreign);
                return retval;
            }
        }
    }
}
