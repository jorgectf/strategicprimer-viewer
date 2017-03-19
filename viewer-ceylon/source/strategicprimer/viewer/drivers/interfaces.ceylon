import java.lang {
    IllegalStateException,
    IllegalArgumentException
}
import model.misc {
    IDriverModel,
    IMultiMapModel
}
import util {
    Warning
}
import java.nio.file {
    JPaths = Paths, JPath = Path
}
import model.map {
    IMutableMapNG,
    HasName
}
import java.io {
    IOException
}
import lovelace.util.common {
    todo
}
import ceylon.interop.java {
    javaString
}
import ceylon.collection {
    MutableMap,
    HashMap
}
import strategicprimer.viewer.xmlio {
    readMultiMapModel,
    namesToFiles,
    writeModel
}
import javax.swing {
    JFrame,
    WindowConstants,
    JDialog,
    JComponent,
    KeyStroke,
    JMenuBar,
    JMenu,
    JMenuItem,
    InputMap
}
import java.awt {
    Dimension,
    Frame
}
import java.awt.event {
    ActionEvent,
    KeyEvent
}
import lovelace.util.jvm {
    createHotKey,
    HotKeyModifier,
    createAccelerator,
    createMenuItem,
    platform,
    ActionWrapper
}
import com.apple.eawt {
    Application, AppEvent
}
import com.bric.window {
    WindowList
}
import strategicprimer.viewer.drivers.worker_mgmt {
    IWorkerModel
}
import strategicprimer.viewer.drivers.map_viewer {
    IViewerModel
}
"""An interface for the command-line options passed by the user. At this point we
   assume that if any option is passed to an app more than once, the subsequent option
   overrides the previous, and any option passed without argument has an implied argument
   of "true"."""
shared interface SPOptions satisfies Iterable<String->String> {
    "Whether the specified option was given, with or without an argument."
    shared formal Boolean hasOption(String option);
    """Get the argument provided for the given argument ("true" if given without one,
       "false" if not given by the user)."""
    shared formal String getArgument(String option);
    "Clone the object."
    shared formal SPOptions copy();
}
"""The command-line options passed by the user. At this point we assume that if any option
   is passed to an app more than once, the subsequent option overrides the previous, and
   any option passed without argument has an implied argument of "true"."""
class SPOptionsImpl({<String->String>*} existing = {}) satisfies SPOptions {
    MutableMap<String, String> options = HashMap<String, String>();
    options.putAll(existing);
    shared void addOption(String option, String argument = "true") {
        if ("false" == argument) {
            options.remove(option);
        } else {
            options.put(option, argument);
        }
    }
    shared actual Boolean hasOption(String option) => options.defines(option);
    shared actual String getArgument(String option) => options.get(option) else "false";
    shared actual SPOptionsImpl copy() => SPOptionsImpl(options);
    shared actual String string {
        StringBuilder builder = StringBuilder();
        for (key->val in options) {
            if (val == "true") {
                builder.append((key));
            } else {
                builder.append("``key``=``val``");
            }
            builder.appendNewline();
        }
        return builder.string;
    }
    shared actual Iterator<String->String> iterator() => options.iterator();

}
"""An interface to allow utility drivers, which operate on files rather than a map model,
   to be a "functional" (single-method-to-implement) interface"""
