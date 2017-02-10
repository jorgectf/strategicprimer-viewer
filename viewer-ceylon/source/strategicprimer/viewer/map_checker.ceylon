import controller.map.drivers {
    UtilityDriver,
    ParamCount,
    DriverUsage,
    IDriverUsage,
    SPOptions
}
import controller.map.misc {
    ICLIHelper,
    MapReaderAdapter
}

import java.awt {
    Dimension,
    Color
}
import java.nio.file {
    JPaths=Paths,
    JPath=Path,
    NoSuchFileException
}
import java.util {
    Optional
}

import javax.swing {
    JScrollPane
}

import model.map {
    Point,
    IFixture,
    TileType,
    IMapNG,
    FixtureIterable
}
import model.map.fixtures.resources {
    StoneDeposit,
    StoneKind
}

import util {
    Warning
}

import view.util {
    SPFrame,
    StreamingLabel
}
import model.map.fixtures.towns {
    Village
}
import model.map.fixtures.mobile.worker {
    IJob
}
import model.map.fixtures.mobile {
    IWorker
}
import java.io {
    FileNotFoundException,
    IOException
}
import javax.xml.stream {
    XMLStreamException
}
import controller.map.formatexceptions {
    SPFormatException
}
import ceylon.interop.java {
    CeylonIterable
}
"""An interface for checks of a map's *contents* that we don't want the XML-*reading*
   code to do."""
interface Checker {
    shared formal void check(
        "The terrain at the location being checked."
        TileType terrain,
        "The location being checked."
        Point context,
         "The tile fixture being checked."
        IFixture fixture,
        "How warnings should be reported."
        Warning warner);
}
class SPContentWarning(Point context, String message)
        extends Exception("At ``context``: ``message``") { }
