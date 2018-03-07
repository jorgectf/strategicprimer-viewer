import ceylon.file {
    parsePath,
    Nil,
    File,
    Path,
    Writer
}

import java.io {
    IOException,
    IOError
}
import java.nio.file {
    JPath=Path,
    JPaths=Paths
}

import strategicprimer.drivers.common {
    IMultiMapModel,
    IDriverModel,
    ParamCount,
    SimpleDriver,
    IDriverUsage,
    SPOptions,
    DriverUsage,
    DriverFailedException
}
import strategicprimer.drivers.common.cli {
    ICLIHelper
}
import strategicprimer.model.map {
    IMapNG,
    Player,
    PlayerImpl
}
import strategicprimer.report {
    reportGenerator,
    tabularReportGenerator
}
import javax.swing {
    JTabbedPane
}
import java.awt {
    Dimension,
    Component
}
import strategicprimer.drivers.gui.common.about {
    aboutDialog
}
import strategicprimer.drivers.gui.common {
    SPFrame,
    UtilityMenu
}
import ceylon.collection {
    HashMap,
    MutableMap,
	MutableSet,
	HashSet
}
import ceylon.io {
	SocketAddress
}
import ceylon.http.server {
	newServer,
	Endpoint,
	Request,
	Response,
	startsWith,
	isRoot
}
import ceylon.html {
	renderTemplate,
	Html,
	Head,
	Title,
	Body,
	H1,
	Ul,
	Li,
	A
}
"A driver to produce a report of the contents of a map."
object reportCLI satisfies SimpleDriver {
    shared actual IDriverUsage usage = DriverUsage {
        graphical = false;
        invocations = ["-m", "--report"];
        paramsWanted = ParamCount.one;
        shortDescription = "Report Generator";
        longDescription = "Produce HTML report of the contents of a map";
        supportedOptionsTemp = [
            ("\\" == operatingSystem.fileSeparator) then
                "--out=C:\\path\\to\\output.html"
                else "--out=/path/to/output.html",
            "--player=NN", "--current-turn=NN", "--serve[=8080]"
        ];
    };
    String suffix(JPath file, Integer count) {
        Integer start;
        if (count >= file.nameCount) {
            start = 0;
        } else {
            start = file.nameCount - count;
        }
        Integer end;
        if (file.nameCount == 0) {
            end = 1;
        } else {
            end = file.nameCount;
        }
        return file.subpath(start, end).string;
    }
    String shortestSuffix({JPath*} all, JPath file) {
        Integer longestPath = Integer.max(all.map(JPath.nameCount)) else 1;
        MutableSet<String> localCache = HashSet<String>();
        for (num in 1..longestPath) {
            for (key in all) {
                String item = suffix(key, num);
                if (localCache.contains(item)) {
                    break;
                } else {
                    localCache.add(item);
                }
            } else {
                return suffix(file, num);
            }
        }
        return file.string;
    }
    void serveReports(IDriverModel model, Integer port, Player? currentPlayer) {
        MutableMap<JPath, String> cache = HashMap<JPath, String>();
        if (is IMultiMapModel model) {
            for ([map, file] in model.allMaps) {
                if (exists file, !cache.defines(file)) {
                    cache[file] = reportGenerator.createReport(map, currentPlayer else map.currentPlayer);
                }
            }
        } else if (exists file = model.mapFile) {
            cache[file] = reportGenerator.createReport(model.map, currentPlayer else model.map.currentPlayer);
        }
        if (cache.empty) {
            return;
        } else {
            value localCache = cache.map((file->report) => shortestSuffix(cache.keys, file.toAbsolutePath())->report);
            {Endpoint*} endpoints = localCache.map((file->report) =>
                Endpoint {
	                path = startsWith("/``file``");
	                service(Request request, Response response) => response.writeString(report);
	            });
            Endpoint rootHandler = Endpoint {
                path = isRoot();
                void service(Request request, Response response) {
	                	renderTemplate(Html {
	                    Head {
	                        Title {
	                            "Strategic Primer Reports";
	                        }
	                    },
	                    Body {
	                        H1 {
	                            "Strategic Primer Reports"
	                        },
	                        Ul {
	                            localCache.map((file->report) => Li {
	                                A { href="/``file``"; children = [file]; }
		                        })
	                        }
	                    }
	                }, response.writeString);
                }
            };
            log.info("About to start serving on port ``port``");
            newServer {
                rootHandler, *endpoints
            }.start(SocketAddress("127.0.0.1", port));
        }
    }
    shared actual void startDriverOnModel(ICLIHelper cli, SPOptions options,
            IDriverModel model) {
        void writeReport(JPath? filename, IMapNG map) { // TODO: move to top level of object
            if (exists filename) {
                Player player;
                if (options.hasOption("--player")) {
                    value playerNum = Integer.parse(options.getArgument("--player"));
                    if (is Integer playerNum) {
                            if (exists temp = map.players
                                    .find((item) => item.playerId == playerNum)) {
                                player = temp;
                            } else {
                                log.warn("No player with that number");
                                player = PlayerImpl(playerNum, "");
                            }
                    } else {
                        log.warn("Non-numeric player", playerNum);
                        player = map.currentPlayer;
                    }
                } else {
                    player = map.currentPlayer;
                }
                String outString;
                JPath outPath;
                if (options.hasOption("--out")) {
                    outString = options.getArgument("--out");
                    outPath = JPaths.get(outString);
                } else {
                    outString = "``filename.fileName``.report.html";
                    outPath = filename.resolveSibling(outString);
                }
                value outPathCeylon = parsePath(outPath.toAbsolutePath().string);
                if (is Nil loc = outPathCeylon.resource) {
                    value file = loc.createFile();
                    try (writer = file.Overwriter()) {
                        writer.write(reportGenerator.createReport(map, player));
                    }
                }
            } else {
                log.error("Asked to make report from map with no filename");
            }
        }
        if (options.hasOption("--serve")) {
            value tempPort = Integer.parse(options.getArgument("--serve"));
            Integer port;
            if (is Integer tempPort) {
                port = tempPort;
            } else {
                port = 8080;
            }
            value playerNum = Integer.parse(options.getArgument("--player"));
            Player? player;
            if (is Integer playerNum) {
                player = model.map.players.find((player) => player.playerId == playerNum);
            } else {
                player = null;
            }
            serveReports(model, port, player);
        } else {
	        if (is IMultiMapModel model) {
	            for ([map, file] in model.allMaps) {
	                writeReport(file, map);
	            }
	        } else {
	            writeReport(model.mapFile, model.map);
	        }
	    }
    }
    "As we're a CLI driver, we can't show a file-chooser dialog."
    shared actual {JPath*} askUserForFiles() => {};
}
"A driver to show tabular reports of the contents of a player's map in a GUI."
object tabularReportGUI satisfies SimpleDriver {
    shared actual IDriverUsage usage = DriverUsage(true, ["-b", "--tabular"],
        ParamCount.one, "Tabular Report Viewer",
        "Show the contents of a map in tabular form");
    suppressWarnings("expressionTypeNothing")
    shared actual void startDriverOnModel(ICLIHelper cli, SPOptions options,
            IDriverModel model) {
        SPFrame window = SPFrame("Tabular Report", model.mapFile, Dimension(640, 480));
        JTabbedPane frame = JTabbedPane(JTabbedPane.top, JTabbedPane.scrollTabLayout);
        tabularReportGenerator.createGUITabularReports(
            (String str, Component comp) => frame.addTab(str, comp), model.map);
        window.add(frame);
        MenuBroker menuHandler = MenuBroker();
        menuHandler.register((event) => window.dispose(), "close");
        menuHandler.register((event) => aboutDialog(frame, window.windowName).setVisible(true), "about");
        menuHandler.register((event) => process.exit(0), "quit");
        window.jMenuBar = UtilityMenu(window);
        window.setVisible(true);
    }
    "Ask the user to choose a file."
    shared actual {JPath*} askUserForFiles() {
        try {
            return FileChooser.open(null).files;
        } catch (FileChooser.ChoiceInterruptedException except) {
            throw DriverFailedException(except,
                "Choice interrupted or user didn't choose");
        }
    }
}
"A driver to produce tabular (CSV) reports of the contents of a player's map."
object tabularReportCLI satisfies SimpleDriver {
    shared actual IDriverUsage usage = DriverUsage(false, ["-b", "--tabular"],
        ParamCount.atLeastOne, "Tabular Report Generator",
        "Produce CSV reports of the contents of a map.");
    MutableMap<String,Writer> writers = HashMap<String,Writer>();
    shared actual void startDriverOnModel(ICLIHelper cli, SPOptions options,
            IDriverModel model) {
        Anything(String)(String) filenameFunction(Path base) {
            assert (exists baseName = base.elements.terminal(1).first);
            Anything(String) retval(String tableName) {
                if (exists writer = writers.get("``baseName``.``tableName``.csv")) {
                    return writer.write;
                } else {
                    File file;
                    switch (temp = base.siblingPath("``baseName``.``tableName``.csv").resource)
                    case (is File) {
                        file = temp;
                    }
                    case (is Nil) {
                        file = temp.createFile();
                    }
                    else {
                        throw IOException("``base``.``tableName``.csv exists but is not a file");
                    }
                    value writer = file.Overwriter();
                    writers["``baseName``.``tableName``.csv"] = writer;
                    return writer.write;
                }
            }
            return retval;
        }
        void createReports(IMapNG map, JPath? mapFile) {
            if (exists mapFile) {
                try {
                    tabularReportGenerator.createTabularReports(map,
                        filenameFunction(parsePath(mapFile.string)));
                } catch (IOException|IOError except) {
                    throw DriverFailedException(except);
                }
            } else {
                log.error("Asked to create reports from map with no filename");
            }
        }
        if (is IMultiMapModel model) {
            for ([map, file] in model.allMaps) {
                createReports(map, file);
            }
        } else {
            createReports(model.map, model.mapFile);
        }
        for (writer in writers.items) {
            writer.close();
        }
    }
    "Since this is a CLI driver, we can't show a file-chooser dialog."
    shared actual {JPath*} askUserForFiles() => {};
}
