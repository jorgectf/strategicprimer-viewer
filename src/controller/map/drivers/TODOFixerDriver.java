package controller.map.drivers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.xml.stream.XMLStreamException;

import model.map.IMutableMapNG;
import model.map.Point;
import model.map.TileFixture;
import model.map.fixtures.mobile.Unit;
import util.Warning;
import controller.map.formatexceptions.SPFormatException;
import controller.map.misc.CLIHelper;
import controller.map.misc.MapReaderAdapter;

/**
 * A hackish class to help fix TODOs (missing content) in the map.
 * @author Jonathan Lovelace
 *
 */
public class TODOFixerDriver {
	/**
	 * The map we operate on.
	 */
	private final IMutableMapNG map;
	/**
	 * A helper to get strings from the user.
	 */
	private final CLIHelper helper = new CLIHelper();
	/**
	 * @param operand the map we operate on
	 */
	public TODOFixerDriver(final IMutableMapNG operand) {
		map = operand;
	}
	/**
	 * Search for and fix units with kinds missing.
	 */
	public void fixUnits() {
		for (final Point point : map.locations()) {
			if (point == null) {
				continue;
			}
			final List<String> jobList = getList(point);
			for (final TileFixture fix : map.getOtherFixtures(point)) {
				if (fix instanceof Unit && "TODO".equals(((Unit) fix).getKind())) {
					fixUnit((Unit) fix, jobList);
				}
			}
		}
	}
	/**
	 * How many units we've fixed.
	 */
	private int count = -1;
	/**
	 * Fix a stubbed-out kind for a unit.
	 * @param unit the unit to fix
	 * @param jobList a list of possible kinds for its surroundings
	 */
	private void fixUnit(final Unit unit, final List<String> jobList) {
		final Random random = new Random(unit.getID());
		count++;
		for (final String string : jobList) {
			if (string == null) {
				continue;
			}
			if (random.nextBoolean()) {
				System.out.print("Setting unit with ID #");
				System.out.print(unit.getID());
				System.out.print(" (");
				System.out.print(count);
				System.out.print(" / 5328) to kind ");
				System.out.println(string);
				unit.setKind(string);
				return;
			}
		}
		final String desc;
		if (jobList == plainsList) {
			desc = "plains, desert, or mountains";
		} else if (jobList == forestList) {
			desc = "forest or jungle";
		} else if (jobList == oceanList) {
			desc = "ocean";
		} else {
			desc = "other";
		}
		try {
			final String string =
					helper.inputString("What's the next possible kind for "
							+ desc + "? ");
			unit.setKind(string);
			jobList.add(string);
		} catch (IOException e) {
			return;
		}
	}
	/**
	 * A list of unit kinds (jobs) for plains etc.
	 */
	private final List<String> plainsList = new ArrayList<>();
	/**
	 * A list of unit kinds (jobs) for forest and jungle.
	 */
	private final List<String> forestList = new ArrayList<>();
	/**
	 * A list of unit kinds (jobs) for ocean.
	 */
	private final List<String> oceanList = new ArrayList<>();
	/**
	 * @param location a location in the map
	 * @return a List of unit kinds (jobs) for the terrain there
	 */
	@SuppressWarnings("deprecation")
	private List<String> getList(final Point location) {
		switch (map.getBaseTerrain(location)) {
		case BorealForest:
			return forestList;
		case Desert:
			return plainsList;
		case Jungle:
			return forestList;
		case Mountain:
			return plainsList;
		case NotVisible:
			return plainsList; // Should never happen, but ...
		case Ocean:
			return oceanList;
		case Plains:
			if (map.isMountainous(location)) {
				return plainsList;
			} else if (map.getForest(location) != null) {
				return forestList;
			} else {
				return plainsList;
			}
		case Steppe:
			return forestList;
		case TemperateForest:
			return forestList;
		case Tundra:
			return plainsList;
		default:
			return plainsList; // Should never get here, but ...
		}
	}
	/**
	 * Run the driver.
	 * @param args maps to run on
	 */
	public static void main(final String... args) {
		final MapReaderAdapter reader = new MapReaderAdapter();
		for (String arg : args) {
			if (arg == null) {
				continue;
			}
			final IMutableMapNG map;
			final File file = new File(arg);
			try {
				map = reader.readMap(file, Warning.INSTANCE);
			} catch (IOException | XMLStreamException | SPFormatException e) {
				e.printStackTrace();
				continue;
			}
			final TODOFixerDriver driver = new TODOFixerDriver(map);
			driver.fixUnits();
			try {
				reader.write(file, map);
			} catch (IOException e) {
				e.printStackTrace();
				continue;
			}
		}
	}
}
