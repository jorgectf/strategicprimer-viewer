import ceylon.logging {
    logger,
    Logger
}

import javax.swing {
    SwingUtilities
}

import strategicprimer.drivers.common {
    IMultiMapModel,
    IDriverModel,
    IDriverUsage,
    DriverUsage,
    SPOptions,
    ParamCount,
    DriverFailedException,
    ISPDriver,
    GUIDriver
}
import strategicprimer.drivers.common.cli {
    ICLIHelper
}
import strategicprimer.model.common.map {
    MapDimensions,
    Point
}
import strategicprimer.drivers.gui.common.about {
    aboutDialog
}
import strategicprimer.viewer.drivers {
    IOHandler,
    MenuBroker,
    SPFileChooser
}
import strategicprimer.drivers.gui.common {
    SPFrame,
    WindowCloseListener
}
import lovelace.util.common {
    silentListener,
    PathWrapper
}
import lovelace.util.jvm {
    FileChooser
}

"A logger."
Logger log = logger(`module strategicprimer.viewer`);
"A driver to start the map viewer."
service(`interface ISPDriver`)
shared class ViewerGUI() satisfies GUIDriver {
    shared actual IDriverUsage usage = DriverUsage {
        graphical = true;
        invocations = ["-m", "--map"];
        paramsWanted = ParamCount.one;
        shortDescription = "Map viewer";
        longDescription = "Look at the map visually. This is probably the app you want.";
        includeInCLIList = false;
        includeInGUIList = true;
        supportedOptionsTemp = [ "--current-turn=NN" ];
    };
    shared actual void startDriverOnModel(ICLIHelper cli, SPOptions options,
            IDriverModel model) {
        if (is IViewerModel model) {
            MenuBroker menuHandler = MenuBroker();
            menuHandler.register(IOHandler(model, options, cli), "load", "save",
                "save as", "new", "load secondary", "save all", "open in map viewer",
                "open secondary map in map viewer", "close", "quit");
            menuHandler.register(silentListener(model.zoomIn), "zoom in");
            menuHandler.register(silentListener(model.zoomOut), "zoom out");
            menuHandler.register(silentListener(model.resetZoom), "reset zoom");
            menuHandler.register((event) {
                Point selection = model.selection;
                MapDimensions dimensions = model.mapDimensions;
                VisibleDimensions visible = model.visibleDimensions;
                Integer topRow;
                if (selection.row - (visible.height / 2) <= 0) {
                    topRow = 0;
                } else if (selection.row + (visible.height / 2) >= dimensions.rows) {
                    topRow = dimensions.rows - visible.height;
                } else {
                    topRow = selection.row - (visible.height / 2);
                }
                Integer leftColumn;
                if (selection.column - (visible.width / 2) <= 0) {
                    leftColumn = 0;
                } else if (selection.column + (visible.width / 2) >= dimensions.columns) {
                    leftColumn = dimensions.columns - visible.width;
                } else {
                    leftColumn = selection.column - (visible.width / 2);
                }
                // Java version had topRow + dimensions.rows and
                // leftColumn + dimensions.columns as max row and column; this seems
                // plainly wrong.
                model.visibleDimensions = VisibleDimensions(topRow,
                    topRow + visible.height, leftColumn, leftColumn + visible.width);
            }, "center");
            SwingUtilities.invokeLater(() {
                SPFrame&MapGUI frame = ViewerFrame(model,
                    menuHandler.actionPerformed);
                frame.addWindowListener(WindowCloseListener(menuHandler.actionPerformed));
                value selectTileDialogInstance = SelectTileDialog(frame, model);
                menuHandler.registerWindowShower(selectTileDialogInstance, "go to tile");
                selectTileDialogInstance.dispose();
                variable FindDialog? finder = null;
                FindDialog getFindDialog() {
                    if (exists temp = finder) {
                        return temp;
                    } else {
                        FindDialog local = FindDialog(frame, model);
                        finder = local;
                        return local;
                    }
                }
                menuHandler.registerWindowShower(getFindDialog, "find a fixture");
                menuHandler.register(silentListener(compose(FindDialog.search,
                    getFindDialog)()), "find next");
                menuHandler.registerWindowShower(aboutDialog(frame, frame.windowName),
                    "about");
                frame.showWindow();
            });
        } else if (is IMultiMapModel model) {
            for (map in model.allMaps) {
                startDriverOnModel(cli, options.copy(), ViewerModel.fromEntry(map));
            }
        } else {
            startDriverOnModel(cli, options, ViewerModel(model.map,
                model.mapFile));
        }
    }
    "Ask the user to choose a file or files."
    shared actual {PathWrapper+} askUserForFiles() {
        try {
            return SPFileChooser.open(null).files;
        } catch (FileChooser.ChoiceInterruptedException except) {
            throw DriverFailedException(except,
                "Choice interrupted or user didn't choose");
        }
    }
}
