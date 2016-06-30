package controller.map.drivers;

import controller.map.misc.IDFactoryFiller;
import controller.map.misc.IDRegistrar;
import java.io.PrintStream;
import model.map.IMapNG;
import model.map.IMutableMapNG;
import model.map.Point;
import model.map.TileType;
import model.map.fixtures.mobile.Animal;
import model.misc.IDriverModel;
import util.SingletonRandom;
import view.util.SystemOut;

/**
 * A driver to add some kind of fixture to suitable tiles throughout the map. Customize
 * isSuitable(), chance(), and create() before each use.
 *
 * This is part of the Strategic Primer assistive programs suite developed by Jonathan
 * Lovelace.
 *
 * Copyright (C) 2015-2015 Jonathan Lovelace
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
public final class MapPopulatorDriver implements SimpleCLIDriver {
	/**
	 * How many tiles we've found suitable so far.
	 */
	@SuppressWarnings("StaticNonFinalField")
	private static int suitableCount = 0;
	/**
	 * How many tiles we've changed so far.
	 */
	@SuppressWarnings("StaticNonFinalField")
	private static int changedCount = 0;

	/**
	 * Whether the given location is suitable for the kind of fixture we're creating.
	 *
	 * @param map      the map
	 * @param location the location being considered
	 * @return whether the location is suitable for whatever fixture is being added
	 */
	@SuppressWarnings("IfStatementWithIdenticalBranches")
	private static boolean isSuitable(final IMapNG map, final Point location) {
		final TileType terrain = map.getBaseTerrain(location);
		// Hares won't appear in mountains, forests, or ocean.
		if (map.isMountainous(location)) {
			return false;
		} else if (map.getForest(location) != null) {
			return false;
		} else if (TileType.Ocean == terrain) {
			return false;
		} else {
			suitableCount++;
			return true;
		}
	}

	/**
	 * @return The probability of adding to any given tile.
	 */
	@SuppressWarnings({"MethodReturnAlwaysConstant", "SameReturnValue"})
	private static double chance() {
		return 0.05;
	}

	/**
	 * Add a fixture of the kind with which we're populating the map to the specified
	 * location.
	 *
	 * @param location the location in question
	 * @param map      the map
	 * @param idf      an ID factory to generate the necessary ID #.
	 */
	private static void create(final Point location, final IMutableMapNG map,
							final IDRegistrar idf) {
		changedCount++;
		map.addFixture(location,
				new Animal("hare", false, false, "wild", idf.createID()));
	}

	/**
	 * An object indicating how to use and invoke this driver.
	 */
	private static final DriverUsage USAGE =
			new DriverUsage(false, "-l", "--populate", ParamCount.One,
								"Add missing fixtures to a map",
								"Add specified kinds of fixtures to suitable points " +
										"throughout a map",
								MapPopulatorDriver.class);

	/**
	 * Run the driver. This form is, at the moment, primarily for use in test code, but
	 * that may change.
	 *
	 * @param model the driver-model that should be used by the app
	 */
	@Override
	public void startDriver(final IDriverModel model) {
		populate(model.getMap());
		// SystemOut.close() delegates to flush() but doesn't close stdout
		try (final PrintStream stdout = SystemOut.SYS_OUT) {
			stdout.printf("%d out of %d suitable locations were changed",
					Integer.valueOf(changedCount), Integer.valueOf(suitableCount));
		}
	}

	/**
	 * Add populations to the map.
	 *
	 * @param map the map
	 */
	private static void populate(final IMutableMapNG map) {
		final IDRegistrar idf = IDFactoryFiller.createFactory(map);
		for (final Point location : map.locations()) {
			if (isSuitable(map, location) &&
						(SingletonRandom.RANDOM.nextDouble() < chance())) {
				create(location, map, idf);
			}
		}
	}

	/**
	 * @return an object indicating how to use and invoke this driver.
	 */
	@Override
	public DriverUsage usage() {
		return USAGE;
	}

	/**
	 * @return a string representation of this class
	 */
	@SuppressWarnings("MethodReturnAlwaysConstant")
	@Override
	public String toString() {
		return "MapPopulatorDriver";
	}
}