shared interface UtilityDriver satisfies ISPDriver {
    shared actual default void startDriverOnModel(ICLIHelper cli, SPOptions options,
            IDriverModel model) {
        throw DriverFailedException(IllegalStateException(
            "A utility driver can't operate on a driver model"));
    }
}
"An interface for drivers which operate on a map model of some kind."
shared interface SimpleDriver satisfies ISPDriver {
    "(Try to) run the driver. If the driver does not need arguments, it should
     override this default method to support that; otherwise, this will throw,
     because nearly all drivers do need arguments."
    shared default void startDriverNoArgs() {
        throw DriverFailedException(
            IllegalStateException("Driver does not support no-arg operation"),
            "Driver does not support no-arg operation");
    }
    "The one method that satisfying classes have to implement."
    shared actual formal void startDriverOnModel(ICLIHelper cli, SPOptions options,
        IDriverModel model);
    "Ask the user to choose a file."
    JPath askUserForFile() {
        try {
            return FileChooser.open(null).file;
        } catch (FileChooser.ChoiceInterruptedException except) {
            throw DriverFailedException(except,
                "Choice interrupted or user didn't choose");
        }
    }
    """Run the driver. If the driver is a GUI driver, this should use
       SwingUtilities.invokeLater(); if it's a CLI driver, that's not necessary. This
       default implementation does *not* write to file after running the driver on the
       driver model, as GUIs will expose a "save" option in their UI."""
    shared actual default void startDriverOnArguments(ICLIHelper cli,
            SPOptions options, String* args) {
        ParamCount desiderata = usage.paramsWanted;
        Anything(IMutableMapNG) turnFixer;
        if (options.hasOption("--current-turn")) {
            if (is Integer currentTurn =
                    Integer.parse(options.getArgument("--current-turn"))) {
                turnFixer = (IMutableMapNG map) => map.setCurrentTurn(currentTurn);
            } else {
                log.warn("--current-turn must be an integer");
                turnFixer = (IMutableMapNG map) {};
            }
        } else {
            turnFixer = (IMutableMapNG map) {};
        }
        if (args.size == 0) {
            if ({ ParamCount.none, ParamCount.anyNumber }.contains(desiderata)) {
                // FIXME: Make "no-arg" form take CLI and options
                // The Java version called startDriver(cli, options), which recurses.
                startDriverNoArgs();
            } else if ({ ParamCount.two, ParamCount.atLeastTwo }.contains(desiderata)) {
                JPath masterPath = askUserForFile();
                JPath subordinatePath = askUserForFile();
                IMultiMapModel mapModel = readMultiMapModel(Warning.default, masterPath,
                    subordinatePath);
                for (pair in mapModel.allMaps) {
                    turnFixer(pair.first());
                }
                startDriverOnModel(cli, options, mapModel);
            } else {
                // TODO: Maybe just use readMapModel() here?
                IMultiMapModel mapModel = readMultiMapModel(Warning.default,
                    askUserForFile());
                for (pair in mapModel.allMaps) {
                    turnFixer(pair.first());
                }
                startDriverOnModel(cli, options, mapModel);
            }
        } else if (ParamCount.none == desiderata) {
            throw IncorrectUsageException(usage);
        } else if (args.size == 1,
                {ParamCount.two, ParamCount.atLeastTwo}.contains(desiderata)) {
            assert (exists firstArg = args.first);
            IMultiMapModel mapModel = readMultiMapModel(Warning.default,
                JPaths.get(firstArg), askUserForFile());
            for (pair in mapModel.allMaps) {
                turnFixer(pair.first());
            }
            startDriverOnModel(cli, options, mapModel);
        } else {
            assert (exists firstArg = args.first);
            assert (nonempty temp = args.map(javaString).sequence());
            IMultiMapModel mapModel = readMultiMapModel(Warning.default, JPaths.get(firstArg),
                *namesToFiles(false, *args.rest));
            for (pair in mapModel.allMaps) {
                turnFixer(pair.first());
            }
            startDriverOnModel(cli, options, mapModel);
        }
    }
}
"An interface for drivers, so one main() method can start different drivers based on
 options."
