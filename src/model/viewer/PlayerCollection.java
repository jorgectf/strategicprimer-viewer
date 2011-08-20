package model.viewer;

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
public class PlayerCollection implements Iterable<Player> {
	/**
	 * The collection this class wraps.
	 */
	private final Map<Integer, Player> players = new HashMap<Integer, Player>();
	/**
	 * "An unknown player" --- used when we're asked for an ID we don't contain.
	 */
	public static final Player NULL_PLAYER = new Player(-1, "Unknown");
	/**
	 * Add a player.
	 * @param player the player to add.
	 */
	public void addPlayer(final Player player) {
		players.put(player.getId(), player);
	}
	/**
	 * @param player a player-id
	 * @return the player with that ID, or an "unknown-player" player if no match.
	 */
	public Player getPlayer(final int player) {
		return players.containsKey(player) ? players.get(player) : NULL_PLAYER;
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
	 * @return whether it is another identical PlayerCollection or not 
	 */
	@Override
	public boolean equals(final Object obj) {
		return obj instanceof PlayerCollection && ((PlayerCollection) obj).players.equals(players);
	}
	/**
	 * @return a hash value for this collection.
	 */
	@Override
	public int hashCode() {
		return players.hashCode();
	}
}
