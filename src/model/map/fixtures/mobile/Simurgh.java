package model.map.fixtures.mobile;

import model.map.HasImage;
import model.map.IFixture;
import model.map.TileFixture;
import model.map.fixtures.UnitMember;

/**
 * A simurgh. TODO: should probably be a unit, or something.
 *
 * @author Jonathan Lovelace
 *
 */
public class Simurgh implements MobileFixture, HasImage, UnitMember {
	/**
	 * Version UID for serialization.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * @param idNum the ID number.
	 */
	public Simurgh(final int idNum) {
		super();
		id = idNum;
	}

	/**
	 * @return a String representation of the djinn
	 */
	@Override
	public String toString() {
		return "simurgh";
	}

	/**
	 * @return the name of an image to represent the simurgh
	 */
	@Override
	public String getImage() {
		return "simurgh.png";
	}

	/**
	 * @return a z-value for use in determining the top fixture on a tile
	 */
	@Override
	public int getZValue() {
		return 45;
	}

	/**
	 * @param obj an object
	 * @return whether it's equal to this one
	 */
	@Override
	public boolean equals(final Object obj) {
		return this == obj || (obj instanceof Simurgh && ((TileFixture) obj).getID() == id);
	}

	/**
	 * @return a hash value for the object
	 */
	@Override
	public int hashCode() {
		return id;
	}

	/**
	 * @param fix a TileFixture to compare to
	 * @return the result of the comparison
	 */
	@Override
	public int compareTo(final TileFixture fix) {
		return fix.hashCode() - hashCode();
	}

	/**
	 * ID number.
	 */
	private final int id; // NOPMD

	/**
	 * @return a UID for the fixture.
	 */
	@Override
	public int getID() {
		return id;
	}

	/**
	 * @param fix a fixture
	 * @return whether it's identical to this except ID and DC.
	 */
	@Override
	public boolean equalsIgnoringID(final IFixture fix) {
		return fix instanceof Simurgh;
	}
}
