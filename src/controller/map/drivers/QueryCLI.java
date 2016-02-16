package controller.map.drivers;

import controller.map.drivers.DriverUsage.ParamCount;
import controller.map.misc.CLIHelper;
import controller.map.misc.ICLIHelper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import model.exploration.HuntingModel;
import model.map.IMapNG;
import model.map.MapDimensions;
import model.map.Player;
import model.map.Point;
import model.map.PointFactory;
import model.map.TileFixture;
import model.map.fixtures.Ground;
import model.map.fixtures.mobile.IUnit;
import model.map.fixtures.mobile.IWorker;
import model.map.fixtures.terrain.Forest;
import model.map.fixtures.towns.Fortress;
import model.misc.IDriverModel;
import org.eclipse.jdt.annotation.Nullable;
import util.TypesafeLogger;

/**
 * A driver for running exploration results, etc., using the new model.
 *
 * This is part of the Strategic Primer assistive programs suite developed by Jonathan
 * Lovelace.
 *
 * Copyright (C) 2012-2015 Jonathan Lovelace
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
public final class QueryCLI implements SimpleDriver {
	/**
	 * An object indicating how to use and invoke this driver.
	 */
	private static final DriverUsage USAGE =
			new DriverUsage(false, "-m", "--map", ParamCount.One,
					               "Answer questions about a map.",
					               "Look at tiles on a map. Or run hunting, gathering, or fishing.",
					               QueryCLI.class);

	/**
	 * Logger.
	 */
	private static final Logger LOGGER = TypesafeLogger
			                                     .getLogger(QueryCLI.class);

	/**
	 * How many hours we assume a working day is for a hunter or such.
	 */
	private static final int HUNTER_HOURS = 10;
	/**
	 * How many encounters per hour for a hunter or such.
	 */
	private static final int HOURLY_ENCOUNTERS = 4;

	/**
	 * @param model   the driver model containing the map to explore
	 * @param cli the interface to the user
	 */
	private void repl(final IDriverModel model, final ICLIHelper cli) {
		final HuntingModel hmodel = new HuntingModel(model.getMap());
		try {
			String input = cli.inputString("Command: ");
			while (!input.isEmpty() && (input.charAt(0) != 'q')) {
				handleCommand(model, hmodel, cli, input.charAt(0));
				input = cli.inputString("Command: ");
			}
		} catch (final IOException except) {
			LOGGER.log(Level.SEVERE, "I/O exception", except);
		}
	}

	/**
	 * @param model   the driver model
	 * @param hmodel  the hunting model
	 * @param cli the interface to the user
	 * @param input   the command
	 * @throws IOException           on I/O error
	 */
	public void handleCommand(final IDriverModel model, final HuntingModel hmodel,
	                          final ICLIHelper cli, final char input)
			throws IOException {
		switch (input) {
		case '?':
			usage(cli);
			break;
		case 'f':
			fortressInfo(model.getMap(), selectPoint(cli), cli);
			break;
		case 'h':
			hunt(hmodel, selectPoint(cli), true, cli, HUNTER_HOURS
					                                           * HOURLY_ENCOUNTERS);
			break;
		case 'i':
			hunt(hmodel, selectPoint(cli), false, cli, HUNTER_HOURS
					                                            * HOURLY_ENCOUNTERS);
			break;
		case 'g':
			gather(hmodel, selectPoint(cli), cli, HUNTER_HOURS
					                                       * HOURLY_ENCOUNTERS);
			break;
		case 'e':
			herd(cli);
			break;
		case 't':
			new TrapModelDriver().startDriver(model);
			break;
		case 'd':
			distance(model.getMapDimensions(), cli);
			break;
		case 'c':
			count(model.getMap(), CLIHelper.toList(model.getMap().players()), cli);
			break;
		default:
			cli.println("Unknown command.");
			break;
		}
	}

	/**
	 * Count the workers belonging to a player.
	 *
	 * @param map     the map
	 * @param players the list of players in the map
	 * @param cli the interface to the user
	 * @throws IOException on I/O error interacting with user
	 */
	private void count(final IMapNG map, final List<Player> players,
	                   final ICLIHelper cli) throws IOException {
		final int playerNum = cli.chooseFromList(players,
				"Players in the map:", "Map contains no players",
				"Owner of workers to count: ", true);
		if ((playerNum < 0) || (playerNum >= players.size())) {
			return;
		}
		final Player player = players.get(playerNum);
		int count = 0;
		for (final Point loc : map.locations()) {
			for (final TileFixture fix : map.getOtherFixtures(loc)) {
				if ((fix instanceof IUnit)
						    && player.equals(((IUnit) fix).getOwner())) {
					count += StreamSupport.stream(((IUnit) fix).spliterator(), false)
							         .filter(IWorker.class::isInstance).count();
				} else if (fix instanceof Fortress) {
					StreamSupport.stream(((Fortress) fix).spliterator(), false)
							.filter(IUnit.class::isInstance).map(IUnit.class::cast)
							.filter(unit -> player.equals(unit.getOwner()))
							.flatMap(unit -> StreamSupport
									                 .stream(unit.spliterator(), false))
							.filter(IWorker.class::isInstance).count();
				}
			}
		}
		cli.printf("%s has %i workers.%n", player.getName(), count);
	}

	/**
	 * Report the distance between two points.
	 *
	 * TODO: use some sort of pathfinding
	 *
	 * @param dims    the dimensions of the map
	 * @param cli the interface to the user
	 * @throws IOException on I/O error dealing with user input
	 */
	private void distance(final MapDimensions dims, final ICLIHelper cli)
			throws IOException {
		cli.print("Starting point:\t");
		final Point start = selectPoint(cli);
		cli.print("Destination:\t");
		final Point end = selectPoint(cli);
		final int rawXdiff = start.row - end.row;
		final int rawYdiff = start.col - end.col;
		final int xdiff;
		if (rawXdiff < (dims.rows / 2)) {
			xdiff = rawXdiff;
		} else {
			xdiff = dims.rows - rawXdiff;
		}
		final int ydiff;
		if (rawYdiff < (dims.cols / 2)) {
			ydiff = rawYdiff;
		} else {
			ydiff = dims.cols - rawYdiff;
		}
		cli.printf("Distance (as the crow flies, in tiles):\t%i%n",
				Math.round(Math.sqrt((xdiff * xdiff) + (ydiff * ydiff))));
	}

	/**
	 * Run herding. TODO: Move the logic here into the HuntingModel or a similar class.
	 *
	 * @param cli the interface to the user
	 * @throws IOException on I/O error dealing with user input
	 */
	private void herd(final ICLIHelper cli) throws IOException {
		final double rate; // The amount of milk per animal
		final int time; // How long it takes to milk one animal, in minutes.
		final boolean poultry;
		if (cli.inputBoolean("Are these small animals, like sheep?\t")) {
			rate = 1.5;
			time = 15;
			poultry = false;
		} else if (cli.inputBoolean("Are these dairy cattle?\t")) {
			rate = 4;
			time = 20;
			poultry = false;
		} else if (cli.inputBoolean("Are these chickens?\t")) {
			// TODO: Support other poultry
			rate = 0.75;
			time = 12;
			poultry = true;
		} else {
			rate = 3;
			time = 20;
			poultry = false;
		}
		final int count = cli.inputNumber("How many animals?\t");
		if (count == 0) {
			cli.println("With no animals, no cost and no gain.");
			return; // NOPMD
		} else if (count < 0) {
			cli.println("Can't have a negative number of animals.");
			return; // NOPMD
		} else {
			final int herders = cli.inputNumber("How many herders?\t");
			if (herders <= 0) {
				cli.println("Can't herd with no herders.");
				return; // NOPMD
			}
			final int animalsPerHerder = ((count + herders) - 1) / herders;
			if (poultry) {
				cli.printf("Gathering eggs takes %i minutes; cleaning up after them,%n",
						animalsPerHerder * 2);
				cli.printf(
						"which should be done every third turn at least, takes %.1f " +
								"hours.%n",
						Double.valueOf(animalsPerHerder * 0.5));
				cli.printf("This produces %.0f eggs, totaling %.1f oz.%n",
						Double.valueOf(rate * count),
						Double.valueOf(rate * 2.0 * count));
			} else {
				cli.printf("Tending the animals takes %i minutes, or %i minutes with ",
						animalsPerHerder * time, animalsPerHerder * (time - 5));
				cli.println("expert herders, twice daily.");
				cli.println("Gathering them for each milking takes 30 min more.");
				cli.printf("This produces %,.1f gallons, %,.1f lbs, of milk per day.%n",
						Double.valueOf(rate * count),
						Double.valueOf(rate * 8.6 * count));
			}
		}
	}

	/**
	 * Run hunting, fishing, or trapping.
	 *
	 * @param hmodel     the hunting model
	 * @param point      where to hunt or fish
	 * @param land       true if this is hunting, false if fishing
	 * @param cli the interface to the user
	 * @param encounters how many encounters to show
	 * @throws IOException on I/O error writing to stream
	 */
	private static void hunt(final HuntingModel hmodel, final Point point,
	                         final boolean land, final ICLIHelper cli,
	                         final int encounters)
			throws IOException {
		if (land) {
			hmodel.hunt(point, encounters).forEach(cli::println);
		} else {
			hmodel.fish(point, encounters).forEach(cli::println);
		}
	}

	/**
	 * Run food-gathering.
	 *
	 * @param hmodel     the hunting model to get results from
	 * @param point      around where to gather
	 * @param cli the interface to the user
	 * @param encounters how many encounters to show
	 * @throws IOException on I/O error writing to stream
	 */
	private static void gather(final HuntingModel hmodel, final Point point,
	                           final ICLIHelper cli, final int encounters)
			throws IOException {
		hmodel.gather(point, encounters).forEach(cli::println);
	}

	/**
	 * Give the data the player automatically knows about a user-specified tile if he has
	 * a fortress on it.
	 *
	 * @param map      the map
	 * @param location the selected location
	 * @param cli the interface to the user
	 * @throws IOException on I/O error writing to stream
	 */
	private static void fortressInfo(final IMapNG map, final Point location,
	                                 final ICLIHelper cli) throws IOException {
		cli.print("Terrain is ");
		cli.println(map.getBaseTerrain(location).toString());
		final List<TileFixture> fixtures =
				map.streamOtherFixtures(location).collect(Collectors.toList());
		final Collection<Ground> ground = new ArrayList<>();
		@Nullable
		final Ground locGround = map.getGround(location);
		if (locGround != null) {
			ground.add(locGround);
		}
		@Nullable
		final Forest forest = map.getForest(location);
		final Collection<Forest> forests = new ArrayList<>();
		if (forest != null) {
			forests.add(forest);
		}
		for (final TileFixture fix : fixtures) {
			if (fix instanceof Ground) {
				ground.add((Ground) fix);
			} else if (fix instanceof Forest) {
				forests.add((Forest) fix);
			}
		}
		if (!ground.isEmpty()) {
			cli.println("Kind(s) of ground (rock) on the tile:");
			ground.stream().map(Object::toString).forEach(cli::println);
		}
		if (!forests.isEmpty()) {
			cli.println("Kind(s) of forests on the tile:");
			forests.stream().map(Object::toString).forEach(cli::println);
		}
	}

	/**
	 * @param cli the interface to the user
	 * @return the poin the user specifies.
	 * @throws IOException on I/O error.
	 */
	private Point selectPoint(final ICLIHelper cli) throws IOException {
		return PointFactory.point(cli.inputNumber("Row: "),
				cli.inputNumber("Column: "));
	}

	/**
	 * @return a String representation of the object.
	 */
	@SuppressWarnings("MethodReturnAlwaysConstant")
	@Override
	public String toString() {
		return "QueryCLI";
	}

	/**
	 * Prints a usage message.
	 *
	 * @param cli the interface to the user
	 * @throws IOException on I/O error writing to stream
	 */
	public static void usage(final ICLIHelper cli) throws IOException {
		cli.println("The following commands are supported:");
		cli.print("Fortress: Print what a player automatically knows ");
		cli.println("about his fortress's tile.");
		final int encounters = HUNTER_HOURS * HOURLY_ENCOUNTERS;
		cli.printf("Hunt/fIsh: Generates up to %i encounters with animals.%n", encounters);
		cli.printf("Gather: Generates up to %i encounters with fields, meadows, ",
				encounters);
		cli.println("groves, orchards, or shrubs.");
		cli.print("hErd: Determine the output from and time required for ");
		cli.println("maintaining a herd.");
		cli.print("Trap: Switch to the trap-modeling program ");
		cli.println("to run trapping or fish-trapping.");
		cli.println("Distance: Report the distance between two points.");
		cli.println("Count: Count how many workers belong to a player.");
		cli.println("Quit: Exit the program.");
	}

	/**
	 * Run the driver.
	 *
	 * @param model the driver model
	 * @throws DriverFailedException on I/O error
	 */
	@Override
	public void startDriver(final IDriverModel model) throws DriverFailedException {
		try (final ICLIHelper cli = new CLIHelper()) {
			repl(model, cli);
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
	@Override
	public void startDriver(final String... args) throws DriverFailedException {
		if (args.length == 0) {
			throw new DriverFailedException("Need one argument",
					                               new IllegalArgumentException("Need one argument"));
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
}
