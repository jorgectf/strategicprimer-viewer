import java.awt {
    Image,
    Graphics
}
import java.awt.image {
    BufferedImage
}
import strategicprimer.viewer.drivers.map_viewer {
    Ver2TileDrawHelper
}
import strategicprimer.drivers.common {
    SPOptions,
    IDriverUsage,
    DriverUsage,
    ParamCount,
    UtilityDriver,
    IncorrectUsageException,
    FixtureMatcher,
    DriverFactory,
    UtilityDriverFactory
}
import strategicprimer.drivers.common.cli {
    ICLIHelper
}
import strategicprimer.model.common.map {
    MapDimensions,
    TileFixture,
    Point,
    IMapNG
}
import strategicprimer.model.impl.xmlio {
    mapIOHelper
}
import strategicprimer.model.common.xmlio {
    warningLevels
}
import ceylon.collection {
    MutableMap,
    HashMap
}
import ceylon.file {
    File,
    parsePath,
    Nil,
    Path
}
import lovelace.util.common {
    PathWrapper,
    Accumulator
}

"A factory for a driver to compare the performance of TileDrawHelpers."
service(`interface DriverFactory`)
shared class DrawHelperComparatorFactory satisfies UtilityDriverFactory {
    shared static IDriverUsage staticUsage = DriverUsage {
        graphical = true;
        invocations = ["-t", "--test"];
        paramsWanted = ParamCount.atLeastOne;
        shortDescription = "Test drawing performance.";
        longDescription =
        """Test the performance of the TileDrawHelper classes---which do the heavy
           lifting of rendering the map in the viewer---using a variety of
           automated tests.""";
        includeInCLIList = true;
        includeInGUIList = false;
        supportedOptions = ["--report=out.csv"];
    };

    shared new () {}

    shared actual IDriverUsage usage => staticUsage;

    shared actual UtilityDriver createDriver(ICLIHelper cli, SPOptions options) =>
            DrawHelperComparator(cli, options);
}

