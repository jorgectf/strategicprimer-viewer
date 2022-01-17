package utility.subset;

import lovelace.util.MissingFileException;
import java.util.logging.Level;
import java.nio.file.NoSuchFileException;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import org.jetbrains.annotations.Nullable;
import java.util.regex.Pattern;

import java.awt.Dimension;
import java.io.IOException;

import javax.swing.JScrollPane;

import lovelace.util.StreamingLabel;
import static lovelace.util.StreamingLabel.LabelTextColor;

import common.map.MapDimensionsImpl;
import common.map.PlayerCollection;
import common.map.IMapNG;
import common.map.SPMapNG;
import impl.xmlio.MapIOHelper;
import common.xmlio.Warning;
import common.xmlio.SPFormatException;
import drivers.gui.common.SPFrame;
import lovelace.util.MalformedXMLException;
import drivers.common.ISPDriver;
import java.util.logging.Logger;

/**
 * A window to show the result of running subset tests.
 */
/* package */ class SubsetFrame extends SPFrame {
	private static final Logger LOGGER = Logger.getLogger(SubsetFrame.class.getName());
	public SubsetFrame(ISPDriver driver) {
		super("Subset Tester", driver, new Dimension(640, 320), true);
		this.driver = driver;
		label = new StreamingLabel();
		setContentPane(new JScrollPane(label));
	}

	private final ISPDriver driver; // TODO: Is this needed?

	private final StreamingLabel label;

	private class HtmlWriter {
		private final String filename;
		public HtmlWriter(String filename) {
			this.filename = filename;
		}
		private boolean lineStart = true;
		private final Pattern matcher = Pattern.compile(System.lineSeparator());
		public void write(String string) {
			if (lineStart) {
				label.append("<p style=\"color:black\">");
			}
			label.append(filename);
			label.append(": ");
			label.append(matcher.matcher(string).replaceAll("</p><p style=\"color:black\">"));
			lineStart = false;
		}
	}

	private void printParagraph(String paragraph) {
		printParagraph(paragraph, LabelTextColor.BLACK);
	}

	private void printParagraph(String paragraph, LabelTextColor color) {
		label.append(String.format("<p style=\"color:%s\">%s</p>", color, paragraph));
	}

	private IMapNG mainMap = new SPMapNG(new MapDimensionsImpl(0, 0, 2), new PlayerCollection(), -1);

	public void loadMain(IMapNG arg) {
		mainMap = arg;
		printParagraph("<span style=\"color:green\">OK</span> if strict subset, " +
					"<span style=\"color:yellow\">WARN</span> if apparently not (but " +
					"check by hand), <span style=\"color:red\">FAIL</span> if error " +
					"in reading");
	}

	/**
	 * Test a map against the main map, to see if it's a strict subset of it.
	 */
	public void testMap(IMapNG map, @Nullable Path file) {
		String filename;
		if (file != null) { // TODO: invert?
			filename = file.toString();
		} else {
			LOGGER.warning("Given a map with no filename");
			printParagraph("Given a map with no filename", LabelTextColor.YELLOW);
			filename = "an unnamed file";
		}
		printParagraph(String.format("Testing %s ...", filename));
		if (mainMap.isSubset(map, new HtmlWriter(filename)::write)) {
			printParagraph("OK", LabelTextColor.GREEN);
		} else {
			printParagraph("WARN", LabelTextColor.YELLOW);
		}
	}

	// FIXME: Do the wrapping-to-DriverFailedException operation here to limit "throws" declaration
	public void loadMain(Path arg) throws MissingFileException, NoSuchFileException, 
			FileNotFoundException, MalformedXMLException, SPFormatException, IOException {
		try {
			mainMap = MapIOHelper.readMap(arg, Warning.IGNORE);
		} catch (MissingFileException|NoSuchFileException|FileNotFoundException except) {
			printParagraph(String.format("File %s not found", arg), LabelTextColor.RED);
			throw except;
		} catch (MalformedXMLException except) {
			printParagraph(String.format(
				"ERROR: Malformed XML in %s; see following error message for details", arg),
				LabelTextColor.RED);
			printParagraph(except.getMessage(), LabelTextColor.RED);
			throw except;
		} catch (SPFormatException except) {
			printParagraph(String.format(
					"ERROR: SP map format error at line %d in file %s; see following error message for details",
					except.getLine(), arg),
				LabelTextColor.RED);
			printParagraph(except.getMessage(), LabelTextColor.RED);
			throw except;
		} catch (IOException except) {
			printParagraph("ERROR: I/O error reading file " + arg, LabelTextColor.RED);
			throw except;
		}
		printParagraph("<span style=\"color:green\">OK</span> if strict subset, " +
					"<span style=\"color:yellow\">WARN</span> if apparently not (but " +
					"check by hand), <span style=\"color:red\">FAIL</span> if error " +
					"in reading");
	}

	/**
	 * Read a map from file and test it against the main map to see if it's
	 * a strict subset. This method "eats" (but logs) all (anticipated)
	 * errors in reading the file.
	 */
	public void testFile(Path path) {
		printParagraph(String.format("Testing %s ...", path));
		IMapNG map;
		try {
			map = MapIOHelper.readMap(path, Warning.IGNORE);
		} catch (MissingFileException|NoSuchFileException|FileNotFoundException except) {
			printParagraph("FAIL: File not found", LabelTextColor.RED);
			LOGGER.log(Level.SEVERE, path + " not found", except);
			return;
		} catch (IOException except) {
			printParagraph("FAIL: I/O error reading file", LabelTextColor.RED);
			LOGGER.log(Level.SEVERE, "I/O error reading " + path, except);
			return;
		} catch (MalformedXMLException except) {
			printParagraph("FAIL: Malformed XML; see following error message for details",
				LabelTextColor.RED);
			printParagraph(except.getMessage(), LabelTextColor.RED);
			LOGGER.log(Level.SEVERE, "Malformed XML in file " + path, except);
			return;
		} catch (SPFormatException except) {
			printParagraph(String.format(
					"FAIL: SP map format error at line %d; see following error message for details",
					except.getLine()),
				LabelTextColor.RED);
			printParagraph(except.getMessage(), LabelTextColor.RED);
			LOGGER.log(Level.SEVERE, "SP map format error reading " + path, except);
			return;
		}
		testMap(map, path);
	}

	@Override
	public void acceptDroppedFile(Path file) {
		testFile(file);
	}
}
