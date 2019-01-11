import strategicprimer.drivers.common {
    CLIDriver
}
import strategicprimer.drivers.common.cli {
    ICLIHelper,
    Applet,
    AppletChooser
}
import ceylon.collection {
    MutableList,
    ArrayList
}
import strategicprimer.model.common.map.fixtures.mobile {
    IUnit
}
import strategicprimer.drivers.exploration.common {
    IExplorationModel
}
import strategicprimer.viewer.drivers.exploration {
    ExplorationCLIHelper
}
class TurnApplet(shared actual void invoke(), shared actual String description,
    shared actual String+ commands) satisfies Applet {}
class TurnRunningCLI(ICLIHelper cli, model) satisfies CLIDriver {
    shared actual IExplorationModel model;
    Boolean unfinishedResults(Integer turn)(IUnit unit) {
        String results = unit.getResults(turn);
        return results.empty || results.lowercased.containsAny(["fixme", "todo", "xxx"]);
    }
    ExplorationCLIHelper explorationCLI = ExplorationCLIHelper(model, cli);
    AppletChooser<TurnApplet> appletChooser =
        AppletChooser(cli, TurnApplet(explorationCLI.moveUntilDone, "move", "move a unit"));
    String createResults(IUnit unit, Integer turn) {
        model.selectedUnit = unit;
        cli.print("Orders for unit ");
        cli.print(unit.name);
        cli.print(" (");
        cli.print(unit.kind);
        cli.print(") for turn ");
        cli.print(turn.string);
        cli.print(": ");
        cli.println(unit.getLatestOrders(turn));
        while (true) {
            switch (command = appletChooser.chooseApplet())
            case (null|true) { continue; }
            case (false) { return ""; }
            case (is TurnApplet) {
                command.invoke();
                break;
            }
        }
        return cli.inputMultilineString("Results: ") else "";
    }
    shared actual void startDriver() {
        Integer currentTurn = model.map.currentTurn;
        if (exists player = cli.chooseFromList(model.playerChoices.sequence(),
                "Players in the maps:", "No players found", "Player to run:",
                false).item) {
            MutableList<IUnit> units = ArrayList {
                elements = model.getUnits(player).filter(unfinishedResults(currentTurn));
            };
            while (true) {
                value index->unit = cli.chooseFromList(units,
                    "Units belonging to ``player``:",
                    "Player has no units without apparently-final results",
                    "Unit to run:", false);
                if (exists unit) {
                    String results = createResults(unit, currentTurn);
                    unit.setResults(currentTurn, results);
                    if (!unfinishedResults(currentTurn)(unit)) {
                        units.delete(index);
                    }
                } else {
                    break;
                }
                if (units.empty) {
                    break;
                }
            }
        }
    }
}
