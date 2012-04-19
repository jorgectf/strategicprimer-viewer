package model.map.fixtures;

import model.map.HasImage;
import model.map.TileFixture;

/**
 * A fairy. TODO: should probably be a unit, or something.
 * @author Jonathan Lovelace
 *
 */
public class Fairy implements TileFixture, HasImage {
	/**
	 * What kind of fairy (great, lesser, snow ...).
	 */
	private final String kind;
	/**
	 * Constructor.
	 * @param fKind the kind of fairy
	 */
	public Fairy(final String fKind) {
		kind = fKind;
	}
	/**
	 * @return the kind of fairy
	 */
	public String getKind() {
		return kind;
	}
	/**
	 * @return an XML representation of the fairy
	 */
	@Override
	public String toXML() {
		return new StringBuilder("<fairy kind=\"").append(kind).append("\" />").toString();
	}
	/**
	 * @return a String representation of the fairy
	 */
	@Override
	public String toString() {
		return kind + " fairy";
	}
	/**
	 * @return the name of an image to represent the fairy
	 */
	@Override
	public String getImage() {
		return "fairy.png";
	}
	/**
	 * @return a z-value for use in determining the top fixture on a tile
	 */
	@Override
	public int getZValue() {
		return 40;
	}
	/**
	 * @param obj an object
	 * @return whether it's equal to this one
	 */
	@Override
	public boolean equals(final Object obj) {
		return obj instanceof Fairy && ((Fairy) obj).kind.equals(kind);
	}
	/**
	 * @return a hash value for the object
	 */
	@Override
	public int hashCode() {
		return kind.hashCode();
	}
	/**
	 * @param fix a TileFixture to compare to
	 * @return the result of the comparison
	 */
	@Override
	public int compareTo(final TileFixture fix) {
		return fix.hashCode() - hashCode();
	}
}
