package controller.map.drivers;

import controller.map.misc.ICLIHelper;
import controller.map.report.ReportGenerator;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.logging.Logger;
import model.map.IMapNG;
import model.map.IMutableMapNG;
import model.misc.IDriverModel;
import model.misc.IMultiMapModel;
import util.Pair;
import util.TypesafeLogger;

/**
 * A driver to produce a report of the units in a map.
 *
 * This is part of the Strategic Primer assistive programs suite developed by Jonathan
 * Lovelace.
 *
 * Copyright (C) 2013-2016 Jonathan Lovelace
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of version 3 of the GNU General Public License as published by the Free Software
 * Foundation; see COPYING or
 * <a href="http://www.gnu.org/licenses/">http://www.gnu.org/licenses/</a>.
 *
 * @author Jonathan Lovelace
 */
public final class WorkerReportDriver implements SimpleDriver {
	/**
	 * An object indicating how to use and invoke this driver.
	 */
	private static final DriverUsage USAGE =
			new DriverUsage(false, "-m", "--map", ParamCount.One,
								   "Report Generator",
								   "Produce HTML report of the contents of a map");
	/**
	 * Logger.
	 */
	private static final Logger LOGGER =
			TypesafeLogger.getLogger(WorkerReportDriver.class);

	static {
		USAGE.addSupportedOption("--current-turn=NN");
		//noinspection HardcodedFileSeparator
		if (File.separatorChar == '\\') {
			USAGE.addSupportedOption(
					String.format("--out=C:%spath%sto%soutput.html", File.separator,
							File.separator, File.separator));
		} else {
			USAGE.addSupportedOption(
					String.format("--out=%spath%sto%soutput.html", File.separator,
							File.separator, File.separator));
		}
	}

	/**
	 * Extracted method operating on exactly one filename and map.
	 *
	 * @param maybeFilename an Optional containing the filename the map was loaded from
	 * @param options       the options that were passed to the driver
	 * @param map           the map to generate the report from
	 * @throws DriverFailedException if writing to file fails for some reason
	 */
	private static void writeReport(final Optional<Path> maybeFilename,
									final SPOptions options, final IMapNG map)
			throws DriverFailedException {
		if (maybeFilename.isPresent()) {
			final Path filename = maybeFilename.get();
			final String report = ReportGenerator.createReport(map);
			final String outString;
			final Path out;
			if (options.hasOption("--out")) {
				outString = options.getArgument("--out");
				out = Paths.get(outString);
			} else {
				outString = filename.getFileName() + ".report.html";
				out = filename.resolveSibling(outString);
			}
			try (final BufferedWriter writer = Files.newBufferedWriter(out)) {
				writer.write(report);
			} catch (final IOException except) {
				//noinspection HardcodedFileSeparator
				throw new DriverFailedException("I/O error writing report to " +
														outString,
													   except);
			}
		} else {
			LOGGER.severe("Asked to make report from map with no filename");
		}
	}

	/**
	 * Run the driver.
	 *
	 * @param cli the interface for user I/O
	 * @param options command-line options passed in
	 * @param model   ignored
	 * @throws DriverFailedException always: this driver has to write to the filesystem
	 */
	@Override
	public void startDriver(final ICLIHelper cli, final SPOptions options,
							final IDriverModel model)
			throws DriverFailedException {
		if (model instanceof IMultiMapModel) {
			for (final Pair<IMutableMapNG, Optional<Path>> pair :
					((IMultiMapModel) model).getAllMaps()) {
				writeReport(pair.second(), options, pair.first());
			}
		} else {
			writeReport(model.getMapFile(), options, model.getMap());
		}
	}

	/**
	 * @return an object indicating how to use and invoke this driver
	 */
	@Override
	public IDriverUsage usage() {
		return USAGE;
	}

	/**
	 * @return a String representation of the object
	 */
	@SuppressWarnings("MethodReturnAlwaysConstant")
	@Override
	public String toString() {
		return "WorkerReportDriver";
	}
}
