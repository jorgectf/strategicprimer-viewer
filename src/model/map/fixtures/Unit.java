package model.map.fixtures;

import model.map.HasImage;
import model.map.Player;
import model.map.TileFixture;

/**
 * A unit on the map.
 * 
 * @author Jonathan Lovelace
 * 
 */
public class Unit implements TileFixture, HasImage {
	/**
	 * The player that owns the unit.
	 */
	private final Player owner;
	/**
	 * What kind of unit this is.
	 */
	private final String kind;
	/**
	 * The name of this unit.
	 */
	private final String name;

	/**
	 * FIXME: We need some more members -- something about stats. What else?
	 * 
	 * @param unitOwner
	 *            the unit's owner
	 * @param unitType
	 *            the kind of unit
	 * @param unitName
	 *            the unit's name
	 */
	/**
	 * Constructor.
	 * 
	 * @param unitOwner
	 *            the player that owns the unit
	 * @param unitType
	 *            the type of unit
	 * @param unitName
	 *            the name of this unit
	 */
	public Unit(final Player unitOwner, final String unitType,
			final String unitName) {
		owner = unitOwner;
		kind = unitType;
		name = unitName;
	}

	/**
	 * 
	 * @return the player that owns the unit
	 */
	public final Player getOwner() {
		return owner;
	}

	/**
	 * 
	 * @return the kind of unit
	 */
	public final String getKind() {
		return kind;
	}

	/**
	 * 
	 * @return the name of the unit
	 */
	public final String getName() {
		return name;
	}

	/**
	 * @param obj
	 *            an object
	 * 
	 * @return whether it's an identical Unit.
	 */
	@Override
	public boolean equals(final Object obj) {
		return this == obj
				|| (obj instanceof Unit && ((Unit) obj).owner.equals(owner)
						&& (((Unit) obj).kind.equals(kind)) && (((Unit) obj).name
							.equals(name)));
	}

	/**
	 * 
	 * @return a hash-code for the object
	 */
	@Override
	public int hashCode() {
		return (kind.hashCode()) + owner.hashCode() << 2 + (name.hashCode()) << 4;
	}

	/**
	 * 
	 * @return a String representation of the Unit.
	 */
	@Override
	public String toString() {
		return "Unit of type " + kind + ", belonging to player " + owner
				+ ", named " + name;
	}

	/**
	 * @param fix
	 *            A TileFixture to compare to
	 * 
	 * @return the result of the comparison
	 */
	@Override
	public int compareTo(final TileFixture fix) {
		return fix.hashCode() - hashCode();
	}
	/**
	 * @return an XML representation of the unit.
	 */
	@Override
	public String toXML() {
		final StringBuilder sbuild = new StringBuilder("<unit owner=\"");
		sbuild.append(owner.getId());
		if (!"".equals(kind)) {
			sbuild.append("\" kind=\"");
			sbuild.append(kind);
		}
		if (!"".equals(name)) {
			sbuild.append("\" name=\"");
			sbuild.append(name);
		}
		sbuild.append("\" />");
		return sbuild.toString();
	}
	/**
	 * TODO: Should be per-unit-type ...
	 * @return the name of an image to represent the unit.
	 */
	@Override
	public String getImage() {
		return "unit.png";
	}
	/**
	 * TODO: But how to determine which unit?
	 * @return a z-value for use in determining the top fixture on a tile
	 */
	@Override
	public int getZValue() {
		return 70;
	}
}