"A driver to compare the performance of TileDrawHelpers."
shared class DrawHelperComparator satisfies UtilityDriver {
    "The first test: Basic drawing."
    static Integer first(TileDrawHelper helper, IMapNG map, Integer reps,
            Integer tileSize) {
        MapDimensions mapDimensions = map.dimensions;
        BufferedImage image = BufferedImage(tileSize * mapDimensions.columns,
            tileSize * mapDimensions.rows, BufferedImage.typeIntRgb);
        Integer start = system.milliseconds;
        Coordinate dimensions = Coordinate(tileSize, tileSize);
        for (rep in 0:reps) {
            image.flush();
            for (point in map.locations) {
                helper.drawTile(image.createGraphics(), map, point,
                    Coordinate(point.row * tileSize, point.column * tileSize),
                    dimensions);
            }
        }
        Integer end = system.milliseconds;
        return end - start;
    }

    "Second test: Basic drawing, reusing Graphics."
    static Integer second(TileDrawHelper helper, IMapNG map, Integer reps,
            Integer tileSize) {
        MapDimensions mapDimensions = map.dimensions;
        BufferedImage image = BufferedImage(tileSize * mapDimensions.columns,
            tileSize * mapDimensions.rows, BufferedImage.typeIntRgb);
        Integer start = system.milliseconds;
        for (rep in 0:reps) {
            image.flush();
            Graphics pen = image.createGraphics();
            Coordinate dimensions = Coordinate(tileSize, tileSize);
            for (point in map.locations) {
                helper.drawTile(pen, map, point, Coordinate(point.row * tileSize,
                    point.column * tileSize), dimensions);
            }
            pen.dispose();
        }
        Integer end = system.milliseconds;
        return end - start;
    }

    static Range<Integer> testRowSpan = 20..40; // TODO: randomize these a bit?
    static Range<Integer> testColSpan = 55..82;

    "Third test, part one: iterating."
    static Integer thirdOne(TileDrawHelper helper, IMapNG map, Integer reps,
            Integer tileSize) {
        MapDimensions mapDimensions = map.dimensions;
        BufferedImage image = BufferedImage(tileSize * mapDimensions.columns,
            tileSize * mapDimensions.rows, BufferedImage.typeIntRgb);
        Integer start = system.milliseconds;
        for (rep in 0:reps) {
            image.flush();
            Graphics pen = image.createGraphics();
            Coordinate dimensions = Coordinate(tileSize, tileSize);
            for (row in testRowSpan) {
                for (col in testColSpan) {
                    helper.drawTile(pen, map, Point(row, col),
                        Coordinate(row * tileSize, col * tileSize),
                        dimensions);
                }
            }
            pen.dispose();
        }
        Integer end = system.milliseconds;
        return end - start;
    }

    static Integer thirdTwo(TileDrawHelper helper, IMapNG map, Integer reps,
            Integer tileSize) {
        MapDimensions mapDimensions = map.dimensions;
        BufferedImage image = BufferedImage(tileSize * mapDimensions.columns,
            tileSize * mapDimensions.rows, BufferedImage.typeIntRgb);
        Integer start = system.milliseconds;
        for (rep in 0:reps) {
            image.flush();
            Graphics pen = image.createGraphics();
            Coordinate dimensions = Coordinate(tileSize, tileSize);
            for (point in map.locations) {
                if (testRowSpan.contains(point.row) &&
                        testColSpan.contains(point.column)) {
                    helper.drawTile(pen, map, point,
                        Coordinate(point.row * tileSize, point.column * tileSize),
                        dimensions);
                }
            }
            pen.dispose();
        }
        Integer end = system.milliseconds;
        return end - start;
    }

    static {[String, Integer(TileDrawHelper, IMapNG, Integer, Integer)]*} tests = [
        ["1. Basic Drawing", first],
        ["2. Basic Drawing, reusing Graphics", second],
        ["3a. Ordered iteration vs filtering: Iteration", thirdOne],
        ["3b. Ordered iteration vs filtering: Filtering", thirdTwo]
    ];

    static Boolean dummyObserver(Image? image, Integer infoflags,
        Integer xCoordinate, Integer yCoordinate, Integer width, Integer height) =>
            false;

    static Boolean dummyFilter(TileFixture? fix) => true;

    static {[TileDrawHelper, String]*} helpers = [
        [Ver2TileDrawHelper(dummyObserver, dummyFilter,
            Singleton(FixtureMatcher(dummyFilter, "test"))), "Ver 2:"]
    ];

    MutableMap<[String, String, String], Accumulator<Integer>> results =
            HashMap<[String, String, String], Accumulator<Integer>>();

    Accumulator<Integer> getResultsAccumulator(String file, String testee, String test) {
        [String, String, String] tuple = [file, testee, test];
        if (exists retval = results[tuple]) {
            return retval;
        } else {
            Accumulator<Integer> retval = Accumulator(0);
            results.put(tuple, retval);
            return retval;
        }
    }

    ICLIHelper cli;
    SPOptions options;
    shared new (ICLIHelper cli, SPOptions options) {
        this.cli = cli;
        this.options = options;
    }

    "Run all the tests on the specified map."
    void runAllTests(IMapNG map, String fileName, Integer repetitions) {
        Integer printStats(String prefix, Integer total, Integer reps) {
            cli.println("``prefix``\t``total``, average of ``total / reps`` ns.");
            return total;
        }
        for ([testDesc, test] in tests) {
            cli.println("``testDesc``:");
            for ([testCase, caseDesc] in helpers) {
                Accumulator<Integer> accumulator = getResultsAccumulator(fileName,
                    caseDesc, testDesc);
                accumulator.add(printStats(caseDesc, test(testCase, map, repetitions,
                    scaleZoom(ViewerModel.defaultZoomLevel, map.dimensions.version)),
                repetitions));
            }
        }
        cli.println("----------------------------------------");
        cli.print("Total:");
        for ([testCase, caseDesc] in helpers) {
            printStats(caseDesc, results
                .filterKeys(shuffle(Tuple<String, String, String[2]>
                    .startsWith)([fileName, caseDesc]))
                .items.map(Accumulator<Integer>.sum).fold(0)(plus), repetitions);
        }
        cli.println("");
    }

    Integer reps = 50;
    "Run the tests."
    shared actual void startDriver(String* args) {
        if (args.size == 0) {
            throw IncorrectUsageException(DrawHelperComparatorFactory.staticUsage);
        }
        MutableMap<String, Integer> mapSizes = HashMap<String, Integer>();
        for (arg in args) {
            Path path = parsePath(arg);
            IMapNG map = mapIOHelper.readMap(PathWrapper(arg), warningLevels.ignore);
            mapSizes[arg] = map.locations.size;
            String filename = path.elements.last else "an unsaved map";
            cli.println("Testing using ``filename``");
            runAllTests(map, filename, reps);
        }
        String reportFilename = options.getArgument("--report");
        if (reportFilename != "false") {
            File outFile;
            switch (resource = parsePath(reportFilename).resource)
            case (is Nil) {
                outFile = resource.createFile();
            }
            case (is File) {
                outFile = resource;
            }
            else {
                cli.println(
                    "Specified file to write to is present but not a regular file");
                return;
            }
            try (writer = outFile.Overwriter()) {
                writer.writeLine(
                    "Filename,# Tile,DrawHelper Tested,Test Case,Repetitions,Time (ns)");
                for ([file, helper, test]->total in results) {
                    writer.writeLine(",".join([file, mapSizes[file] else "", helper, test,
                        reps, total.sum]));
                }
            }
        }
    }
}
