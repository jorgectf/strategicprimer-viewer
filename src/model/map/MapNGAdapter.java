package model.map;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import model.map.fixtures.Ground;
import model.map.fixtures.RiverFixture;
import model.map.fixtures.terrain.Forest;
import model.map.fixtures.terrain.Mountain;
import util.NullCleaner;

/**
 * An implementation of IMapNG that is, under the hood, just a MapView.
 *
 * TODO: Write tests.
 *
 * TODO: Write proper toString based on the interface.
 *
 * This is part of the Strategic Primer assistive programs suite developed by
 * Jonathan Lovelace.
 *
 * Copyright (C) 2012-2015 Jonathan Lovelace
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of version 3 of the GNU General Public License as published by the
 * Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <a href="http://www.gnu.org/licenses/">http://www.gnu.org/licenses/</a>.
 *
 * @author Jonathan Lovelace
 *
 * @deprecated We want to use SPMapNG instead
 */
@Deprecated
public class MapNGAdapter implements IMutableMapNG { // $codepro.audit.disable
	/**
	 * The old-interface map we use for our state.
	 */
	private final IMutableMapView state;

	/**
	 * Constructor.
	 *
	 * @param wrapped the map to adapt to the new interface
	 */
	public MapNGAdapter(final IMutableMapView wrapped) {
		state = wrapped;
	}

	/**
	 * @param obj another map
	 * @param ostream the stream to write to
	 * @param context
	 *            a string to print before every line of output, describing the
	 *            context
	 * @return whether it is a strict subset of map.
	 * @throws IOException on I/O error writing output to the stream
	 */
	@Override
	public boolean isSubset(final IMapNG obj, final Appendable ostream,
			final String context) throws IOException {
		if (dimensions().equals(obj.dimensions())) {
			boolean retval = true;
			for (final Player player : obj.players()) {
				if (player != null && !state.getPlayers().contains(player)) {
					// return false;
					retval = false;
					ostream.append(context);
					ostream.append("\tExtra player ");
					ostream.append(player.toString());
					ostream.append('\n');
				}
			}
			for (final Point point : obj.locations()) {
				if (point != null && !isTileSubset(obj, ostream, point, context)) {
					retval = false;
				}
			}
			return retval; // NOPMD
		} else {
			ostream.append("Sizes differ\n");
			return false;
		}
	}

	/**
	 * @param obj the map that might be a subset of us
	 * @param ostream the stream to write detailed results to
	 * @param loc the current location
	 * @param context
	 *            a string to print before every line of output, describing the
	 *            context
	 * @return whether that location fits the "subset" hypothesis.
	 * @throws IOException on I/O error writing output to the stream
	 */
	private boolean isTileSubset(final IMapNG obj, final Appendable ostream,
			final Point loc, final String context) throws IOException {
		boolean retval = true;
		final String ctxt = context + " At " + loc.toString() + ':';
		if (!getBaseTerrain(loc).equals(obj.getBaseTerrain(loc))) { // NOPMD
			// return false;
			retval = false;
			ostream.append(ctxt);
			ostream.append("\tTile types differ\n");
		} else if (isMountainous(loc) != obj.isMountainous(loc)) { // NOPMD
			// return false;
			retval = false;
			ostream.append(ctxt);
			ostream.append("\tReports of mountains differ\n");
		} else if (!areRiversSubset(getRivers(loc), obj.getRivers(loc))) { //NOPMD
			retval = false;
			ostream.append(ctxt);
			ostream.append("\tExtra rivers\n");
		} else if (obj.getForest(loc) != null
				&& !Objects.equals(getForest(loc), obj.getForest(loc))) { // NOPMD
			// return false;
			retval = false;
			ostream.append(ctxt);
			ostream.append("\tPrimary forests differ. Representation error?\n");
		} else if (obj.getGround(loc) != null
				&& !Objects.equals(getGround(loc), obj.getGround(loc))) { // NOPMD
			// return false;
			retval = false;
			ostream.append(ctxt);
			ostream.append("\tPrimary Ground differs. Representation error?\n");
		} else {
			// TODO: Use Guava collection-from-iterable to improve/simplify this
			final List<TileFixture> fixtures = new ArrayList<>();
			for (final TileFixture fix : state.getTile(loc)) {
				fixtures.add(fix);
			}
			for (final TileFixture fix : obj.getOtherFixtures(loc)) {
				if (fix != null && !fixtures.contains(fix)
						&& !Tile.shouldSkip(fix)) {
					// return false;
					retval = false;
					ostream.append(ctxt);
					ostream.append(" Extra fixture:\t");
					ostream.append(fix.toString());
					ostream.append('\n');
				}
			}
		}
		return retval;
	}

