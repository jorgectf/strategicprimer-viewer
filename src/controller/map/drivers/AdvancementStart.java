package controller.map.drivers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;
import javax.xml.stream.XMLStreamException;

import controller.map.drivers.ISPDriver.DriverUsage.ParamCount;
import controller.map.formatexceptions.SPFormatException;
import controller.map.misc.FileChooser;
import controller.map.misc.FileChooser.ChoiceInterruptedException;
import controller.map.misc.MapReaderAdapter;
import controller.map.misc.MultiIOHandler;
import controller.map.misc.WindowThread;
import model.workermgmt.IWorkerModel;
import model.workermgmt.WorkerModel;
import util.NullCleaner;
import util.TypesafeLogger;
import util.Warning;
import util.Warning.Action;
import view.map.main.MapFileFilter;
import view.map.main.ViewerFrame;
import view.util.FilteredFileChooser;
import view.worker.AdvancementFrame;

/**
 * A class to start the worker management GUI. TODO: Merge with ViewerStart.
 *
 * @author Jonathan Lovelace
 *
 */
public final class AdvancementStart implements ISPDriver {
	/**
	 * An object indicating how to use and invoke this driver.
	 */
	private static final DriverUsage USAGE_OBJ = new DriverUsage(true, "-a",
			"--adv", ParamCount.One,
			"View a player's workers and manage their advancement",
			"View a player's units, the workers in those units, each worker's Jobs, "
					+ "and his or her level in each Skill in each Job.",
			AdvancementStart.class);

	/**
	 * An error message refactored from at least four uses.
	 */
	private static final String XML_ERROR_STRING = "Error reading XML file";
	/**
	 * Logger.
	 */
	private static final Logger LOGGER = TypesafeLogger.getLogger(ViewerFrame.class);
	/**
	 * Error message fragment when file not found.
	 */
	private static final String NOT_FOUND_ERROR = " not found";
	/**
	 * Error message when the map contains invalid data.
	 */
	private static final String INV_DATA_ERROR = "Map contained invalid data";

	/**
	 * Run the driver.
	 *
	 * @param args Command-line arguments.
	 * @throws DriverFailedException if the driver failed to run.
	 *
	 * @see controller.map.drivers.ISPDriver#startDriver(java.lang.String[])
	 */
	@Override
	public void startDriver(final String... args) throws DriverFailedException {
		try {
			final File file; // NOPMD
			if (args.length == 0) {
				file = new FileChooser(new File("")).getFile();
			} else {
				file =
						new FileChooser(new File(NullCleaner.valueOrDefault(
								args[0], ""))).getFile();
			}
			try {
				final MapReaderAdapter reader = new MapReaderAdapter();
				final Warning warner = new Warning(Action.Warn);
				final IWorkerModel model =
						new WorkerModel(reader.readMap(file,
								warner), file);
				for (String arg : args) {
					if (arg != null && !arg.equals(args[0])) {
						final File newFile = new File(arg);
						model.addSubordinateMap(reader.readMap(newFile, warner), newFile);
					}
				}
				SwingUtilities.invokeLater(new WindowThread(new AdvancementFrame(
						model, new MultiIOHandler(model, new FilteredFileChooser(".",
								new MapFileFilter())))));
			} catch (final XMLStreamException e) {
				throw new DriverFailedException(XML_ERROR_STRING + ' '
						+ file.getPath(), e);
			} catch (final FileNotFoundException e) {
				throw new DriverFailedException("File " + file.getPath()
						+ NOT_FOUND_ERROR, e);
			} catch (final IOException e) {
				throw new DriverFailedException("I/O error reading "
						+ file.getPath(), e);
			} catch (final SPFormatException e) {
				throw new DriverFailedException(INV_DATA_ERROR, e);
			}
		} catch (final ChoiceInterruptedException except) {
			LOGGER.log(
					Level.INFO,
					"Choice was interrupted or user declined to choose; aborting.",
					except);
			return;
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
		return "AdvancementStart";
	}
}
