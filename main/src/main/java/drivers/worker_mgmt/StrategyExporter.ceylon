import ceylon.collection {
    MutableMap,
    HashMap,
    MutableList,
    ArrayList
}
import ceylon.file {
    Resource,
    File,
    Writer,
    Nil
}

import strategicprimer.drivers.common {
    SPOptions,
    PlayerChangeListener,
    IWorkerModel
}
import strategicprimer.model.common.map {
    Player,
    HasName
}
import strategicprimer.model.common.map.fixtures {
    Implement,
    IResourcePile,
    UnitMember
}
import strategicprimer.model.common.map.fixtures.mobile {
    IWorker,
    IUnit,
    Animal,
    animalPlurals
}
import strategicprimer.model.common.map.fixtures.mobile.worker {
    IJob
}
import com.vasileff.ceylon.structures {
    MutableMultimap,
    ArrayListMultimap
}

"A class to write a proto-strategy to file."
class StrategyExporter(IWorkerModel model, SPOptions options)
        satisfies PlayerChangeListener {
    variable Player currentPlayer = model.currentPlayer;
    shared actual void playerChanged(Player? old, Player newPlayer) =>
            currentPlayer = newPlayer;

    String jobString(IJob job) => "``job.name`` ``job.level``";

    void writeMember(Writer writer, UnitMember? member) {
        if (is IWorker member) {
            writer.write(member.name);
            {IJob*} jobs;
            if (options.hasOption("--include-unleveled-jobs")) {
                jobs = member.filter(not(IJob.emptyJob));
            } else {
                jobs = member.filter(compose(Integer.positive, IJob.level));
            }
            if (!jobs.empty) {
                writer.write(" (``", ".join(jobs.map(jobString))``)");
            }
        } else if (is Animal member) {
            if (member.population > 1) {
                writer.write("``member.population`` ");
            }
            if (member.status != "domesticated") {
                writer.write("``member.status`` ");
            }
            if (member.population > 1) {
                //writer.write(animalPlurals[member.kind]); // TODO: syntax sugar once compiler bug fixed
                writer.write(animalPlurals.get(member.kind));
            } else {
                writer.write(member.kind);
            }
            if (member.born >= 0) {
               writer.write(" (born turn ``member.born``)");
            }
        } else if (exists member) {
            writer.write(member.string);
        }
    }

    void summarizeUnitMembers(Writer writer, IUnit unit) {
        MutableList<IWorker> leveledWorkers = ArrayList<IWorker>();
        MutableList<UnitMember> nonWorkers = ArrayList<UnitMember>();
        MutableList<IWorker> unleveledWorkers = ArrayList<IWorker>();
        for (member in unit) {
            if (is IWorker member) {
                if (member.map(IJob.level).any(Integer.positive)) {
                    leveledWorkers.add(member);
                } else {
                    unleveledWorkers.add(member);
                }
            } else {
                nonWorkers.add(member);
            }
        }
        variable Boolean needComma;
        if (leveledWorkers.empty) {
            if (unleveledWorkers.empty) {
                needComma = false;
            } else {
                writeMember(writer, unleveledWorkers.first);
                if (!unleveledWorkers.rest.empty) {
                    writer.write(
                        ", ``unleveledWorkers.rest.size`` other unleveled workers");
                }
                needComma = true;
            }
        } else {
            needComma = true;
            writeMember(writer, leveledWorkers.first);
            for (worker in leveledWorkers) {
                writer.write(", ");
                writeMember(writer, worker);
            }
            if (!unleveledWorkers.empty) {
                writer.write(", ``unleveledWorkers.size`` unleveled workers");
            }
        }
        for (member in nonWorkers) {
            if (needComma) {
                writer.write(", ");
            }
            writeMember(writer, member);
            needComma = true;
        }
    }

    String workerString(UnitMember? member) =>
            if (is HasName member) then member.name else (member?.string else "");

    shared void writeStrategy(Resource path, {UnitMember*} dismissed) {
        File file;
        assert (is File|Nil path);
        switch (path)
        case (is Nil) {
            file = path.createFile();
        } case (is File) {
            file = path;
        }
        try (writer = file.Overwriter()) {
            String playerName = currentPlayer.name;
            Integer turn = model.map.currentTurn;
            {IUnit*} units = model.getUnits(currentPlayer);
            MutableMultimap<String, IUnit> unitsByKind =
                    ArrayListMultimap<String, IUnit>();
            for (unit in units) {
                if (!unit.empty || "true" == options.getArgument("--print-empty")) {
                    unitsByKind.put(unit.kind, unit);
                }
            }
            MutableMap<IUnit, String> orders = HashMap<IUnit, String>();
            for (kind->unit in unitsByKind) {
                String unitOrders = unit.getLatestOrders(turn);
                Integer ordersTurn = unit.getOrdersTurn(unitOrders);
                if (unitOrders == unit.getOrders(turn) || ordersTurn < 0) {
                    orders[unit] = unitOrders;
                } else {
                    orders[unit] = "(From turn #``ordersTurn``) ``unitOrders``";
                }
            }
            writer.writeLine("[Player: ``playerName``");
            if (exists country = currentPlayer.country, !country.empty) {
                writer.write("Country: ");
                writer.writeLine(country);
            }
            writer.writeLine("Turn ``turn``]");
            writer.writeLine();
            writer.writeLine("Inventions: TODO: any?");
            writer.writeLine();

            if (!dismissed.empty) {
                writer.write("Dismissed workers etc.: ");
                writer.write(", ".join(dismissed.map(workerString)));
                writer.writeLine();
                writer.writeLine();
            }

            writer.writeLine("Workers:");
            writer.writeLine();
            for (kind->list in unitsByKind.asMap) {
                if (list.empty) {
                    continue;
                }
                writer.writeLine("* ``kind``:");
                for (unit in list) {
                    writer.write("  - ``unit.name``");
                    if (!unit.empty) {
                        writer.write(" [");
                        if (unit.size > 4,
                                "true" == options
                                    .getArgument("--summarize-large-units")) {
                            summarizeUnitMembers(writer, unit);
                        } else {
                            writeMember(writer, unit.first);
                            for (member in unit.rest) {
                                writer.write(", ");
                                writeMember(writer, member);
                            }
                        }
                        writer.write("]");
                    }
                    if (options.hasOption("--results")) {
                        writer.write(": ");
                        if (exists unitOrders = orders[unit], !unitOrders.empty) {
                            writer.writeLine(unitOrders);
                        } else {
                            writer.writeLine("TODO");
                        }
                        writer.writeLine();
                        String results = unit.getResults(turn);
                        if (results.empty) {
                            writer.writeLine("TODO: run");
                        } else {
                            writer.writeLine(results);
                        }
                        writer.writeLine();
                        writer.writeLine("TODO: advancement");
                    } else {
                        writer.writeLine(":");
                        writer.writeLine();
                        if (exists unitOrders = orders[unit], !unitOrders.empty) {
                            writer.writeLine(unitOrders);
                        } else {
                            writer.writeLine("TODO");
                        }
                    }
                    writer.writeLine();
                }
            }
            if (options.hasOption("--results")) {
                writer.writeLine("Resources:");
                writer.writeLine();
                for (fortress in model.getFortresses(currentPlayer)) {
                    writer.write("- In ");
                    writer.writeLine(fortress.name);
                    value equipment = fortress.narrow<Implement>();
                    if (!equipment.empty) {
                        writer.writeLine("  - Equipment not in a unit:");
                        for (item in equipment) {
                            writer.write("    - ");
                            writer.writeLine(item.string); // FIXME: This is egregiously verbose ("An implement of kind ...")
                        }
                    }
                    for (kind->resources in fortress.narrow<IResourcePile>().group(IResourcePile.kind)) {
                        writer.write("  - ");
                        writer.write(kind);
                        writer.writeLine(":");
                        for (pile in resources) {
                            writer.write("    - ");
                            writer.writeLine(pile.string); // FIXME: This is egregiously verbose ("A pile of ...")
                        }
                    }
                    if (fortress.size > fortress.narrow<Implement|IResourcePile|IUnit>().size) {
                        process.writeErrorLine("Unhandled members in ``fortress.name``"); // TODO: Take ICLIHelper to report diagnostics on
                    }
                }
                for (unit in model.getUnits(currentPlayer)) {
                    if (unit.filter(not(`IWorker`.typeOf)).empty) {
                        continue;
                    }
                    writer.write("- With unit ");
                    writer.write(unit.name);
                    writer.write(" (");
                    writer.write(unit.kind);
                    writer.writeLine("):");
                    for (member in unit) {
                        if (!member is IWorker) {
                            writer.write("  - ");
                            writer.writeLine(member.string);
                        }
                    }
                }
            }
        }
    }
}
