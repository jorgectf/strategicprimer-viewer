import ceylon.collection {
    MutableMap,
    HashMap,
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
    Priority
}

import java.awt {
    GraphicsEnvironment,
    GridLayout,
    Dimension
}
import java.awt.event {
    ActionListener,
    ActionEvent
}
import java.io {
    IOException
}
import java.lang {
    System,
    Runnable
}

import javax.swing {
    UIManager,
    SwingUtilities,
    JButton,
    JPanel,
    JScrollPane,
    JLabel
}

import lovelace.util.jvm {
    showErrorDialog,
    BorderedPanel,
    platform
}

import strategicprimer.viewer.drivers.advancement {
    advancementCLI,
    advancementGUI
}
import strategicprimer.viewer.drivers.exploration {
    explorationGUI,
    explorationCLI
}
import strategicprimer.viewer.drivers.map_viewer {
    viewerGUI,
    drawHelperComparator
}
import strategicprimer.viewer.drivers.mining {
    miningCLI
}
import strategicprimer.drivers.utility.subset {
    subsetCLI,
    subsetGUI
}
import strategicprimer.viewer.drivers.worker_mgmt {
    workerGUI,
    strategyExportCLI
}
import strategicprimer.drivers.common {
    IDriverModel,
    ISPDriver,
    IDriverUsage,
    SPOptions,
    ParamCount,
    DriverFailedException,
    IncorrectUsageException,
    DriverUsage,
    SPOptionsImpl
}
import strategicprimer.drivers.common.cli {
    ICLIHelper
}
import com.apple.eawt {
    AppEvent,
    Application
}
import com.pump.window {
    WindowList
}
import strategicprimer.drivers.gui.common.about {
    aboutDialog
}
import strategicprimer.drivers.generators {
    townGeneratingCLI,
    statGeneratingCLI
}
import strategicprimer.drivers.gui.common {
    SPFrame,
    UtilityMenu
}
import strategicprimer.drivers.utility {
    mapCheckerCLI,
    readerComparator,
    mapCheckerGUI,
    duplicateFixtureRemoverCLI
}
import strategicprimer.model.xmlio {
    SPFormatException
}
import strategicprimer.viewer.drivers.query {
	queryCLI,
	trappingCLI
}
import lovelace.util.common {
	todo
}
"A logger."
Logger log = logger(`module strategicprimer.viewer`);
"The method to actually write log messages to stderr."
void logWriter(Priority priority, Module|Package mod,
        String message, Throwable? except) {
    process.writeErrorLine("``priority`` (``mod``): ``message``");
    if (exists except) {
        process.writeErrorLine(except.message);
        except.printStackTrace();
    }
}
"Create the cache of driver objects."
todo("FIXME: Get rid of the CLI/GUI (exact) pair idea and the short-option/long-option pair idea.
      Instead, each app should have a list of options it responds to, each of which should be unique
      among CLI or GUI apps.")
Map<String, ISPDriver[2]> createCache() {
    MutableMap<String, [ISPDriver, ISPDriver]> cache =
            HashMap<String, [ISPDriver, ISPDriver]>();
    void choices(ISPDriver cliDriver, ISPDriver guiDriver) {
        IDriverUsage cliUsage = cliDriver.usage;
        IDriverUsage guiUsage = guiDriver.usage;
        if (cliUsage.graphical) {
            log.warn("``cliUsage.shortDescription`` is GUI but passed as CLI");
        }
        if (!guiUsage.graphical) {
            log.warn("``guiUsage.shortDescription`` is CLI but passed as GUI");
        }
        if (cliUsage.shortOption != guiUsage.shortOption) {
            log.warn("Short options don't match between ``
                        cliUsage.shortDescription`` and ``guiUsage.shortDescription``");
        }
        if (cliUsage.longOption != guiUsage.longOption) {
            log.warn("Long options don't match between ``
                        cliUsage.shortDescription`` and ``guiUsage.shortDescription``");
        }
        cache[cliUsage.shortOption] = [cliDriver, guiDriver];
        cache[cliUsage.longOption] = [cliDriver, guiDriver];
    }
    void choice(ISPDriver driver) {
        IDriverUsage usage = driver.usage;
        cache[usage.shortOption] = [driver, driver];
        cache[usage.longOption] = [driver, driver];
    }
    choices(reportCLI, viewerGUI);
    choices(advancementCLI, advancementGUI);
    choices(strategyExportCLI, workerGUI);
    choices(explorationCLI, explorationGUI);
    choices(readerComparator, drawHelperComparator);
    choices(mapCheckerCLI, mapCheckerGUI);
    choices(subsetCLI, subsetGUI);
    // FIXME: Write GUI equivalent of query CLI
    choice(queryCLI);
    choice(echoDriver);
    // FIXME: Write GUI for the duplicate fixture remover
    choice(duplicateFixtureRemoverCLI);
    // FIXME: Write trapping (and hunting, etc.) GUI
    choice(trappingCLI);
    // TODO: AppStarter went here
    // FIXME: Write stat-generating/stat-entering GUI
    choice(statGeneratingCLI);
    // FIXME: Write GUI for map-expanding driver
    choice(expansionDriver);
    // TODO: Write GUI equivalent of Map Populator Driver
    choice(mapPopulatorDriver);
    choices(resourceAddingCLI, resourceAddingGUI);
    choices(tabularReportCLI, tabularReportGUI);
    // TODO: Write GUI to allow user to visually explore a mine
    choice(miningCLI);
    // TODO: Write GUI to allow user to generate or enter town contents
    choice(townGeneratingCLI);
    return cache;
}
"Create the usage message for a particular driver."
String usageMessage(IDriverUsage usage, Boolean verbose) {
    StringBuilder builder = StringBuilder();
    // FIXME: should open with either "ceylon run" or "java -jar /path/to/fat.jar"
    // and this module's name.
    builder.append("Usage: java controller.map.drivers.AppStarter ");
    if (usage.graphical) {
        builder.append("[-g|--gui]");
    } else {
        builder.append("-c|--cli");
    }
    builder.append(" ``usage.shortOption``|``usage.longOption``");
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
void handleDroppedFiles(AppEvent.OpenFilesEvent openFilesEvent) {
    if (exists topWindow = WindowList.getWindows(true, false)
            .iterable.narrow<SPFrame>().last) {
        for (file in openFilesEvent.files) {
            topWindow.acceptDroppedFile(file.toPath());
        }
    }
}
suppressWarnings("expressionTypeNothing")
shared void run() {
    addLogWriter(logWriter);
    System.setProperty("com.apple.mrj.application.apple.menu.about.name",
        "SP Helpers");
    System.setProperty("apple.awt.application.name", "SP Helpers");
    UIManager.setLookAndFeel(UIManager.systemLookAndFeelClassName);
    System.setProperty("apple.laf.useScreenMenuBar", "true");
    SPOptionsImpl options = SPOptionsImpl();
    Map<String, ISPDriver[2]> driverCache = createCache();
    if (platform.systemIsMac) {
        Application.application.setOpenFileHandler(handleDroppedFiles);
    }
    object appStarter satisfies ISPDriver {
        shared actual IDriverUsage usage = DriverUsage(true, "-p", "--app-starter",
            ParamCount.anyNumber, "App Chooser",
            "Let the user choose an app to start, or handle options.");
        shared actual void startDriverOnArguments(ICLIHelper cli, SPOptions options,
                String* args) {
//            log.info("Inside appStarter.startDriver()");
            variable Boolean gui = !GraphicsEnvironment.headless;
            variable SPOptionsImpl currentOptions = SPOptionsImpl(options);
            if (!currentOptions.hasOption("--gui")) {
                currentOptions.addOption("--gui", gui.string);
            }
            MutableList<String> others = ArrayList<String>();
            variable [ISPDriver, ISPDriver]? currentDrivers = null;
            for (arg in args.coalesced) {
                if (arg == "-g" || arg == "--gui") {
                    currentOptions.addOption("--gui");
                    gui = true;
                } else if (arg == "-c" || arg == "--cli") {
                    currentOptions.addOption("--gui", "false");
                    gui = false;
                } else if (arg.startsWith("--gui=")) {
                    String tempString = arg.substring(6);
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
                        if (partial.empty) then element else "``partial``=``element``")
                        else "");
                } else if (driverCache.defines(arg.lowercased)) {
                    if (exists temp = currentDrivers) {
                        SPOptions currentOptionsTyped = currentOptions;
                        if (gui) {
                            // TODO: catch and log a DriverFailedException inside lambda
                            SwingUtilities.invokeLater(() =>
                                temp.rest.first.startDriverOnArguments(cli,
                                    currentOptionsTyped, *others));
                        } else {
                            temp.first.startDriverOnArguments(cli, currentOptionsTyped,
                                *others);
                        }
                    }
                    currentDrivers = driverCache[arg.lowercased];
                } else if (arg.startsWith("-")) {
                    currentOptions.addOption(arg);
                } else {
                    others.add(arg);
                }
            }
            if (options.hasOption("--help")) {
                IDriverUsage tempUsage;
                if (exists drivers = currentDrivers) {
                    if (gui) {
                        tempUsage = drivers.rest.first.usage;
                    } else {
                        tempUsage = drivers.first.usage;
                    }
                } else {
                    tempUsage = usage;
                }
                usageMessage(tempUsage, options.getArgument("--verbose") == "true");
            } else if (exists drivers = currentDrivers) {
                SPOptions currentOptionsTyped = currentOptions;
                if (gui) {
                    // TODO: catch and log a DriverFailedException inside the lambda
                    SwingUtilities.invokeLater(() {
                        try {
                            drivers.rest.first.startDriverOnArguments(cli,
                                currentOptionsTyped, *others);
                        } catch (IncorrectUsageException except) {
                            cli.println(usageMessage(except.correctUsage,
                                options.getArgument("--verbose") == "true"));
                        } catch (DriverFailedException except) {
                            if (is SPFormatException cause = except.cause) {
                                log.error(cause.message);
                            } else if (exists cause = except.cause) {
                                log.error("Driver failed:", cause);
                            } else {
                                log.error("Driver failed:", except);
                            }
                        }
                    });
                } else {
                    drivers.first.startDriverOnArguments(cli, currentOptionsTyped,
                        *others);
                }
            } else {
                SPOptions currentOptionsTyped = currentOptions;
                if (gui) {
                    try {
                        SwingUtilities.invokeLater(() => appChooserFrame(cli,
                            currentOptionsTyped, others).setVisible(true));
                    } catch (DriverFailedException except) {
                        log.fatal(except.message, except);
                        SwingUtilities.invokeLater(() => showErrorDialog(null,
                            "Strategic Primer Assistive Programs", except.message));
                    }
                } else {
                    ISPDriver[] driversList = [*driverCache.items.map(Tuple.first).distinct];
                    value choice = cli.chooseFromList(driversList,
                        "CLI apps available:", "No applications available",
                        "App to start: ", true);
                    if (exists chosenDriver = choice.item) {
                        chosenDriver.startDriverOnArguments(cli, options, *others);
                    }
                }
            }
        }

        shared actual void startDriverOnModel(ICLIHelper cli, SPOptions options,
                IDriverModel driverModel) {
            // TODO: what about -c?
            if (GraphicsEnvironment.headless) {
                ISPDriver[] cliDrivers = [*driverCache.items.map(
                            (element) => element.first)];
                try {
                    if (exists driver = cli.chooseFromList(
                            cliDrivers, "CLI apps available:",
                            "No applications available", "App to start: ", true).item) {
                        driver.startDriverOnModel(cli, options, driverModel);
                    }
                } catch (IOException except) {
                    log.error("I/O error prompting user for app to start", except);
                }
            } else {
                SwingUtilities.invokeLater(
                    () => appChooserFrame(cli, options, driverModel).setVisible(true));
            }
        }
    }
    try {
        appStarter.startDriverOnArgumentsNoCLI(options, *process.arguments);
    } catch (IncorrectUsageException except) {
        IDriverUsage usage = except.correctUsage;
        process.writeErrorLine(usageMessage(usage, options.hasOption("--verbose")));
        process.exit(1);
    } catch (IOException|DriverFailedException except) {
        log.error(except.message, except.cause);
        process.exit(2);
    }
}
suppressWarnings("expressionTypeNothing")
SPFrame appChooserFrame(ICLIHelper cli, SPOptions options,
        {String*}|IDriverModel finalArg) {
    object frame extends SPFrame("SP App Chooser", null, Dimension(220, 110)) {
        shared actual String windowName = "SP App Chooser";
        shared actual Boolean supportsDroppedFiles = false;
    }
    JButton button(String desc, ISPDriver() target) {
        object retval extends JButton(desc) satisfies ActionListener&Runnable {
            shared actual void actionPerformed(ActionEvent event) {
                if (is IDriverModel finalArg) {
                    target().startDriverOnModel(cli, options, finalArg);
                } else {
                    target().startDriverOnArguments(cli, options, *finalArg);
                }
                SwingUtilities.invokeLater(this);
            }
            shared actual void run() {
                frame.setVisible(false);
                frame.dispose();
            }
        }
        retval.addActionListener(retval);
        return retval;
    }
    JPanel buttonPanel = JPanel(GridLayout(0, 1));
    buttonPanel.add(button("Map Viewer", () => viewerGUI));
    buttonPanel.add(button("Worker Skill Advancement", () => advancementGUI));
    buttonPanel.add(button("Unit Orders and Worker Management", () => workerGUI));
    buttonPanel.add(button("Exploration", () => explorationGUI));
    frame.contentPane = BorderedPanel.verticalPanel(
        JLabel("Please choose one of the applications below"),
        JScrollPane(buttonPanel), null);
    frame.pack();
    MenuBroker menuHandler = MenuBroker();
    menuHandler.register((event) => frame.dispose(), "close");
    menuHandler.register((event) => aboutDialog(frame, frame.windowName).setVisible(true), "about");
    menuHandler.register((event) => process.exit(0), "quit");
    frame.jMenuBar = UtilityMenu(frame);
    return frame;
}
