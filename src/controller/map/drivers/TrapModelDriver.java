package controller.map.drivers;

import controller.map.drivers.DriverUsage.ParamCount;
import controller.map.misc.CLIHelper;
import controller.map.misc.ICLIHelper;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.exploration.HuntingModel;
import model.map.HasName;
import model.map.IMapNG;
import model.map.Point;
import model.misc.IDriverModel;
import util.NullCleaner;
import util.TypesafeLogger;

import static model.map.PointFactory.point;

/**
 * A driver to run a player's trapping activity.
 *
 * TODO: Tests
 *
 * This is part of the Strategic Primer assistive programs suite developed by Jonathan
 * Lovelace.
 *
 * Copyright (C) 2013-2015 Jonathan Lovelace
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
public final class TrapModelDriver implements SimpleDriver {
	/**
	 * An object indicating how to use and invoke this driver.
	 */
	private static final DriverUsage USAGE =
			new DriverUsage(false, "-r", "--trap", ParamCount.One,
					               "Run a player's trapping",
					               "Determine the results a player's trapper finds.",
					               TrapModelDriver.class);
	/**
	 * A somewhat lengthy prompt.
	 */
	private static final String FISH_OR_TRAP =
			"Is this a fisherman trapping fish rather than a trapper?";
	/**
	 * Logger.
	 */
	private static final Logger LOGGER = TypesafeLogger
			                                     .getLogger(TrapModelDriver.class);

	/**
	 * The number of minutes in an hour.
	 */
	private static final int MINS_PER_HOUR = 60;

	/**
	 * How many minutes a fruitless check of a fishing trap takes.
	 */
	private static final int FRUITLESS_FISH_TRAP = 5;
	/**
	 * How many minutes a fruitless check of a trap takes.
	 */
	private static final int FRUITLESS_TRAP = 10;

	/**
	 * List of commands.
	 */
	private static final List<TrapperCommand> COMMANDS =
			NullCleaner.assertNotNull(Collections
					                          .unmodifiableList(Arrays.asList(
							                          TrapperCommand.values())));

	/**
	 * The possible commands.
	 */
	private enum TrapperCommand implements HasName {
		/**
		 * Set or reset a trap.
		 */
		Set("Set or reset a trap"),
		/**
		 * Check a trap.
		 */
		Check("Check a trap"),
		/**
		 * Move to the next trap.
		 */
		Move("Move to another trap"),
		/**
		 * Reset a trap that's made for easy resetting.
		 */
		EasyReset("Reset a foothold trap, e.g."),
		/**
		 * Quit.
		 */
		Quit("Quit");
		/**
		 * The "name" of the command.
		 */
		private final String name;

		/**
		 * Constructor.
		 *
		 * @param cName the "name" of the command
		 */
		TrapperCommand(final String cName) {
			name = cName;
		}

		/**
		 * @return the "name" of the command
		 */
		@Override
		public String getName() {
			return name;
		}

		/**
		 * @param nomen ignored
		 */
		@Override
		public void setName(final String nomen) {
			throw new IllegalStateException("Can't rename");
		}
	}

	/**
	 * @param map     the map to explore
	 * @param cli the interface to interact with the user
	 */
	private static void repl(final IMapNG map, final ICLIHelper cli) {
		try {
			final HuntingModel hmodel = new HuntingModel(map);
			final boolean fishing = cli.inputBoolean(FISH_OR_TRAP);
			final String name; // NOPMD
			if (fishing) {
				name = "fisherman";
			} else {
				name = "trapper";
			}
			int minutes = cli.inputNumber("How many hours will the " + name
					                                 + " work? ")
					              * MINS_PER_HOUR;
			final int row = cli.inputNumber("Row of the tile where the "
					                                   + name + " is working: ");
			final int col = cli.inputNumber("Column of that tile: ");
			final Point point = point(row, col);
			final List<String> fixtures; // NOPMD
			if (fishing) {
				fixtures = hmodel.fish(point, minutes);
			} else {
				fixtures = hmodel.hunt(point, minutes);
			}
			int input = -1;
			while ((minutes > 0) && (input < TrapperCommand.values().length)) {
				if (input >= 0) {
					final TrapperCommand command =
							NullCleaner
									.assertNotNull(TrapperCommand.values()[input]);
					minutes -= handleCommand(fixtures, cli,
							command, fishing);
					cli.print(inHours(minutes));
					cli.println(" remaining");
					if (command == TrapperCommand.Quit) {
						break;
					}
				}
				input = cli.chooseFromList(COMMANDS, "What should the "
						                                        + name + " do next?",
						"Oops! No commands",
						"Next action: ", false);
			}
		} catch (final IOException except) {
			LOGGER.log(Level.SEVERE, "I/O exception", except);
		}
	}

	/**
	 * @param minutes a number of minutes
	 * @return a String representation, including the number of hours
	 */
	@SuppressWarnings("TypeMayBeWeakened")
	private static String inHours(final int minutes) {
		if (minutes < MINS_PER_HOUR) {
			return Integer.toString(minutes) + " minutes"; // NOPMD
		} else {
			return Integer.toString(minutes / MINS_PER_HOUR) + " hours, "
					       + Integer.toString(minutes % MINS_PER_HOUR)
					       + " minutes";
		}
	}

	/**
	 * Handle a command.
	 *
	 * @param fixtures the animals generated from the tile and surrounding tiles.
	 * @param cli  the interface to interact with the user
	 * @param command  the command to handle
	 * @param fishing  whether we're dealing with *fish* traps .. which take different
	 *                 amounts of time
	 * @return how many minutes it took to execute the command
	 * @throws IOException on I/O error interacting with user
	 */
	private static int handleCommand(final List<String> fixtures,
			final ICLIHelper cli, final TrapperCommand command,
			final boolean fishing) throws IOException {
		switch (command) {
		case Check: // TODO: extract method?
			final String top = fixtures.remove(0);
			if (HuntingModel.NOTHING.equals(top)) {
				cli.println("Nothing in the trap");
				if (fishing) {
					return FRUITLESS_FISH_TRAP; // NOPMD
				} else {
					return FRUITLESS_TRAP; // NOPMD
				}
			} else {
				cli.print("Found either ");
				cli.print(top);
				cli.println(" or evidence of it escaping.");
				return cli.inputNumber("How long to check and deal with animal? ");
			}
		case EasyReset:
			if (fishing) {
				return 20; // NOPMD
			} else {
				return 5; // NOPMD
			}
		case Move:
			return 2; // NOPMD
		case Quit:
			return 0; // NOPMD
		case Set:
			if (fishing) {
				return 30; // NOPMD
			} else {
				return 45;
			}
		default:
			throw new IllegalArgumentException("Unhandled case");
		}
	}

	/**
	 * Start the driver.
	 *
	 * @param model the driver model to operate on
	 * @throws DriverFailedException on I/O error
	 */
	@Override
	public void startDriver(final IDriverModel model) throws DriverFailedException {
		try (final ICLIHelper cli = new CLIHelper()) {
			repl(model.getMap(), cli);
		} catch (final IOException except) {
			throw new DriverFailedException("I/O error closing CLIHelper", except);
		}
	}

	/**
	 * Run the driver.
	 *
	 * @param args command-line arguments
	 * @throws DriverFailedException if something goes wrong
	 */
	@SuppressWarnings("OverloadedVarargsMethod")
	@Override
	public void startDriver(final String... args) throws DriverFailedException {
		if (args.length == 0) {
			throw new IncorrectUsageException(usage());
		}
		SimpleDriver.super.startDriver(args);
	}

	/**
	 * @return an object indicating how to use and invoke this driver.
	 */
	@Override
	public DriverUsage usage() {
		return USAGE;
	}

	/**
	 * @return a String representation of the object
	 */
	@SuppressWarnings("MethodReturnAlwaysConstant")
	@Override
	public String toString() {
		return "TrapModelDriver";
	}
}
