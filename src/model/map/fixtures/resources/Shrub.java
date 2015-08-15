package model.map.fixtures.resources;

import org.eclipse.jdt.annotation.Nullable;

import model.map.HasKind;
import model.map.IFixture;
import model.map.TileFixture;

/**
 * A TileFixture to represent shrubs, or their aquatic equivalents, on a tile.
 *
 * @author Jonathan Lovelace
 *
 */
public class Shrub implements HarvestableFixture, HasKind {
	/**
	 * The name of an image to use for this particular fixture.
	 */
	private String image = "";

	/**
	 * A description of what kind of shrub this is.
	 */
	private String description;

	/**
	 * Constructor.
	 *
	 * @param desc a description of the shrub.
	 * @param idNum the ID number.
	 */
	public Shrub(final String desc, final int idNum) {
		description = desc;
		id = idNum;
	}

	/**
	 * @return a copy of this shrub
	 * @param zero ignored, as there's no sensitive data
	 */
	@Override
	public Shrub copy(final boolean zero) {
		Shrub retval = new Shrub(description, id);
		retval.setImage(image);
		return retval;
	}
	/**
	 * @return a description of the shrub
	 */
	@Override
	public String getKind() {
		return description;
	}

	/**
	 * @return the name of an image to represent the shrub.
	 */
	@Override
	public String getDefaultImage() {
		return "shrub.png";
	}

	/**
	 * @return the description of the shrub
	 */
	@Override
	public String toString() {
		return getKind();
	}

	/**
	 * @return a z-value for use in determining the top fixture on a tile
	 */
	@Override
	public int getZValue() {
		return 15;
	}

	/**
	 * @param obj an object
	 * @return whether it's equal to this one
	 */
	@Override
	public boolean equals(@Nullable final Object obj) {
		return this == obj || obj instanceof Shrub && equalsImpl((Shrub) obj);
	}
	/**
	 * @param obj a shrub
	 * @return whether it's equal to this one
	 */
	private boolean equalsImpl(final Shrub obj) {
		return description.equals(obj.description) && id == obj.getID();
	}
	/**
	 * @return a hash value for the object
	 */
	@Override
	public int hashCode() {
		return id;
	}

	/**
	 * @param fix A TileFixture to compare to
	 *
	 * @return the result of the comparison
	 */
	@Override
	public int compareTo(@Nullable final TileFixture fix) {
		if (fix == null) {
			throw new IllegalArgumentException("Compared to null fixture");
		}
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
		return fix instanceof Shrub
				&& description.equals(((Shrub) fix).description);
	}

	/**
	 * @param kind the new kind
	 */
	@Override
	public final void setKind(final String kind) {
		description = kind;
	}

	/**
	 * @param img the name of an image to use for this particular fixture
	 */
	@Override
	public void setImage(final String img) {
		image = img;
	}

	/**
	 * @return the name of an image to use for this particular fixture.
	 */
	@Override
	public String getImage() {
		return image;
	}

	/**
	 * @return a string describing all shrubs as a class
	 */
	@Override
	public String plural() {
		return "Shrubs";
	}
	/**
	 * @return a short description of the fixture
	 */
	@Override
	public String shortDesc() {
		return toString();
	}
}
