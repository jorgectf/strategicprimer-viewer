package controller.map.drivers;

import static view.util.SystemOut.SYS_OUT;

import java.io.File;
import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import model.exploration.ExplorationModel;
import model.exploration.IExplorationModel;
import model.map.IMap;
import model.map.MapView;
import model.map.Player;
import model.map.fixtures.mobile.IUnit;
import util.Pair;
import util.Warning;
import view.exploration.ExplorationCLI;
import controller.map.drivers.ISPDriver.DriverUsage.ParamCount;
import controller.map.formatexceptions.SPFormatException;
import controller.map.misc.CLIHelper;
import controller.map.misc.MapReaderAdapter;

/**
 * A CLI to help running exploration.
 *
 * @author Jonathan Lovelace
 *
 */
public class ExplorationCLIDriver implements ISPDriver {
	/**
	 * An object indicating how to use and invoke this driver.
	 */
	private static final DriverUsage USAGE_OBJ = new DriverUsage(false, "-x",
			"--explore", ParamCount.Many, "Run exploration.",
			"Move a unit around the map, "
					+ "updating the player's map with what it sees.",
			ExplorationCLIDriver.class);

	/**
	 * Driver. Takes as its parameters the map files to use.
	 *
	 * @param args the command-line arguments
	 */
	public static void main(final String[] args) {
		try {
			new ExplorationCLIDriver().startDriver(args);
		} catch (final DriverFailedException except) {
			System.err.print(except.getMessage());
			System.err.println(':');
			System.err.println(except.getCause().getLocalizedMessage());
		}
	}

	/**
	 * Read maps.
	 *
	 * @param filenames the files to read from
	 * @return an exploration-model containing all of them
	 * @throws SPFormatException on SP format problems
	 * @throws XMLStreamException on malformed XML
	 * @throws IOException on basic file I/O error
	 */
	private static ExplorationModel readMaps(final String[] filenames)
			throws IOException, XMLStreamException, SPFormatException {
		final MapReaderAdapter reader = new MapReaderAdapter();
		final File firstFile = new File(filenames[0]);
		final MapView master = reader.readMap(firstFile, Warning.INSTANCE);
		final ExplorationModel model = new ExplorationModel(master,
				firstFile);
		for (final String filename : filenames) {
			if (filename == null || filename.equals(filenames[0])) {
				continue;
			}
			final File file = new File(filename);
			final IMap map = reader.readMap(file, Warning.INSTANCE);
			if (!map.getDimensions().equals(master.getDimensions())) {
				throw new IllegalArgumentException("Size mismatch between "
						+ filenames[0] + " and " + filename);
			}
			model.addSubordinateMap(map, file);
		}
		return model;
	}

	/**
	 * Run the driver.
	 *
	 * @param args the command-line arguments
	 * @throws DriverFailedException on error.
	 */
	@Override
	public void startDriver(final String... args) throws DriverFailedException {
		if (args.length == 0) {
			SYS_OUT.print("Usage: ");
			SYS_OUT.print(getClass().getSimpleName());
			SYS_OUT.println(" master-map [player-map ...]");
			System.exit(1);
		}
		// ESCA-JAVA0177:
		final ExplorationModel model; // NOPMD
		try {
			model = readMaps(args);
		} catch (final IOException except) {
			throw new DriverFailedException("I/O error reading maps", except);
		} catch (final XMLStreamException except) {
			throw new DriverFailedException("Malformed XML in map file", except);
		} catch (final SPFormatException except) {
			throw new DriverFailedException("SP format error in map file",
					except);
		}
		final ExplorationCLI cli = new ExplorationCLI(model, new CLIHelper());
		try {
			final Player player = cli.choosePlayer();
			if (player.getPlayerId() < 0) {
				return; // NOPMD
			}
			final IUnit unit = cli.chooseUnit(player);
			if (unit.getID() < 0) {
				return; // NOPMD
			}
			model.selectUnit(unit);
			cli.moveUntilDone();
		} catch (final IOException except) {
			throw new DriverFailedException("I/O error interacting with user",
					except);
		}
		try {
			writeMaps(model);
		} catch (final IOException except) {
			throw new DriverFailedException("I/O error writing to a map file",
					except);
		}
	}

	/**
	 * Write maps to disk.
	 *
	 * @param model the model containing all the maps
	 * @throws IOException on I/O error
	 */
	private static void writeMaps(final IExplorationModel model)
			throws IOException {
		final MapReaderAdapter reader = new MapReaderAdapter();
		for (final Pair<IMap, File> pair : model.getAllMaps()) {
			reader.write(pair.second(), pair.first());
		}
	}

	/**
	 * @return an object indicating how to use and invoke this driver.
	 */
	@Override
	public DriverUsage usage() {
		return USAGE_OBJ;
	}

	/**
	 * @return what to call the driver in a CLI list.
	 */
	@Override
	public String getName() {
		return usage().getShortDescription();
	}

	/**
	 * @param nomen ignored
	 */
	@Override
	public void setName(final String nomen) {
		throw new IllegalStateException("Can't rename a driver");
	}
	/**
	 * @return a String representation of the object
	 */
	@Override
	public String toString() {
		return "ExplorationCLIDriver";
	}
}