object lateriteChecker satisfies Checker {
    shared actual void check(TileType terrain, Point context, IFixture fixture,
            Warning warner) {
        if (is StoneDeposit fixture, StoneKind.laterite ==fixture.stone(),
                !TileType.jungle == terrain) {
            warner.warn(SPContentWarning(context, "Laterite stone in non-jungle"));
        }
    }
}
{String+} landRaces = { "Danan", "dwarf", "elf", "half-elf", "gnome", "human" };
object aquaticVillageChecker satisfies Checker {
    shared actual void check(TileType terrain, Point context, IFixture fixture,
            Warning warner) {
        if (is Village fixture, landRaces.contains(fixture.race), 
                TileType.ocean == terrain) {
            warner.warn(SPContentWarning(context,
                "Aquatic village has non-aquatic race"));
        }
    }
}
Boolean suspiciousSkill(IJob job) {
    if (job.stream().count() > 1) {
        return false;
    } else {
        return {*job}.any(IJob.suspiciousSkills.contains);
    }
}
object suspiciousSkillCheck satisfies Checker {
    shared actual void check(TileType terrain, Point context, IFixture fixture,
            Warning warner) {
        if (is IWorker fixture) {
            if ({*fixture}.any(suspiciousSkill)) {
                warner.warn(SPContentWarning(context,
                    "``fixture.name`` has a Job with one suspiciously-named Skill"));
            }
            for (job in fixture) {
                for (skill in job) {
                    if (skill.name == "miscellaneous", skill.level > 0) {
                        warner.warn(SPContentWarning(context,
                            "``fixture.name`` has a level in 'miscellaneous'"));
                        return;
                    }
                }
            }
        }
    }
}
{Checker+} extraChecks = { lateriteChecker, aquaticVillageChecker, suspiciousSkillCheck };
"A driver to check every map file in a list for errors."
object mapCheckerCLI satisfies UtilityDriver {
    IDriverUsage usageObject = DriverUsage(false, "-k", "--check", ParamCount.atLeastOne,
        "Check map for errors", "Check a map file for errors, deprecated syntax, etc.");
    shared actual IDriverUsage usage() => usageObject;
    void contentCheck(Checker checker, TileType terrain, Point context, Warning warner,
            IFixture* list) {
        for (fixture in list) {
            if (is FixtureIterable<out IFixture> fixture) {
                contentCheck(checker, terrain, context, warner, *CeylonIterable(fixture));
            }
            checker.check(terrain, context, fixture, warner);
        }
    }
    shared void check(JPath file, Anything(String) outStream, Anything(String) err) {
        outStream("Starting ``file``");
// TODO: take Warning instead of using Warning.Custom and assuming callers have set it up
        Warning warner = Warning.custom;
        MapReaderAdapter reader = MapReaderAdapter();
        IMapNG map;
        try {
            map = reader.readMap(file, warner);
        } catch (FileNotFoundException|NoSuchFileException except) {
            err("``file`` not found");
            log.error("``file`` not found", except);
            return;
        } catch (IOException except) {
            err("I/O error reading ``file``");
            log.error("I/O error reading ``file``", except);
            return;
        } catch (XMLStreamException except) {
            err("Malformed XML in ``file``");
            log.error("Malformed XML in ``file``", except);
            return;
        } catch (SPFormatException except) {
            err("SP map format error in ``file``");
            log.error("SP map format error in ``file``", except);
            return;
        }
        for (checker in extraChecks) {
            for (location in map.locations()) {
                TileType terrain = map.getBaseTerrain(location);
                if (exists forest = map.getForest(location)) {
                    contentCheck(checker, terrain, location, warner, forest);
                }
                if (exists ground = map.getGround(location)) {
                    contentCheck(checker, terrain, location, warner, ground);
                }
                contentCheck(checker, terrain, location, warner,
                    *CeylonIterable(map.getOtherFixtures(location)));
            }
        }
    }
    shared actual void startDriver(ICLIHelper cli, SPOptions options, String?* args) {
        String[] filenames = args.coalesced.sequence();
        if (nonempty filenames) {
            for (filename in filenames) {
                check(JPaths.get(filename), cli.println, cli.println);
            }
        }
    }
}
"The map-checker GUI window."
class MapCheckerFrame() extends SPFrame("Strategic Primer Map Checker",
        Optional.empty<JPath>(), Dimension(640, 320)) {
    shared actual String windowName = "Map Checker";
    StreamingLabel label = StreamingLabel();
    void printParagraph(String paragraph,
            StreamingLabel.LabelTextColor color = StreamingLabel.LabelTextColor.white) {
        try (writer = label.writer) {
            writer.println("<p style=\"color:``color``\">``paragraph``</p>");
        }
        label.repaint();
    }
    Warning.custom.setCustomPrinter(Warning.wrapHandler(
        (string) => printParagraph(string, StreamingLabel.LabelTextColor.yellow)));
    setBackground(Color.black);
    contentPane = JScrollPane(label);
    contentPane.background = Color.black;
    shared void check(JPath filename) {
        mapCheckerCLI.check(filename, (text) {
            if (text.startsWith("No errors")) {
                printParagraph(text, StreamingLabel.LabelTextColor.green);
            } else {
                printParagraph(text);
            }
        }, (text) => printParagraph(text, StreamingLabel.LabelTextColor.red));
    }
}
"A driver to check every map file in a list for errors and report the results in a
 window."
object mapCheckerGUI satisfies UtilityDriver {
    IDriverUsage usageObject = DriverUsage(true, "-k", "--check", ParamCount.atLeastOne,
        "Check map for errors", "Check a map file for errors, deprecated syntax, etc.");
    shared actual IDriverUsage usage() => usageObject;
    shared actual void startDriver(ICLIHelper cli, SPOptions options, String?* args) {
        MapCheckerFrame window = MapCheckerFrame();
        window.setVisible(true);
        for (arg in args.coalesced) {
            window.check(JPaths.get(arg));
        }
    }
}