	/**
	 * @param ours Our rivers
	 * @param theirs Another map's rivers for the same location
	 * @return whether theirs are a subset of ours.
	 */
	private static boolean areRiversSubset(final Iterable<River> ours,
			final Iterable<River> theirs) {
		final Set<River> theirRivers = EnumSet.noneOf(River.class);
		for (final River river : theirs) {
			theirRivers.add(river);
		}
		for (final River river : ours) {
			theirRivers.remove(river);
		}
		return theirRivers.isEmpty();
	}

	/**
	 * Compare to another map. This method is needed so the class can be put in
	 * a Pair.
	 *
	 * @param other the other map
	 * @return the result of the comparison
	 */
	@Override
	public int compareTo(@Nullable final IMapNG other) {
		if (equals(other)) {
			return 0; // NOPMD
		} else {
			return hashCode() - Objects.hashCode(other);
		}
	}

	/**
	 * @return a view of the players in the map.
	 */
	@Override
	public Iterable<Player> players() {
		return state.getPlayers();
	}

	/**
	 * @return a view of the locations on the map
	 */
	@Override
	public Iterable<Point> locations() {
		return state.getTiles();
	}

	/**
	 * @param location a location
	 * @return the "base terrain" at that location
	 */
	@Override
	public TileType getBaseTerrain(final Point location) {
		return state.getTile(location).getTerrain();
	}

	/**
	 * @param location a location
	 * @return whether that location is mountainous
	 */
	@Override
	public boolean isMountainous(final Point location) {
		if (dimensions().version < 2
				&& TileType.Mountain.equals(getBaseTerrain(location))) {
			return true; // NOPMD
		}
		for (final TileFixture fix : state.getTile(location)) {
			if (fix instanceof Mountain) {
				return true; // NOPMD
			}
		}
		return false;
	}

	/**
	 * @param location a location
	 * @return a view of the river directions, if any, at that location
	 */
	@Override
	public Iterable<River> getRivers(final Point location) {
		if (state.getTile(location).hasRiver()) {
			return state.getTile(location).getRivers(); // NOPMD
		} else {
			return NullCleaner.assertNotNull(EnumSet.noneOf(River.class));
		}
	}

	/**
	 * Implementations should aim to have only the "main" forest here, and any
	 * "extra" forest Fixtures in the "et cetera" collection.
	 *
	 * @param location a location
	 * @return the forest (if any) at that location; null if there is none
	 */
	@Override
	@Nullable
	public Forest getForest(final Point location) {
		Forest retval = null;
		for (final TileFixture fix : state.getTile(location)) {
			if (fix instanceof Forest) {
				if (retval == null || retval.isRows()) {
					retval = (Forest) fix;
				} else {
					break;
				}
			}
		}
		return retval;
	}

	/**
	 * We actually include the main Ground and Forest too; there's no easy way
	 * around that ...
	 *
	 * @param location a location
	 * @return a view of any fixtures on the map that aren't covered in the
	 *         other querying methods.
	 */
	@Override
	public Iterable<TileFixture> getOtherFixtures(final Point location) {
		return state.getTile(location);
	}

	/**
	 * @return the current turn
	 */
	@Override
	public int getCurrentTurn() {
		return state.getCurrentTurn();
	}

	/**
	 * @return the current player
	 */
	@Override
	public Player getCurrentPlayer() {
		return state.getPlayers().getCurrentPlayer();
	}

	/**
	 * Implementations should aim to have only the "main" Ground here, and any
	 * exposed or otherwise "extra" Fixtures in the "et cetera" collection.
	 *
	 * @param location a location
	 * @return the Ground at that location
	 */
	@Override
	@Nullable
	public Ground getGround(final Point location) {
		Ground retval = null;
		for (final TileFixture fix : state.getTile(location)) {
			if (fix instanceof Ground) {
				if (retval == null || retval.isExposed()) {
					retval = (Ground) fix;
				} else {
					break;
				}
			}
		}
		return retval;
	}