shared interface ISPDriver satisfies HasName {
    // In the Java implementation, these were overloads; Ceylon doesn't allow that,
    // which is in general better but a pain here.
    "Run the driver. If the driver is a GUI driver, this should use
     SwingUtilities.invokeLater(); if it's a CLI driver, that's not necessary.
     This form should only be used if the caller doesn't have an ICLIHelper to pass in."
    todo("Return exception instead of throwing?")
    shared default void startDriverOnArgumentsNoCLI(SPOptions options, String* args) {
        try (ICLIHelper cli = CLIHelper()) {
            startDriverOnArguments(cli, options, *args);
        } catch (IOException except) { // TODO: what will a Ceylon ICLIHelper throw?
            throw DriverFailedException(except, "I/O error interacting with user");
        }
    }
    "Run the driver. If the driver is a GUI driver, this should use
     SwingUtilities.invokeLater(); if it's a CLI driver, that's not necessary."
    shared formal void startDriverOnArguments(
        "The interface to interact with the user, either on the console or in a window
         emulating a console"
        ICLIHelper cli,
        "Any (already-processed) command-line options"
        SPOptions options,
        "Any command-line arguments that should be passed to the driver"
        String* args
    );
    "Run the driver on a driver model.
     This form should only be used if the caller doesn't have an ICLIHelper to pass in."
    shared default void startDriverOnModelNoCLI(SPOptions options, IDriverModel model) {
        try (ICLIHelper cli = CLIHelper()) {
            startDriverOnModel(cli, options, model);
        } catch (IOException except) { // TODO: what will a Ceylon ICLIHelper throw?
            throw DriverFailedException(except, "I/O error interacting with user");
        }
    }
    "Run the driver on a driver model."
    todo("Rename back to startDriver() and take String*|IDriverModel")
    shared formal void startDriverOnModel(
        "The interface to interact with the user, either on the console or in a window
         emulating a console"
        ICLIHelper cli,
        "Any (already-processed) command-line options"
        SPOptions options,
        "The driver-model that should be used by the app."
        IDriverModel model
    );
    """The usage object for the driver. The default implementation throws, to allow
       satisfying interfaces to be "functional" (single-formal-method) interfaces, but
       implementations *should* implement this."""
    shared formal IDriverUsage usage;
    "What to call this driver in a CLI list."
    shared actual default String name => usage.shortDescription;
}
"An interface for drivers which operate on a map model of some kind and want to write it
 out again to file when they finish."
shared interface SimpleCLIDriver satisfies SimpleDriver {
    "Run the driver. This is the one method that implementations must implement."
    shared actual formal void startDriverOnModel(ICLIHelper cli, SPOptions options,
        IDriverModel model);
    "Run the driver. If the driver is a GUIDriver, this should use
     SwingUtilities.invokeLater(); if it's a CLI driver, that's not necessary. This
     default implementation assumes a CLI driver, and writes the model back to file(s)
     after calling startDriver with the model."
    // TODO: remove optional from String args after ISPDriver ported
    shared actual default void startDriverOnArguments(ICLIHelper cli, SPOptions options,
            String* args) {
        switch (usage.paramsWanted)
        case (ParamCount.none) {
            if (args.size == 0) {
                super.startDriverNoArgs();
                return;
            } else {
                throw IncorrectUsageException(usage);
            }
        }
        case (ParamCount.anyNumber) {
            if (args.size == 0) {
                super.startDriverNoArgs();
                return;
            }
        }
        case (ParamCount.atLeastOne) {
            if (args.size == 0) {
                throw IncorrectUsageException(usage);
            }
        }
        case (ParamCount.one) {
            if (args.size != 1) {
                throw IncorrectUsageException(usage);
            }
        }
        case (ParamCount.two) {
            if (args.size != 2) {
                throw IncorrectUsageException(usage);
            }
        }
        case (ParamCount.atLeastTwo) {
            if (args.size < 2) {
                throw IncorrectUsageException(usage);
            }
        }
        assert (exists firstArg = args.first);
        assert (nonempty temp = args.map(javaString).sequence());
        // We declare this as IMultiMapModel so we can correct the current turn in all
        // maps if needed.
        IMultiMapModel model = readMultiMapModel(Warning.ignore, JPaths.get(firstArg),
            *namesToFiles(false, *args.rest));
        if (options.hasOption("--current-turn")) {
            if (is Integer currentTurn =
                    Integer.parse(options.getArgument("--current-turn"))) {
                for (pair in model.allMaps) {
                    pair.first().setCurrentTurn(currentTurn);
                }
            } else {
                cli.println("--current-turn must be an integer");
            }
        }
        startDriverOnModel(cli, options, model);
        writeModel(model);
    }
}
"An exception to throw whenever a driver fails, so drivers only have to directly handle
 one exception class."
todo("Is this really necessary any more?")
shared class DriverFailedException(Throwable cause,
        String message = "The driver could not start because of an exception:")
        extends Exception(message, cause) { }
