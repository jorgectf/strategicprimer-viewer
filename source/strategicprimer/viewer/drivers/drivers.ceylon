import ceylon.collection {
    ArrayList,
    MutableList
}
import ceylon.language.meta.declaration {
    Package,
    Module
}
import ceylon.logging {
    Logger,
    logger,
    addLogWriter,
    Priority,
    trace,
    debug,
    defaultPriority
}

import java.awt {
    GraphicsEnvironment,
    GridLayout,
    Dimension,
    Graphics2D
}
import java.io {
    IOException
}
import java.lang {
    System
}

import javax.swing {
    UIManager,
    SwingUtilities,
    JPanel,
    JScrollPane,
    JLabel,
    JEditorPane
}

import lovelace.util.jvm {
    showErrorDialog,
    BorderedPanel,
    platform,
    ListenedButton
}

import strategicprimer.drivers.common {
    IDriverModel,
    IDriverUsage,
    SPOptions,
    ParamCount,
    DriverFailedException,
    IncorrectUsageException,
    SPOptionsImpl,
    CLIDriver,
    IMultiMapModel,
    DriverFactory,
    GUIDriverFactory,
    UtilityDriverFactory,
    ModelDriverFactory,
    UtilityGUI
}
import strategicprimer.drivers.common.cli {
    ICLIHelper,
    CLIHelper
}
import com.apple.eawt {
    AppEvent,
    Application
}
import com.pump.window {
    WindowList
}
import strategicprimer.drivers.gui.common {
    SPFrame,
    WindowCloseListener,
    SPMenu
}
import strategicprimer.model.common.xmlio {
    SPFormatException,
    warningLevels
}
import lovelace.util.common {
    todo,
    defer,
    silentListener,
    PathWrapper,
    MissingFileException,
    matchingValue
}
import com.vasileff.ceylon.structures {
    MutableMultimap,
    ArrayListMultimap,
    HashMultimap
}
import java.awt.image {
    BufferedImage
}
import strategicprimer.model.impl.xmlio {
    mapIOHelper
}

"A logger."
Logger log = logger(`module strategicprimer.viewer`);

object appChooserState {
    "The method to actually write log messages to stderr."
    shared void logWriter(Priority priority, Module|Package mod,
            String message, Throwable? except) {
        process.writeErrorLine("``priority`` (``mod``): ``message``");
        if (exists except) {
            process.writeErrorLine(except.message);
            except.printStackTrace();
        }
    }

