package controller.map.drivers;

import controller.map.misc.MapReaderAdapter;
import java.io.File;
import model.misc.IDriverModel;
import util.Warning;

/**
 * An interface for drivers which operate on a map model of some kind and want to write
 * it out again to file when they finish.
 *
 * This is part of the Strategic Primer assistive programs suite developed by Jonathan
 * Lovelace.
 *
 * Copyright (C) 2016 Jonathan Lovelace
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
@FunctionalInterface
public interface SimpleCLIDriver extends SimpleDriver {
	/**
	 * Run the driver. If the driver is a GUIDriver, this should use
	 * SwingUtilities.invokeLater(); if it's a CLIDriver, that's not necessary. This
	 * default implementation assumes a CLI driver, and writes the model back to file(s)
	 * after calling startDriver with the model.
	 *
	 * TODO: Check if there are enough arguments, and throw IncorrectUsageException if not
	 *
	 * @param args any command-line arguments that should be passed to the driver.
	 * @throws DriverFailedException if it's impossible for the driver to start.
	 */
	@SuppressWarnings("OverloadedVarargsMethod")
	@Override
	default void startDriver(final String... args) throws DriverFailedException {
		final MapReaderAdapter reader = new MapReaderAdapter();
		final IDriverModel model =
				reader.readMultiMapModel(Warning.DEFAULT, new File(args[0]),
						MapReaderAdapter.namesToFiles(true, args));
		startDriver(model);
		reader.writeModel(model);
	}
}