"An exception to throw when a driver fails because the user tried to use it improperly."
shared class IncorrectUsageException(correctUsage)
        extends DriverFailedException(IllegalArgumentException("Incorrect usage"),
    "Incorrect usage") {
    """The "usage object" for the driver, describing its correct usage."""
    shared IDriverUsage correctUsage;
}
"Possible numbers of (non-option?) parameters a driver might shared want."
shared class ParamCount of none | one | two | atLeastOne | atLeastTwo | anyNumber {
    "None at all."
    shared new none { }
    "Exactly one."
    shared new one { }
    "Exactly two."
    shared new two { }
    "One or more."
    shared new atLeastOne { }
    "Two or more."
    shared new atLeastTwo { }
    "Zero or more."
    shared new anyNumber { }
}
"An interface for objects representing usage information for drivers, for use in the app
 starter and in help text."
todo("Make shashared shared red?")
shared interface IDriverUsage {
    "Whether the driver is a GUI."
    shared formal Boolean graphical;
    "The short option to select this driver."
    shared formal String shortOption;
    "The long option to select this driver."
    shared formal String longOption;
    "How many non-option parameters this driver wants."
    shared formal ParamCount paramsWanted;
    "A short (one-line at most) description of the driver."
    shared formal String shortDescription;
    "A long(er) description of the driver."
    shared formal String longDescription;
    "A description of the first parameter for use in a usage statement."
    shared formal String firstParamDescription;
    "A description of parameters other than the first for use in a usage statement."
    shared formal String subsequentParamDescription;
    """Options this driver supports. (To show the user, so "=NN" to mean a numeric option
       is reasonable."""
    shared formal {String*} supportedOptions;
}
shared class DriverUsage(
    "Whether this driver is graphical or not."
    shared actual Boolean graphical,
    "The short (generally one character) option to give to the app-starter to get this
     driver."
    shared actual String shortOption,
    "The long option to give to the app-starter to get this driver."
    shared actual String longOption,
    "How many parameters this driver wants."
    shared actual ParamCount paramsWanted,
    "A short description of the driver"
    shared actual String shortDescription,
    "A longer description of the driver"
    shared actual String longDescription,
    "A description of the first (non-option) parameter, for use in a usage statement."
    shared actual String firstParamDescription = "filename.xml",
    "A description of a later parameter, for use in a usage statement. (We assume that all
     parameters after the first should be described similarly.)"
    shared actual String subsequentParamDescription = "filename.xml",
    "The options this driver supports."
    String* supportedOptionsTemp
) satisfies IDriverUsage {
    shared actual {String*} supportedOptions =
            supportedOptionsTemp;
}
"An interface for top-level windows in assistive programs."
shared interface ISPWindow {
    """The name of this window. This method should *not* return a string including the
       loaded file, since it is used only in the About dialog to "personalize" it for the
       particular app."""
    shared formal String windowName;
}
"An intermediate subclass of JFrame to take care of some common setup things that can't be
 done in an interface."
