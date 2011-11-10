package model.map.fixtures;

import model.map.HasImage;
import model.map.TileFixture;
/**
 * A TileFixture to represent the basic rock beneath the tile, possibly exposed.
 * @author Jonathan Lovelace
 *
 */
public class Ground implements TileFixture, HasImage {
	/**
	 * @return an XML representation of the Fixture.
	 */
	@Override
	public String toXML() {
		return new StringBuilder("<ground ground=\"").append(description)
				.append("\" exposed=\"").append(exposed).append("\" />")
				.toString();
	}
	/**
	 * Constructor.
	 * @param desc a description of the ground (the type of rock)
	 * @param exp whether it's exposed. (If not, the tile should also include a grass or forest Fixture ...)  
	 */
	public Ground(final String desc, final boolean exp) {
		description = desc;
		exposed = exp;
	}
	/**
	 * A description of the ground (the kind of rock).
	 */
	private final String description;
	/**
	 * Whether the ground is exposed.
	 */
	private final boolean exposed;
	/**
	 * @return whether the ground is exposed
	 */
	public boolean isExposed() {
		return exposed;
	}
	/**
	 * @return a description of the grond
	 */
	public String getDescription() {
		return description;
	}
	/**
	 * @return the name of an image to represent the ground.
	 */
	@Override
	public String getImage() {
		return exposed ? "expground.png" : "blank.png";
	}
	/**
	 * TODO: Should perhaps depend on whether it's exposed or not.
	 * @return a z-value for use in determining the top fixture on a tile
	 */
	@Override
	public int getZValue() {
		return 0;
	}
	/**
	 * @param obj an object
	 * @return whether it's equal to this one
	 */
	@Override
	public boolean equals(final Object obj) {
		return obj instanceof Ground && description.equals(((Ground) obj).description)
				&& exposed == ((Ground) obj).exposed;
	}
	/**
	 * @return a hash value for the object
	 */
	@Override
	public int hashCode() {
		return description.hashCode() << (exposed ? 1 : 0);
	}

}