	/**
	 * @param obj an object
	 * @return whether it's the same as us---we're a subset of it and it's a
	 *         subset of us
	 */
	@Override
	public boolean equals(@Nullable final Object obj) {
		return this == obj || obj instanceof IMapNG && equalsImpl((IMapNG) obj);
	}
	/**
	 * @param obj another map
	 * @return whether it equals one
	 */
	private boolean equalsImpl(final IMapNG obj) {
		if (dimensions().equals(obj.dimensions())
				&& iterablesEqual(players(), obj.players())
				&& getCurrentTurn() == obj.getCurrentTurn()
				&& getCurrentPlayer().equals(obj.getCurrentPlayer())) {
			for (final Point point : locations()) {
				if (point == null) {
					continue;
				} else if (!getBaseTerrain(point).equals(obj.getBaseTerrain(point))
						|| isMountainous(point) != obj.isMountainous(point)
						|| !iterablesEqual(getRivers(point),
								obj.getRivers(point))
						|| !Objects.equals(getForest(point),
								obj.getForest(point))
						|| !Objects.equals(getGround(point),
								obj.getGround(point))
						|| !iterablesEqual(getOtherFixtures(point),
								obj.getOtherFixtures(point))) {
					return false; // NOPMD
				}
			}
			return true; // NOPMD
		} else {
			return false; // NOPMD
		}
	}

	/**
	 * FIXME: This is probably very slow ...
	 * @param one one iterable
	 * @param two another
	 * @param <T> the type of thing they contain
	 * @return whether they contain the same elements.
	 */
	private static <T> boolean iterablesEqual(final Iterable<T> one,
			final Iterable<T> two) {
		// ESCA-JAVA0177:
		final Collection<T> first;
		if (one instanceof Collection) {
			first = (Collection<T>) one;
		} else {
			first = new ArrayList<>();
			for (final T item : one) {
				first.add(item);
			}
		}
		// ESCA-JAVA0177:
		final Collection<T> second;
		if (two instanceof Collection) {
			second = (Collection<T>) two;
		} else {
			second = new ArrayList<>();
			for (final T item : two) {
				second.add(item);
			}
		}
		return first.containsAll(second) && second.containsAll(first);
	}
	/**
	 * The hash code is based on the dimensions, the current turn, and the
	 * current player; basing it on anything else would certainly break any code
	 * that placed an IMapNG into a hash-table.
	 *
	 * @return a hash code for the object
	 */
	@Override
	public int hashCode() {
		return dimensions().hashCode() + (getCurrentTurn() << 3)
				+ (getCurrentPlayer().hashCode() << 5);
	}

