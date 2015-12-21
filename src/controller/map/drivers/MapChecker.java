package controller.map.drivers;

import controller.map.drivers.DriverUsage.ParamCount;
import controller.map.formatexceptions.MapVersionException;
import controller.map.formatexceptions.SPFormatException;
import controller.map.misc.MapReaderAdapter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamException;
import model.misc.IDriverModel;
import util.TypesafeLogger;
import util.Warning;

import static view.util.SystemOut.SYS_OUT;

/**
 * A driver to check every map file in a list for errors.
 *
 * This is part of the Strategic Primer assistive programs suite developed by Jonathan
 * Lovelace.
 *
 * Copyright (C) 2011-2014 Jonathan Lovelace
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of version 3 of the GNU General Public License as published by the Free Software
 * Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this
 * program. If not, see
 * <a href="http://www.gnu.org/licenses/">http://www.gnu.org/licenses/</a>.
 *
 * @author Jonathan Lovelace
 */
public final class MapChecker implements ISPDriver {
	/**
	 * An object indicating how to use and invoke this driver.
	 */
	private static final DriverUsage USAGE_OBJ = new DriverUsage(false, "-k",
			                                                            "--check",
			                                                            ParamCount.One,
			                                                            "Check map for " +
					                                                            "errors",
			                                                            "Check a map " +
					                                                            "file " +
					                                                            "for " +
					                                                            "errors," +
					                                                            " deprecated syntax, etc.",
			                                                            MapChecker
					                                                            .class);

	/**
	 * Logger.
	 */
	private static final Logger LOGGER = TypesafeLogger
			                                     .getLogger(MapChecker.class);
	/**
	 * The map reader we'll use.
	 */
	private final MapReaderAdapter reader = new MapReaderAdapter();

	/**
	 * Run the driver.
	 *
	 * @param args command-line arguments
	 * @throws DriverFailedException if not enough arguments
	 */
	@Override
	public void startDriver(final String... args) throws DriverFailedException {
		if (args.length < 1) {
			throw new DriverFailedException("Need at least one argument",
					                               new IllegalArgumentException("Need at" +
							                                                            " least one argument"));
		}
		for (final String filename : args) {
			if (filename != null) {
				check(new File(filename));
			}
		}
	}

	/**
	 * Check a map.
	 *
	 * @param file the file to check
	 */
	private void check(final File file) {
		SYS_OUT.print("Starting ");
		SYS_OUT.println(file.getPath());
		boolean retval = true;
		try {
			reader.readMap(file, Warning.INSTANCE);
		} catch (final MapVersionException e) {
			LOGGER.log(Level.SEVERE, "Map version in " + file.getPath()
					                         + " not acceptable to reader", e);
			retval = false;
		} catch (final FileNotFoundException e) {
			LOGGER.log(Level.SEVERE, file.getPath() + " not found", e);
			retval = false;
		} catch (final IOException e) {
			LOGGER.log(Level.SEVERE, "I/O error reading " + file.getPath(), e);
			retval = false;
		} catch (final XMLStreamException e) {
			LOGGER.log(Level.SEVERE,
					"XML stream error reading " + file.getPath(), e);
			retval = false;
		} catch (final SPFormatException e) {
			LOGGER.log(Level.SEVERE,
					"SP map format error reading " + file.getPath(), e);
			retval = false;
		}
		if (retval) {
			SYS_OUT.print("No errors in ");
			SYS_OUT.println(file.getPath());
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

	/**
	 * @return a String representation of the object
	 */
	@Override
	public String toString() {
		return "MapChecker";
	}

	/**
	 * @param model ignored
	 * @throws DriverFailedException always; this driver has finished once maps are fully
	 *                               read.
	 */
	@Override
	public void startDriver(final IDriverModel model) throws DriverFailedException {
		throw new DriverFailedException(new IllegalStateException("MapChecker can't operate on a driver model"));
	}
}
