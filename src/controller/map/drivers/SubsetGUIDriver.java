package controller.map.drivers;

import java.io.IOException;
import java.util.logging.Level;

import javax.swing.SwingUtilities;
import javax.xml.stream.XMLStreamException;

import util.TypesafeLogger;
import view.map.misc.SubsetFrame;
import controller.map.drivers.ISPDriver.DriverUsage.ParamCount;
import controller.map.formatexceptions.SPFormatException;
import controller.map.misc.WindowThread;

/**
 * A driver to check whether player maps are subsets of the main map and display
 * the results graphically.
 *
 * @author Jonathan Lovelace
 *
 */
public class SubsetGUIDriver implements ISPDriver {
	/**
	 * An object indicating how to use and invoke this driver.
	 */
	private static final DriverUsage USAGE_OBJ = new DriverUsage(true, "-s",
			"--subset", ParamCount.Many, "Check players' maps against master",
			"Check that subordinate maps are subsets of the main map, containing "
					+ "nothing that it does not contain in the same place",
			SubsetGUIDriver.class);

	/**
	 * @param args the files to check
	 */
	// ESCA-JAVA0177:
	public static void main(final String[] args) {
		try {
			new SubsetGUIDriver().startDriver(args);
		} catch (final DriverFailedException except) {
			TypesafeLogger.getLogger(SubsetDriver.class).log(Level.SEVERE,
					except.getMessage(), except.getCause());
		}
	}

	/**
	 * Run the driver.
	 *
	 * @param args command-line arguments
	 * @throws DriverFailedException if the main map fails to load
	 */
	@Override
	public void startDriver(final String... args) throws DriverFailedException {
		if (args.length < 2) {
			throw new DriverFailedException("Need at least two arguments",
					new IllegalArgumentException("Need at least two arguments"));
		}
		final SubsetFrame frame = new SubsetFrame();
		SwingUtilities.invokeLater(new WindowThread(frame));
		final String first = args[0];
		assert first != null;
		try {
			frame.loadMain(first);
		} catch (final IOException except) {
			throw new DriverFailedException("I/O error loading main map "
					+ first, except);
		} catch (final XMLStreamException except) {
			throw new DriverFailedException("XML error reading main map "
					+ first, except);
		} catch (final SPFormatException except) {
			throw new DriverFailedException("Invalid SP XML in main map "
					+ first, except);
		}
		for (final String arg : args) {
			if (arg.equals(first)) {
				continue;
			}
			frame.test(arg);
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
		return USAGE_OBJ.getShortDescription();
	}

	/**
	 * @param nomen ignored
	 */
	@Override
	public void setName(final String nomen) {
		throw new IllegalStateException("Can't rename a driver");
	}
}