	/**
	 * @return the map's dimensions and version
	 */
	@Override
	public MapDimensions dimensions() {
		return state.getDimensions();
	}
	/**
	 * Add a player.
	 * @param player the player to add.
	 */
	@Override
	public void addPlayer(final Player player) {
		state.addPlayer(player);
	}
	/**
	 * Set the base terrain type at a point.
	 * @param location the location in question
	 * @param terrain the terrain type there
	 */
	@Override
	public void setBaseTerrain(final Point location, final TileType terrain) {
		IMutableTile tile = state.getTile(location);
		tile.setTerrain(terrain);
		state.getTiles().addTile(location, tile);
	}
	/**
	 * Set whether the given point is mountainous.
	 * @param location a location in the map
	 * @param mtn whether it is mountainous
	 */
	@Override
	public void setMountainous(final Point location, final boolean mtn) {
		IMutableTile tile = state.getTile(location);
		if (mtn) {
			for (TileFixture fix : tile) {
				if (fix instanceof Mountain) {
					return;
				}
			}
			tile.addFixture(new Mountain());
			state.getTiles().addTile(location, tile);
		} else {
			if (!isMountainous(location)) {
				return;
			}
			// We take advantage of the fact that all Mountains are equal.
			tile.removeFixture(new Mountain());
			state.getTiles().addTile(location, tile);
		}
	}
	/**
	 * Add rivers to a location.
	 * @param location the location
	 * @param rivers the rivers to add
	 */
	@Override
	public void addRivers(final Point location, @NonNull final River... rivers) {
		IMutableTile tile = state.getTile(location);
		// Taking advantage of the special handling in Tile's addFixture()
		tile.addFixture(new RiverFixture(rivers));
		state.getTiles().addTile(location, tile);
	}
	/**
	 * Remove rivers from a location.
	 * @param location where to remove from
	 * @param rivers the rivers to remove
	 */
	@Override
	public void removeRivers(final Point location, final River... rivers) {
		IMutableTile tile = state.getTile(location);
		if (tile.hasRiver()) {
			for (River river : rivers) {
				if (river == null) {
					continue;
				}
				tile.removeRiver(river);
			}
		}
	}
	/**
	 * Set the forest at the given location.
	 * @param location the location in question
	 * @param forest the new forest there
	 */
	@Override
	public void setForest(final Point location, @Nullable final Forest forest) {
		IMutableTile tile = state.getTile(location);
		Forest old = getForest(location);
		if (forest == null) {
			if (old != null) {
				tile.removeFixture(old);
			}
		} else if (!forest.equals(old)) {
			if (old != null) {
				tile.removeFixture(old);
			}
			tile.addFixture(forest);
			state.getTiles().addTile(location, tile);
		}
	}
	/**
	 * @param location a location in the map
	 * @param ground the new base-ground there
	 */
	@Override
	public void setGround(final Point location, @Nullable final Ground ground) {
		IMutableTile tile = state.getTile(location);
		Ground old = getGround(location);
		if (ground == null) {
			if (old != null) {
				tile.removeFixture(old);
			}
		} else if (!ground.equals(old)) {
			if (old != null) {
				tile.removeFixture(old);
			}
			tile.addFixture(ground);
			state.getTiles().addTile(location, tile);
		}
	}
	/**
	 * @param fix the fixture to add
	 * @param location where to add it
	 */
	@Override
	public void addFixture(final Point location, final TileFixture fix) {
		IMutableTile tile = state.getTile(location);
		tile.addFixture(fix);
		state.getTiles().addTile(location, tile);
	}
	/**
	 * @param fix the fixture to remove
	 * @param location its location
	 */
	@Override
	public void removeFixture(final Point location, final TileFixture fix) {
		IMutableTile tile = state.getTile(location);
		tile.removeFixture(fix);
	}
	/**
	 * @parm player the new current player
	 */
	@Override
	public void setCurrentPlayer(final Player player) {
		state.setCurrentPlayer(player.getPlayerId());
	}
	/**
	 * @param turn the new current turn
	 */
	@Override
	public void setTurn(final int turn) {
		state.setCurrentTurn(turn);
	}
	/**
	 * @return a string representation of the map
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("MapNGAdapter:\n");
		builder.append("Map version: ");
		builder.append(dimensions().version);
		builder.append("\nRows: ");
		builder.append(dimensions().rows);
		builder.append("\nColumns: ");
		builder.append(dimensions().cols);
		builder.append("\nCurrent Turn: ");
		builder.append(getCurrentTurn());
		builder.append("\n\nPlayers:\n");
		for (Player player : players()) {
			if (player != null) {
				builder.append(player.toString());
				if (player.equals(getCurrentPlayer())) {
					builder.append(" (current)");
				}
				builder.append('\n');
			}
		}
		builder.append("\nContents:\n");
		for (Point location : locations()) {
			builder.append("At ");
			builder.append(location.toString());
			builder.append(": ");
			if (isMountainous(location)) {
				builder.append("mountains, ");
			}
			builder.append("ground: ");
			builder.append(getGround(location));
			builder.append(", forest: ");
			builder.append(getForest(location));
			builder.append(", rivers:");
			for (River river : getRivers(location)) {
				builder.append(' ');
				builder.append(river.toString());
			}
			builder.append(", other: ");
			for (TileFixture fixture : getOtherFixtures(location)) {
				builder.append('\n');
				builder.append(fixture.toString());
//				builder.append(" (");
//				builder.append(fixture.getClass().getSimpleName());
//				builder.append(")");
			}
		}
		return NullCleaner.assertNotNull(builder.toString());
	}
	/**
	 * FIXME: Add tests to ensure that a zeroed map is still a subset, and a
	 * non-zeroed map is still equal.
	 *
	 * @return a copy of this map
	 * @param zero
	 *            whether to "zero" sensitive data (probably just DCs)
	 */
	@Override
	public IMapNG copy(final boolean zero) {
		SPMapNG retval = new SPMapNG(dimensions(), state.getPlayers().copy(false),
				getCurrentTurn());
		for (Point point : locations()) {
			if (point == null) {
				continue;
			}
			retval.setBaseTerrain(point, getBaseTerrain(point));
			Ground grd = getGround(point);
			if (grd == null) {
				retval.setGround(point, null);
			} else {
				retval.setGround(point, grd.copy(false));
			}
			Forest frst = getForest(point);
			if (frst == null) {
				retval.setForest(point, null);
			} else {
				retval.setForest(point, frst.copy(false));
			}
			retval.setMountainous(point, isMountainous(point));
			for (River river : getRivers(point)) {
				retval.addRivers(point, river);
			}
			for (TileFixture fixture : getOtherFixtures(point)) {
				if (fixture == null) {
					continue;
				} else if (fixture instanceof IEvent) {
					retval.addFixture(point, fixture.copy(zero));
				} else {
					retval.addFixture(point, fixture.copy(false));
				}
			}
		}
		return retval;
	}
}
