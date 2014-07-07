package model.map;


/**
 * An interface to specify all the mutator methods that were in IMap.
 * @author Jonathan Lovelace
 */
public interface IMutableMap extends IMap {
	/**
	 * Add a player to the game.
	 *
	 * @param player the player to add
	 */
	void addPlayer(Player player);

}
