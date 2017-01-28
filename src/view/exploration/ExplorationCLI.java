package view.exploration;

import controller.map.misc.ICLIHelper;
import controller.map.misc.IDFactoryFiller;
import controller.map.misc.IDRegistrar;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.exploration.HuntingModel;
import model.exploration.IExplorationModel;
import model.exploration.IExplorationModel.Speed;
import model.listeners.MovementCostListener;
import model.listeners.MovementCostSource;
import model.map.HasOwner;
import model.map.IMutableMapNG;
import model.map.Player;
import model.map.Point;
import model.map.TileFixture;
import model.map.fixtures.Ground;
import model.map.fixtures.mobile.Animal;
import model.map.fixtures.mobile.IUnit;
import model.map.fixtures.mobile.SimpleMovement;
import model.map.fixtures.resources.CacheFixture;
import model.map.fixtures.terrain.Forest;
import model.map.fixtures.terrain.Mountain;
import org.eclipse.jdt.annotation.Nullable;
import util.Accumulator;
import util.IntHolder;
import util.Pair;

import static model.map.TileType.Ocean;

/**
 * A CLI to help running exploration. Now separated from the "driver" bits, to simplify
 * things.
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
public final class ExplorationCLI implements MovementCostSource {
	/**
	 * Logger.
	 */
	private static final Logger LOGGER =
			Logger.getLogger(ExplorationCLI.class.getName());
	/**
	 * The direction prompt.
	 */
	private static final String PROMPT =
			"0 = N, 1 = NE, 2 = E, 3 = SE, 4 = S, 5 = SW, 6 = W, 7 = NW, 8 = Stay Here," +
					" 9 = Change Speed, 10 = Quit.";
	/**
	 * The prompt to use when the user tells the unit to go nowhere.
	 */
	private static final String FEALTY_PROMPT =
			"Should any village here swear to the player?  ";
	/**
	 * The exploration model we use.
	 */
	private final IExplorationModel model;
	/**
	 * The helper to handle user I/O.
	 */
	private final ICLIHelper helper;
	/**
	 * A "hunting model," to get the animals to have traces of.
	 */
	private final HuntingModel huntingModel;
	/**
	 * An ID number factory for the animal tracks.
	 */
	private final IDRegistrar idf;
	/**
	 * The list of movement-cost listeners.
	 */
	private final Collection<MovementCostListener> mcListeners = new ArrayList<>();
	/**
	 * The explorer's current movement speed.
	 */
	private Speed speed = Speed.Normal;
	/**
	 * Constructor.
	 * @param explorationModel the exploration model to use
	 * @param cli              the helper to handle user I/O
	 */
	public ExplorationCLI(final IExplorationModel explorationModel,
						  final ICLIHelper cli) {
		model = explorationModel;
		helper = cli;
		huntingModel = new HuntingModel(model.getMap());
		idf = IDFactoryFiller.createFactory(model);
	}

	/**
	 * Have the user choose a player.
	 *
	 * @return the chosen player, or a player with a negative number if no choice made.
	 * @throws IOException on I/O error
	 */
	@Nullable
	public Player choosePlayer() throws IOException {
		final List<Player> players = model.getPlayerChoices();
		final int playerNum = helper.chooseFromList(players,
				"The players shared by all the maps:",
				"No players shared by all the maps.",
				"Please make a selection: ", true);
		if ((playerNum < 0) || (playerNum >= players.size())) {
			return null;
		} else {
			return players.get(playerNum);
		}
	}

	/**
	 * Have the player choose a unit.
	 *
	 * @param player the player to whom the unit must belong
	 * @return the chosen unit, or a unit with a negative ID number if none selected.
	 * @throws IOException on I/O error
	 */
	@Nullable
	public IUnit chooseUnit(final Player player) throws IOException {
		final List<IUnit> units = model.getUnits(player);
		final int unitNum = helper.chooseFromList(units, "Player's units:",
				"That player has no units in the master map.",
				"Please make a selection: ", true);
		if ((unitNum < 0) || (unitNum >= units.size())) {
			return null;
		} else {
			return units.get(unitNum);
		}
	}

	/**
	 * Have the player move the selected unit. Throws an exception if no unit is
	 * selected. Movement cost is reported by the driver model to all registered
	 * MovementCostListeners, while any additional costs for non-movement
	 * actions are reported by this class, so a listener should be attached to both.
	 *
	 * @throws IOException on I/O error
	 */
	public void move() throws IOException {
		final IUnit mover = model.getSelectedUnit();
		final int directionNum = helper.inputNumber("Direction to move: ");
		if (directionNum == 9) {
			changeSpeed();
			return;
		} else if (directionNum > 9) {
			fireMovementCost(Integer.MAX_VALUE);
			return;
		}
		final IExplorationModel.Direction direction =
				IExplorationModel.Direction.values()[directionNum];
		final Point point = model.getSelectedUnitLocation();
		final Point dPoint = model.getDestination(point, direction);
		try {
			model.move(direction, speed);
		} catch (final SimpleMovement.TraversalImpossibleException except) {
			LOGGER.log(Level.FINEST, "Attempted movement to impassable destination",
					except);
			helper.print("That direction is impassable; we've made sure ");
			helper.println("all maps show that at a cost of 1 MP");
			return;
		}
		final Collection<TileFixture> constants = new ArrayList<>();
		final IMutableMapNG map = model.getMap();
		if (map.isMountainous(dPoint)) {
			constants.add(new Mountain());
		}
		final List<TileFixture> allFixtures = new ArrayList<>();
		final Consumer<TileFixture> consider = fix -> {
			if (SimpleMovement.shouldAlwaysNotice(mover, fix)) {
				constants.add(fix);
			} else if (SimpleMovement.shouldSometimesNotice(mover, speed, fix)) {
				allFixtures.add(fix);
			}
		};
		consider.accept(map.getGround(dPoint));
		consider.accept(map.getForest(dPoint));
		map.streamOtherFixtures(dPoint).forEach(consider);
		final String tracks = getAnimalTraces(dPoint);
		if (!HuntingModel.NOTHING.equals(tracks)) {
			allFixtures.add(new Animal(tracks, true, false, "wild", idf.createID()));
		}
		if (IExplorationModel.Direction.Nowhere == direction) {
			if (helper.inputBooleanInSeries(FEALTY_PROMPT)) {
				model.swearVillages();
			}
			if (helper.inputBooleanInSeries("Dig to expose some ground here?")) {
				model.dig();
			}
		}
		helper.printf("The explorer comes to %s, a tile with terrain %s%n",
				dPoint.toString(), map.getBaseTerrain(dPoint).toString());
		final List<TileFixture> noticed =
				SimpleMovement.selectNoticed(allFixtures, x -> x, mover, speed);
		if (noticed.isEmpty()) {
			helper.println("The following were automatically noticed:");
		} else if (noticed.size() > 1) {
			helper.printf(
					"The following were noticed, all but the last %d automatically:%n",
					Integer.valueOf(noticed.size()));
		} else {
			helper.println("The following were noticed, all but the last automatically:");
		}
		constants.addAll(noticed);
		for (final TileFixture fix : constants) {
			printAndTransferFixture(dPoint, fix, mover);
		}
	}
	/**
	 * Get an animal for there to be traces of near the given location.
	 * @param point a location
	 * @return a randomly-selected animal for that location to possibly have traces of
	 */
	private String getAnimalTraces(final Point point) {
		if (Ocean == model.getMap().getBaseTerrain(point)) {
			return huntingModel.fish(point, 1).get(0);
		} else {
			return huntingModel.hunt(point, 1).get(0);
		}
	}
	/**
	 * Let the user change the explorer's speed.
	 * @throws IOException on I/O error
	 */
	private void changeSpeed() throws IOException {
		final int newSpeed =
				helper.chooseFromList(Arrays.asList(Speed.values()), "Possible Speeds:",
						"No speeds available", "Chosen Speed: ", true);
		if (newSpeed >= 0 && newSpeed < Speed.values().length) {
			speed = Speed.values()[newSpeed];
		}
	}
	/**
	 * Copy the given fixture to subordinate maps and print it to the output stream.
	 * @param dPoint the current location
	 * @param fix    the fixture to copy to subordinate maps. May be null, to simplify
	 *                  the caller.
	 * @param mover  the current unit (needed for its owner)
	 */
	private void printAndTransferFixture(final Point dPoint,
										 @Nullable final TileFixture fix,
										 final HasOwner mover) {
		if (fix != null) {
			helper.println(fix.toString());
			final boolean zero = (fix instanceof HasOwner) &&
										 !((HasOwner) fix).getOwner()
												  .equals(mover.getOwner());
			for (final Pair<IMutableMapNG, Optional<Path>> pair :
					model.getSubordinateMaps()) {
				final IMutableMapNG map = pair.first();
				if ((fix instanceof Ground) && (map.getGround(dPoint) == null)) {
					map.setGround(dPoint, ((Ground) fix).copy(false));
				} else if ((fix instanceof Forest) && (map.getForest(dPoint) == null)) {
					map.setForest(dPoint, ((Forest) fix).copy(false));
				} else if (fix instanceof Mountain) {
					map.setMountainous(dPoint, true);
				} else {
					map.addFixture(dPoint, fix.copy(zero));
				}
			}
		}
		if (fix instanceof CacheFixture) {
			model.getMap().removeFixture(dPoint, fix);
		}
	}

	/**
	 * Ask the user for directions the unit should move until it runs out of MP or the
	 * user decides to quit.
	 *
	 * @throws IOException on I/O error.
	 */
	public void moveUntilDone() throws IOException {
		final IUnit selUnit = model.getSelectedUnit();
		if (selUnit == null) {
			helper.println("No unit is selected");
		} else {
			helper.println("Details of the unit:");
			helper.println(selUnit.verbose());
			final int totalMP = helper.inputNumber("MP the unit has: ");
			final Accumulator movement = new IntHolder(totalMP);
			model.addMovementCostListener(cost -> movement.add(0 - cost));
			addMovementCostListener(cost -> movement.add(0 - cost));
			while (movement.getValue() > 0) {
				helper.printf("%d MP of %d remaining.%nCurrent speed: %s%n%s%n",
						Integer.valueOf(movement.getValue()), Integer.valueOf(totalMP),
						speed.getName(), PROMPT);
				move();
			}
		}
	}

	/**
	 * A trivial toString().
	 * @return a String representation of the object
	 */
	@SuppressWarnings("MethodReturnAlwaysConstant")
	@Override
	public String toString() {
		return "ExplorationCLI";
	}

	/**
	 * Add a listener.
	 * @param listener the listener to add
	 */
	@SuppressWarnings("ParameterNameDiffersFromOverriddenParameter")
	@Override
	public void addMovementCostListener(final MovementCostListener listener) {
		mcListeners.add(listener);
	}

	/**
	 * Remove a listener.
	 * @param listener the listener to remove
	 */
	@SuppressWarnings("ParameterNameDiffersFromOverriddenParameter")
	@Override
	public void removeMovementCostListener(final MovementCostListener listener) {
		mcListeners.remove(listener);
	}

	/**
	 * Tell listeners of a movement cost.
	 *
	 * @param cost how much the move cost
	 */
	private void fireMovementCost(final int cost) {
		for (final MovementCostListener list : mcListeners) {
			list.deduct(cost);
		}
	}
}