    "Create the cache of driver objects."
    shared Map<String, {DriverFactory*}> createCache() {
        MutableMultimap<String, DriverFactory> cache =
            HashMultimap<String, DriverFactory>();
        MutableMultimap<String, DriverFactory> conflicts =
                ArrayListMultimap<String, DriverFactory>();
        for (factory in `module strategicprimer.viewer`
                .findServiceProviders(`DriverFactory`)) {
            value command = factory.usage.invocation;
            if (command.startsWith("-")) {
                log.error("A driver wants to register an option, ``command
                    ``, not a subcommand");
            } else if (conflicts.defines(command)) {
                log.warn("Additional conflict for '``command``': '``
                    factory.usage.shortDescription``'");
                conflicts.put(command, factory);
            } else if (exists existing = cache.get(command)
                    .find(matchingValue(factory.usage.graphical,
                        compose(IDriverUsage.graphical, DriverFactory.usage)))) {
                log.warn("Invocation command conflict for '``command``' between '``
                    factory.usage.shortDescription``' and '``
                    existing.usage.shortDescription``'");
                conflicts.put(command, factory);
                conflicts.put(command, existing);
                cache.remove(command, existing);
            } else {
                cache.put(command, factory);
            }
        }
        return cache.asMap;
    }

    "Create the usage message for a particular driver."
    shared String usageMessage(IDriverUsage usage, Boolean verbose) {
        StringBuilder builder = StringBuilder();
        builder.append("Usage: ");
        String mainInvocation;
        if (exists invocationResource = `module strategicprimer.viewer`
                .resourceByPath("invocation")) {
            mainInvocation = invocationResource.textContent().trimmed;
        } else {
            mainInvocation =
                    "ceylon --cwd=. run `` `module strategicprimer.viewer`.name``";
        }
        builder.append(mainInvocation);
        if (usage.graphical) {
            builder.append(" [-g|--gui] ");
        } else {
            builder.append(" -c|--cli ");
        }
        builder.append(usage.invocation);
        for (option in usage.supportedOptions) {
            builder.append(" [``option``]");
        }
        switch (usage.paramsWanted)
        case (ParamCount.none) {}
        case (ParamCount.one) { builder.append(" ``usage.firstParamDescription``"); }
        case (ParamCount.atLeastOne) {
            builder.append(" ``usage.firstParamDescription`` [``
                usage.subsequentParamDescription`` ...]");
        }
        case (ParamCount.two) {
            builder.append(" ``usage.firstParamDescription`` ``
                usage.subsequentParamDescription``");
        }
        case (ParamCount.atLeastTwo) {
            builder.append(" ``usage.firstParamDescription`` ``
                usage.subsequentParamDescription`` [``
                usage.subsequentParamDescription`` ...]");
        }
        case (ParamCount.anyNumber) {
            builder.append(" [``usage.subsequentParamDescription`` ...]");
        }
        builder.appendNewline();
        if (verbose) {
            builder.append(usage.longDescription);
        } else {
            builder.append(usage.shortDescription);
        }
        return builder.string;
    }

    shared void handleDroppedFiles(AppEvent.OpenFilesEvent openFilesEvent) {
        if (exists topWindow = WindowList.getWindows(true, false)
                .iterable.narrow<SPFrame>().last) {
            for (file in openFilesEvent.files) {
                topWindow.acceptDroppedFile(PathWrapper(file.toPath().string));
            }
        }
    }
}

class DriverWrapper(DriverFactory factory) {
    Boolean enoughArguments(Integer argc) {
        assert (argc >= 0);
        switch (factory.usage.paramsWanted)
        case (ParamCount.none|ParamCount.anyNumber) {
            return true;
        }
        case (ParamCount.one|ParamCount.atLeastOne) {
            return argc >= 1;
        }
        case (ParamCount.two|ParamCount.atLeastTwo) {
            return argc >= 2;
        }
    }

    Boolean tooManyArguments(Integer argc) {
        assert (argc >= 0);
        switch (factory.usage.paramsWanted)
        case (ParamCount.anyNumber|ParamCount.atLeastOne|ParamCount.atLeastTwo) {
            return false;
        }
        case (ParamCount.none) {
            return argc > 0;
        }
        case (ParamCount.one) {
            return argc > 1;
        }
        case (ParamCount.two) {
            return argc > 2;
        }
    }

    void checkArguments(String* args) {
        if (!enoughArguments(args.size) || tooManyArguments(args.size)) {
            throw IncorrectUsageException(factory.usage);
        }
    }

    {PathWrapper*} extendArguments(String* args) {
        if (is GUIDriverFactory factory) {
            MutableList<PathWrapper> files;
            if (nonempty temp = args.sequence()) {
                files = ArrayList {
                    elements = mapIOHelper.namesToFiles(*temp); };
            } else {
                files = ArrayList<PathWrapper>();
            }
            if (tooManyArguments(files.size)) {
                throw IncorrectUsageException(factory.usage);
            }
            while (!enoughArguments(files.size) &&
                    !tooManyArguments(files.size + 1)) {
                value requested = factory.askUserForFiles();
                if (requested.empty || tooManyArguments(files.size + requested.size)) {
                    throw IncorrectUsageException(factory.usage);
                } else {
                    files.addAll(requested);
                }
            }
            return files;
        } else if (nonempty temp = args.sequence()) {
            return mapIOHelper.namesToFiles(*temp);
        } else {
            return [];
        }
    }

    void fixCurrentTurn(SPOptions options, IDriverModel model) {
        if (options.hasOption("--current-turn")) {
            if (is Integer currentTurn =
                    Integer.parse(options.getArgument("--current-turn"))) {
                if (is IMultiMapModel model) {
                    for (map->_ in model.allMaps) {
                        map.currentTurn = currentTurn;
                    }
                } else {
                    model.map.currentTurn = currentTurn;
                }
            }
        }
    }

    shared void startCatchingErrors(ICLIHelper cli, SPOptions options, String* args) {
        try {
            switch (factory)
            case (is UtilityDriverFactory) {
                checkArguments(*args);
                factory.createDriver(cli, options).startDriver(*args);
            }
            case (is ModelDriverFactory) {
                if (is GUIDriverFactory factory) {
                    if (ParamCount.one == factory.usage.paramsWanted, args.size > 1) {
                        for (arg in args) {
                            startCatchingErrors(cli, options, arg);
                        }
                    } else {
                        assert (nonempty files = extendArguments(*args).sequence());
                        value model = mapReaderAdapter.readMultiMapModel(
                            warningLevels.warn, files.first, *files.rest);
                        fixCurrentTurn(options, model);
                        factory.createDriver(cli, options, model).startDriver();
                    }
                } else {
                    checkArguments(*args);
                    assert (nonempty args);
                    value files = mapIOHelper.namesToFiles(*args);
                    value model = mapReaderAdapter.readMultiMapModel(warningLevels.warn,
                        files.first, *files.rest);
                    fixCurrentTurn(options, model);
                    value driver = factory.createDriver(cli, options, model);
                    driver.startDriver();
                    if (is CLIDriver driver) {
                        mapReaderAdapter.writeModel(model);
                    }
                }
            }
        } catch (IncorrectUsageException except) {
            cli.println(appChooserState.usageMessage(except.correctUsage,
                options.getArgument("--verbose") == "true"));
        } catch (DriverFailedException except) {
            if (is SPFormatException cause = except.cause) {
                log.error(cause.message);
            } else if (exists cause = except.cause) {
                log.error("Driver failed:", cause);
            } else {
                log.error("Driver failed:", except);
            }
        } catch (MissingFileException except) {
            log.error("File ``except.filename`` not found");
            log.trace("File-not-found stack trace:", except);
        } catch (Exception except) {
            log.error(except.message, except);
        }
    }
}

class AppStarter() {
    Map<String, {DriverFactory*}> driverCache =
            appChooserState.createCache(); // TODO: Can we inline that into here?

    Boolean includeInCLIList(DriverFactory driver) => driver.usage.includeInList(false);

    shared void startDriverOnArguments(ICLIHelper cli, SPOptions options, String* args) {
        //            log.info("Inside appStarter.startDriver()");
        variable Boolean gui = !GraphicsEnvironment.headless;
        variable SPOptionsImpl currentOptions = SPOptionsImpl(options);
        if (!currentOptions.hasOption("--gui")) {
            currentOptions.addOption("--gui", gui.string);
        }
        MutableList<String> others = ArrayList<String>();
        void startChosenDriver(DriverFactory driver, SPOptions currentOptionsTyped) {
            if (driver.usage.graphical) {
                value lambda = defer(DriverWrapper(driver).startCatchingErrors,  // TODO: inline once eclipse/ceylon#7379 fixed
                    [cli, currentOptionsTyped, *others.rest]);
                SwingUtilities.invokeLater(lambda);
            } else {
                DriverWrapper(driver)
                    .startCatchingErrors(cli, currentOptionsTyped, *others.rest);
            }
        }

        for (arg in args.coalesced) {
            if (arg == "-g" || arg == "--gui") {
                log.trace("User specified either -g or --gui");
                currentOptions.addOption("--gui");
                gui = true;
            } else if (arg == "-c" || arg == "--cli") {
                log.trace("User specified either -c or --cli");
                currentOptions.addOption("--gui", "false");
                gui = false;
            } else if (arg.startsWith("--gui=")) {
                String tempString = arg.substring(6);
                log.trace("User specified --gui=``tempString``");
                value tempBool = Boolean.parse(tempString);
                if (is Boolean tempBool) {
                    currentOptions.addOption("--gui", tempString);
                    gui = tempBool;
                } else {
                    throw DriverFailedException(tempBool, "--gui=nonBoolean");
                }
            } else if (arg.startsWith("-") && arg.contains("=")) {
                {String+} broken = arg.split('='.equals, true, false);
                currentOptions.addOption(broken.first, broken.rest.reduce<String>(
                    (String partial, String element) =>
                            if (partial.empty) then element
                                else "``partial``=``element``")
                else "");
                log.trace(
                    "User specified ``broken.first``=``broken.rest.first else ""``");
            } else if (arg.startsWith("-")) {
                log.trace("User specified non-app-choosing option ``arg``");
                currentOptions.addOption(arg);
            } else {
                log.trace("User specified non-option argument ``arg``");
                others.add(arg);
            }
        }

        log.trace("Reached the end of arguments");
        // TODO: Use appletChooser so we can support prefixes
        DriverFactory? currentDriver;
        if (exists command = others.first, exists drivers = driverCache[command],
                exists first = drivers.first) {
            log.trace("Found a driver or drivers");
            if (drivers.rest.empty) {
                log.trace("Only one driver registered for that command");
                currentDriver = first;
            } else {
                log.trace("Multiple drivers registered; filtering by interface");
                currentDriver = drivers.find(matchingValue(gui,
                    compose(IDriverUsage.graphical, DriverFactory.usage)));
            }
        } else {
            log.trace("No matching driver found");
            currentDriver = null;
        }
        if (currentOptions.hasOption("--help")) {
            if (exists currentUsage = currentDriver?.usage) {
                log.trace("Giving usage information for selected driver");
                process.writeLine(appChooserState.usageMessage(currentUsage,
                    options.getArgument("--verbose") == "true"));
            } else {
                log.trace("No driver selected, so giving choices.");
                process.writeLine("Strategic Primer assistive programs suite");
                process.writeLine(
                    "No app specified; use one of the following invocations:");
                process.writeLine();
                for (driver in driverCache.flatMap(Entry.item).distinct) {
                    value lines = appChooserState.usageMessage(driver.usage,
                        options.getArgument("--verbose") == "true").lines;
                    String invocationExample = lines.first.replace("Usage: ", "");
                    String description =
                            lines.rest.first?.replace(".", "") else "An unknown app";
                    process.writeLine("``description``: ``invocationExample``");
                }
            }
        } else if (exists driver = currentDriver) {
            log.trace("Starting chosen app.");
            startChosenDriver(driver, currentOptions.copy());
        } else {
            log.trace("Starting app-chooser.");
            SPOptions currentOptionsTyped = currentOptions.copy();
            if (gui) {
                try {
                    value lambda = AppChooserGUI(cli, currentOptionsTyped).startDriver; // TODO: Inline once eclipse/ceylon#73739 fixed
                    SwingUtilities.invokeLater(defer(lambda, others.sequence()));
                } catch (DriverFailedException except) {
                    log.fatal(except.message, except);
                    SwingUtilities.invokeLater(defer(showErrorDialog, [null,
                        "Strategic Primer Assistive Programs", except.message]));
                }
            } else {
                if (exists chosenDriver = cli.chooseFromList(
                        driverCache.flatMap(Entry.item).distinct.filter(includeInCLIList)
                            .sequence(),
                        "CLI apps available:", "No applications available",
                        "App to start: ", true).item) {
                    DriverWrapper(chosenDriver).startCatchingErrors(cli, options,
                        *others);
                }
            }
        }
    }
}

todo("Try to combine/rearrange things so we have as few top-level and inner classes and
      `object`s as possible")
suppressWarnings("expressionTypeNothing")
shared void run() {
    addLogWriter(appChooserState.logWriter);
    System.setProperty("com.apple.mrj.application.apple.menu.about.name",
        "SP Helpers");
    System.setProperty("apple.awt.application.name", "SP Helpers");
    UIManager.setLookAndFeel(UIManager.systemLookAndFeelClassName);
    System.setProperty("apple.laf.useScreenMenuBar", "true");
    if (process.namedArgumentPresent("trace")) {
        defaultPriority = trace;
    } else if (process.namedArgumentPresent("debug")) {
        defaultPriority = debug;
    }
    log.debug("If you can see this, debug-level log messages are enabled.");
    log.trace("If you can see this, trace-level log messages are enabled.");
    SPOptionsImpl options = SPOptionsImpl();
    if (platform.systemIsMac) {
        Application.application.setOpenFileHandler(appChooserState.handleDroppedFiles);
    }
    AppStarter appStarter = AppStarter();
    try {
        appStarter.startDriverOnArguments(CLIHelper(), options, *process.arguments);
    } catch (IncorrectUsageException except) {
        IDriverUsage usage = except.correctUsage;
        process.writeErrorLine(appChooserState.usageMessage(usage,
            options.hasOption("--verbose")));
        process.exit(1);
    } catch (IOException|DriverFailedException except) {
        log.error(except.message, except.cause);
        process.exit(2);
    }
}

class AppChooserGUI(ICLIHelper cli, options) satisfies UtilityGUI {
    shared actual SPOptions options;
    MutableList<String> additionalFiles = ArrayList<String>();
    Boolean includeInGUIList(DriverFactory driver) => driver.usage.includeInList(true);
    shared actual void startDriver(String* args) {
        value tempComponent = JEditorPane();
        value font = tempComponent.font;
        assert (is Graphics2D pen = BufferedImage(1, 1, BufferedImage.typeIntRgb)
            .createGraphics());
        value context = pen.fontRenderContext;
        variable Integer width = 0;
        variable Integer height = 10;
        value drivers = `module strategicprimer.viewer`
            .findServiceProviders(`DriverFactory`).filter(includeInGUIList).sequence();
        for (driver in drivers) {
            value dimensions =
                font.getStringBounds(driver.usage.shortDescription, context);
            width = Integer.largest(width, dimensions.width.integer);
            height += dimensions.height.integer;
        }
        SPFrame frame = SPFrame("SP App Chooser", this, Dimension(width, height));
        JPanel buttonPanel = JPanel(GridLayout(0, 1));
        void buttonHandler(DriverFactory target) {
            try {
                DriverWrapper(target).startCatchingErrors(cli, options,
                    *(args.chain(additionalFiles)));
                SwingUtilities.invokeLater(() {
                    frame.setVisible(false);
                    frame.dispose();
                });
            } catch (IOException except) {
                log.error("I/O error prompting user for app to start", except);
                showErrorDialog(frame, "I/O error", except.message );
            } catch (DriverFailedException except) {
                if (is SPFormatException cause = except.cause) {
                    showErrorDialog(frame, except.message, cause.message);
                    log.error(cause.message);
                } else if (exists cause = except.cause) {
                    showErrorDialog(frame, except.message, cause.message);
                    log.error("Driver failed:", cause);
                } else {
                    showErrorDialog(frame, except.message, except.message);
                    log.error("Driver failed:", except);
                }
            } catch (Exception except) {
                showErrorDialog(frame, except.message, except.message);
                log.error(except.message, except);
            }
        }
        for (driver in drivers) {
            buttonPanel.add(ListenedButton(driver.usage.shortDescription,
                        defer(buttonHandler, [driver])));
        }
        value mainPanel = BorderedPanel.verticalPanel(
            JLabel("Please choose one of the applications below"),
            JScrollPane(buttonPanel), null);
        frame.contentPane = mainPanel;
        frame.pack();
        frame.jMenuBar = SPMenu.forWindowContaining(mainPanel,
            SPMenu.createFileMenu(IOHandler(this).actionPerformed, this),
            SPMenu.disabledMenu(SPMenu.createMapMenu(noop, this)),
            SPMenu.disabledMenu(SPMenu.createViewMenu(noop, this)));
        frame.addWindowListener(WindowCloseListener(silentListener(frame.dispose)));
        frame.setVisible(true);
    }
    shared actual void open(PathWrapper path) => additionalFiles.add(path.filename);
}
