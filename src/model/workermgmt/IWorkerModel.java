package model.workermgmt;

import java.util.List;

import model.map.Player;
import model.map.fixtures.mobile.IUnit;
import model.misc.IMultiMapModel;

/**
 * An interface for a model to underlie the advancement GUI, etc.
 *
 * @author Jonathan Lovelace
 *
 */
public interface IWorkerModel extends IMultiMapModel {
	/**
	 * @param player a player in the map
	 * @return a list of the units in the map belonging to the player
	 */
	List<IUnit> getUnits(Player player);
	/**
	 * @param player a player in the map
	 * @return the "kinds" of unit that player has.
	 */
	List<String> getUnitKinds(Player player);
	/**
	 * @param player a player in the map
	 * @param kind a "kind" of unit.
	 * @return a list of the units of that kind in the map belonging to that player
	 */
	List<IUnit> getUnits(Player player, String kind);
	/**
	 * Add a unit in its owner's HQ.
	 *
	 * @param unit the unit to add.
	 */
	void addUnit(IUnit unit);
}
