package model.map;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A collection of players. Using a simple List doesn't work when -1 is the
 * default index if one isn't given in the XML.
 *
 * @author Jonathan Lovelace
 *
 */
public class PlayerCollection implements Iterable<Player>,
		Subsettable<PlayerCollection> {
	/**
	 * The collection this class wraps.
	 */
	private final Map<Integer, Player> players = new HashMap<Integer, Player>();

	/**
	 * @param player a player-id
	 *
	 * @return the player with that ID, or a new Player with that number if we
	 *         don't have it.
	 */
	public Player getPlayer(final int player) {
		return players.containsKey(Integer.valueOf(player)) ? players
				.get(Integer.valueOf(player)) : new Player(player, "");
	}

	/**
	 * @return an iterator over the players we contain.
	 */
	@Override
	public Iterator<Player> iterator() {
		return players.values().iterator();
	}

	/**
	 * @param obj an object
	 *
	 * @return whether it is another identical PlayerCollection or not
	 */
	@Override
	public boolean equals(final Object obj) {
		return this == obj
				|| (obj instanceof PlayerCollection && ((PlayerCollection) obj).players
						.equals(players));
	}

	/**
	 *
	 * @return a hash value for this collection.
	 */
	@Override
	public int hashCode() {
		return players.hashCode();
	}

	/**
	 * Note that this method currently iterates through all the players to find
	 * the one marked current.
	 *
	 *
	 * @return the current player, or a new player with a negative number and
	 *         the empty string for a name.
	 */
	public Player getCurrentPlayer() {
		for (final Player player : this) {
			if (player.isCurrent()) {
				return player; // NOPMD
			}
		}
		return new Player(-1, "");
	}

	/**
	 *
	 * @return a String representation of the class
	 */
	@Override
	public String toString() {
		return "PlayerCollection";
	}

	/**
	 * @param obj another PlayerCollection
	 * @return whether it's a strict subset of this one
	 * @param out the stream to write details of the differences to
	 */
	@Override
	public boolean isSubset(final PlayerCollection obj, final PrintStream out) {
		for (final Player player : obj) {
			if (!players.containsValue(player)) {
				out.print("Extra player ");
				out.print(player.getName());
				out.print(' ');
				return false; // NOPMD
			}
		}
		return true;
	}
	/**
	 * @param obj an object
	 * @return whether we contain it
	 */
	public boolean contains(final Player obj) {
		return players.containsValue(obj);
	}
	/**
	 * Add a player to the collection.
	 * @param player the player to add
	 * @return whether the collection was changed by the operation.
	 */
	public boolean add(final Player player) {
		final boolean retval = !players.containsValue(player);
		players.put(Integer.valueOf(player.getPlayerId()), player);
		return retval;
	}
	/**
	 * Remove an object from the collection.
	 * @param obj an object
	 * @return true if it was removed as a result of this call
	 */
	public boolean remove(final Object obj) {
		if (obj instanceof Integer) {
			final boolean retval = players.containsKey(obj);
			players.remove(obj);
			return retval; // NOPMD
		} else if (obj instanceof Player) {
			final boolean retval = players.containsValue(obj);
			players.remove(Integer.valueOf(((Player) obj).getPlayerId()));
			return retval; // NOPMD
		} else {
			return false;
		}
	}
}
