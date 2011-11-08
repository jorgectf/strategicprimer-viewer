package model.map.fixtures;

import model.map.HasImage;
import model.map.TerrainFixture;

/**
 * A hill on the map. Should increase unit's effective vision by a small
 * fraction when the unit is on it, if not in forest.
 * 
 * @author Jonathan Lovelace
 * 
 */
public class Hill implements TerrainFixture, HasImage {
	/**
	 * @return a String representation of the hill.
	 */
	@Override
	public String toString() {
		return "Hill";
	}
	/**
	 * @return an XML representation of the hill.
	 */
	@Override
	public String toXML() {
		return "<hill />";
	}
	/**
	 * @return the name of an image to represent the hill.
	 */
	@Override
	public String getImage() {
		return "hill.png";
	}
	/**
	 * @return a z-value for use in determining the top fixture on a tile
	 */
	@Override
	public int getZValue() {
		return 5;
	}
}