shared abstract class SPFrame(String windowTitle, JPath? file, Dimension? minSize = null)
        extends JFrame(windowTitle) satisfies ISPWindow {
    if (exists file) {
        title = "``file`` | ``windowTitle``";
        rootPane.putClientProperty("Window.documentFile", file.toFile());
    }
    defaultCloseOperation = WindowConstants.disposeOnClose;
    if (exists minSize) {
        setMinimumSize(minSize);
    }
}
"A superclass to perform setup common to dialogs."
shared class SPDialog(Frame? parentFrame, String title)
        extends JDialog(parentFrame, title) {
    defaultCloseOperation = WindowConstants.disposeOnClose;
    createHotKey(rootPane, "close", ActionWrapper((ActionEvent event) => dispose()),
        JComponent.whenInFocusedWindow, KeyStroke.getKeyStroke(KeyEvent.vkW,
            platform.shortcutMask), KeyStroke.getKeyStroke(KeyEvent.vkEscape, 0));
}
"A class to hold the logic for building our menus."
todo("Make the methods static once MenuItemCreator has been ported.",
    "Redesign so users just have to say which menus they want enabled instead of
     instantiatng them one by one")
shared class SPMenu() extends JMenuBar() {
    "Create the file menu."
    shared JMenu createFileMenu(/*ActionListener|*/Anything(ActionEvent) handler,
            IDriverModel model) {
        JMenu fileMenu = JMenu("File");
        fileMenu.mnemonic = KeyEvent.vkF;
        JMenuItem newItem = createMenuItem("New", KeyEvent.vkN,
            createAccelerator(KeyEvent.vkN),
            "Create a new, empty map the same size as the current one", handler);
        fileMenu.add(newItem);
        if (!model is IViewerModel) {
            newItem.enabled = false;
        }
        String loadCaption;
        String saveCaption;
        String saveAsCaption;
        if (model is IMultiMapModel) { // TODO: use interpolation of "the main" instead
            loadCaption = "Load the main map from file";
            saveCaption = "Save the main map to the file it was loaded from";
            saveAsCaption = "Save the main map to file";
        } else {
            loadCaption = "Load a map from file";
            saveCaption = "Save the map to the file it was loaded from";
            saveAsCaption = "Save the map to file";
        }
        fileMenu.add(createMenuItem("Load", KeyEvent.vkL, createAccelerator(KeyEvent.vkO),
            loadCaption, handler));
        JMenuItem loadSecondaryItem = createMenuItem("Load secondary", KeyEvent.vkE,
            createAccelerator(KeyEvent.vkO, HotKeyModifier.shift),
            "Load an additional secondary map from file", handler);
        fileMenu.add(loadSecondaryItem);
        fileMenu.add(createMenuItem("Save", KeyEvent.vkS, createAccelerator(KeyEvent.vkS),
            saveCaption, handler));
        fileMenu.add(createMenuItem("Save As", KeyEvent.vkA,
            createAccelerator(KeyEvent.vkS, HotKeyModifier.shift), saveAsCaption,
            handler));
        JMenuItem saveAllItem = createMenuItem("Save All", KeyEvent.vkV,
            createAccelerator(KeyEvent.vkL), "Save all maps to their files", handler);
        fileMenu.add(saveAllItem);
        if (!model is IMultiMapModel) {
            loadSecondaryItem.enabled = false;
            saveAllItem.enabled = false;
        }
        fileMenu.addSeparator();
        JMenuItem openViewerItem = createMenuItem("Open in map viwer", KeyEvent.vkM,
            createAccelerator(KeyEvent.vkM),
            "Open the main map in the map viewer for a broader view", handler);
        fileMenu.add(openViewerItem);
        if (model is IViewerModel) {
            openViewerItem.enabled = false;
        }
        JMenuItem openSecondaryViewerItem = createMenuItem(
            "Open secondary map in map viewer", KeyEvent.vkE,
            createAccelerator(KeyEvent.vkE),
            "Open the first secondary map in the map vieer for a broader view", handler);
        fileMenu.add(openSecondaryViewerItem);
        if (model is IViewerModel || !model is IMultiMapModel) {
            openSecondaryViewerItem.enabled = false;
        }
        fileMenu.addSeparator();
        if (platform.systemIsMac) {
            Application.application.setAboutHandler((AppEvent.AboutEvent event) {
                Object source = WindowList.getWindows(true, false).iterable.coalesced
                    .sequence().reversed.first else event;
                handler(ActionEvent(source, ActionEvent.actionFirst,
                    "About"));
            });
        } else {
            fileMenu.add(createMenuItem("About", KeyEvent.vkB,
                createAccelerator(KeyEvent.vkB), "Show development credits", handler));
            fileMenu.addSeparator();
            fileMenu.add(createMenuItem("Quit", KeyEvent.vkQ,
                createAccelerator(KeyEvent.vkQ), "Quit the application", handler));
        }
        return fileMenu;
    }
    """Create the "map" menu, including go-to-tile, find, and zooming functions."""
    shared JMenu createMapMenu(Anything(ActionEvent) handler, IDriverModel model) {
        JMenu retval = JMenu("Map");
        retval.mnemonic = KeyEvent.vkM;
        Integer findKey = KeyEvent.vkF;
        KeyStroke findStroke = createAccelerator(findKey);
        KeyStroke nextStroke = createAccelerator(KeyEvent.vkG);
        JMenuItem gotoTileItem = createMenuItem("Go to tile", KeyEvent.vkT,
            createAccelerator(KeyEvent.vkT), "Go to a tile by coordinates", handler);
        JMenuItem findItem = createMenuItem("Find a fixture", findKey, findStroke,
            "Find a fixture by name, kind or ID #", handler);
        Integer nextKey = KeyEvent.vkN;
        JMenuItem nextItem = createMenuItem("Find next", nextKey, nextStroke,
            "Find the next fixture matching the pattern", handler);
        if (!model is IViewerModel) {
            gotoTileItem.enabled = false;
            findItem.enabled = false;
            nextItem.enabled = false;
        }
        retval.add(gotoTileItem);
        InputMap findInput = findItem.getInputMap(JComponent.whenInFocusedWindow);
        findInput.put(KeyStroke.getKeyStroke(KeyEvent.vkSlash, 0),
            findInput.get(findStroke));
        retval.add(findItem);
        InputMap nextInput = nextItem.getInputMap(JComponent.whenInFocusedWindow);
        nextInput.put(KeyStroke.getKeyStroke(nextKey, 0), nextInput.get(nextStroke));
        retval.add(nextItem);
        retval.addSeparator();
        // vkPlus only works on non-US keyboards, but we leave it as the primary hot-key
        // because it's the best to *show* in the menu.
        KeyStroke plusKey = createAccelerator(KeyEvent.vkPlus);
        JMenuItem zoomInItem = createMenuItem("Zoom in", KeyEvent.vkI, plusKey,
            "Increase the visible size of each tile", handler);
        InputMap zoomInInputMap = zoomInItem.getInputMap(JComponent.whenInFocusedWindow);
        zoomInInputMap.put(createAccelerator(KeyEvent.vkEquals), inputMap.get(plusKey));
        zoomInInputMap.put(createAccelerator(KeyEvent.vkEquals, HotKeyModifier.shift),
            inputMap.get(plusKey));
        zoomInInputMap.put(createAccelerator(KeyEvent.vkAdd), inputMap.get(plusKey));
        retval.add(zoomInItem);
        retval.add(createMenuItem("Zoom out", KeyEvent.vkO,
            createAccelerator(KeyEvent.vkMinus), "Decrease the visible size of each tile",
            handler));
        // TODO: Shouldn't there be a "reset zoom" item?
        retval.addSeparator();
        retval.add(createMenuItem("Center", KeyEvent.vkC, createAccelerator(KeyEvent.vkC),
            "Center the view on the selected tile", handler));
        return retval;
    }
    """Create the "view" menu."""
    shared JMenu createViewMenu(Anything(ActionEvent) handler, IDriverModel model) {
        JMenu viewMenu = JMenu("View");
        viewMenu.mnemonic = KeyEvent.vkE;

        // We *create* these items here (early) so that we can enable or disable them
        // without an extra branch.
        // TODO: create Ceylon Iterable instead of add()ing to a List
        {JMenuItem*} treeItems = {
            createMenuItem("Reload tree", KeyEvent.vkR, createAccelerator(KeyEvent.vkR),
                "Refresh the view of the workers", handler),
            createMenuItem("Expand All", KeyEvent.vkX, null,
                "Expand all nodes in the unit tree", handler),
            createMenuItem("Expand Unit Kinds", KeyEvent.vkK, null,
                "Expand all unit kinds to show the units", handler),
            createMenuItem("Collapse All", KeyEvent.vkC, null,
                "Collapse all nodes in the unit tree", handler)
        };
        JMenuItem currentPlayerItem;
        if (is IWorkerModel model) {
            currentPlayerItem = createMenuItem("Change current player", KeyEvent.vkP,
                createAccelerator(KeyEvent.vkP),
                "Look at a different player's units and workers", handler);
        } else {
            currentPlayerItem = createMenuItem("Change current player", KeyEvent.vkP,
                null, "Mark a player as the current player in the map", handler);
            for (item in treeItems) {
                item.enabled = false;
            }
        }
        viewMenu.add(currentPlayerItem);
        for (item in treeItems) {
            viewMenu.add(item);
        }
        return viewMenu;
    }
    "Add a menu, but set it to disabled."
    shared JMenu addDisabled(JMenu menu) {
        add(menu);
        menu.enabled = false;
        return menu;
    }
}
