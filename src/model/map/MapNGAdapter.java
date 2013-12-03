package model.map;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import model.map.fixtures.Ground;
import model.map.fixtures.terrain.Forest;
import model.map.fixtures.terrain.Mountain;

import org.eclipse.jdt.annotation.Nullable;

/**
 * An implementation of IMapNG that is, under the hood, just a MapView.
 *
 * TODO: Write tests.
 *
 * TODO: Write proper toString based on the interface.
 *
 * @author Jonathan Lovelace
 *
 */
public class MapNGAdapter implements IMapNG { // $codepro.audit.disable
	/**
	 * The old-interface map we use for our state.
	 */
	private final MapView state;

	/**
	 * Constructor.
	 *
	 * @param wrapped the map to adapt to the new interface
	 */
	public MapNGAdapter(final MapView wrapped) {
		state = wrapped;
	}

	/**
	 * @param obj another map
	 * @param out the stream to write to
	 * @return whether it is a strict subset of this map.
	 */
	@Override
	public boolean isSubset(final IMapNG obj, final PrintWriter out) {
		if (dimensions().equals(obj.dimensions())) {
			boolean retval = true;
			for (final Player player : obj.players()) {
				if (player != null && !state.getPlayers().contains(player)) {
					// return false;
					retval = false;
					out.print("Extra player ");
					out.println(player);
				}
			}
			for (final Point point : obj.locations()) {
				if (point != null && !isTileSubset(obj, out, point)) {
					retval = false;
				}
			}
			return retval; // NOPMD
		} else {
			out.println("Sizes differ");
			return false;
		}
	}

	/**
	 * @param obj the map that might be a subset of us
	 * @param out the stream to write detailed results to
	 * @param point the current location
	 * @return whether that location fits the "subset" hypothesis.
	 */
	private boolean isTileSubset(final IMapNG obj, final PrintWriter out,
			final Point point) {
		boolean retval = true;
		if (!getBaseTerrain(point).equals(obj.getBaseTerrain(point))) { // NOPMD
			// return false;
			retval = false;
			out.print("Tile types differ at ");
			out.println(point);
		} else if (isMountainous(point) != obj.isMountainous(point)) { // NOPMD
			// return false;
			retval = false;
			out.print("Reports of mountains differ at ");
			out.println(point);
		} else if (!areRiversSubset(getRivers(point), obj.getRivers(point))) { // NOPMD
			retval = false;
			out.print("Extra rivers at ");
			out.println(point);
		} else if (!safeEquals(getForest(point), obj.getForest(point))) { // NOPMD
			// return false;
			retval = false;
			out.print("Primary forests differ at ");
			out.print(point);
			out.println(", may be representation error");
		} else if (!safeEquals(getGround(point), obj.getGround(point))) { // NOPMD
			// return false;
			retval = false;
			out.print("Primary Ground differs at ");
			out.print(point);
			out.println(", may be representation error");
		} else {
			// TODO: Use Guava collection-from-iterable to improve/simplify this
			final List<TileFixture> fixtures = new ArrayList<>();
			for (final TileFixture fix : state.getTile(point)) {
				fixtures.add(fix);
			}
			for (final TileFixture fix : obj.getOtherFixtures(point)) {
				if (fix != null && !fixtures.contains(fix)
						&& !Tile.shouldSkip(fix)) {
					// return false;
					retval = false;
					out.print("Extra fixture ");
					out.print(fix);
					out.print(" at ");
					out.println(point);
				}
			}
		}
		return retval;
	}

	/**
	 * Since getForest and getGround can both return null, we can't just use
	 * equals() for them.
	 *
	 * @param one one object
	 * @param two another
	 * @param <T> the type of the two objects
	 * @return true if both are null or if they are equal according to one's
	 *         equals(), false otherwise.
	 */
	private static <T> boolean safeEquals(@Nullable final T one,
			@Nullable final T two) {
		return one == null ? two == null : one.equals(two);
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
		return !theirRivers.isEmpty();
	}

	/**
	 * Compare to another map. This method is needed so the class can be put in
	 * a Pair.
	 *
	 * @param other the other map
	 * @return the result of the comparison
	 */
	@Override
	public int compareTo(final IMapNG other) {
		return equals(other) ? 0 : hashCode() - other.hashCode();
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
	@SuppressWarnings("deprecation")
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
			final Set<River> none = EnumSet.noneOf(River.class);
			assert none != null;
			return none;
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
		return this == obj
				|| (obj instanceof IMapNG && equalsImpl((IMapNG) obj));
	}
	/**
	 * @param obj another map
	 * @return whether it equals this one
	 */
	private boolean equalsImpl(final IMapNG obj) {
		if (!dimensions().equals(obj.dimensions())
				|| !iterablesEqual(players(), obj.players())
				|| getCurrentTurn() != obj.getCurrentTurn()
				|| !getCurrentPlayer().equals(obj.getCurrentPlayer())) {
			return false; // NOPMD
		} else {
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
					return false;
				}
			}
			return true;
		}
	}
	/**
	 * FIXME: This is probably very slow ...
	 * @param one one iterable
	 * @param two another
	 * @param <T> the type of thing they contain
	 * @return whether they contain the same elements.
	 */
	private static <T> boolean iterablesEqual(final Iterable<T> one, final Iterable<T> two) {
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
			for (final T item : second) {
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
		return dimensions().hashCode() + getCurrentTurn() << 3 + getCurrentPlayer().hashCode() << 5;
	}

	/**
	 * @return the map's dimensions and version
	 */
	@Override
	public MapDimensions dimensions() {
		return state.getDimensions();
	}
}
