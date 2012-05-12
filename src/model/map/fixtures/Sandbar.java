package model.map.fixtures;

import model.map.HasImage;
import model.map.TerrainFixture;
import model.map.TileFixture;
/**
 * A sandbar on the map.
 * @author Jonathan Lovelace
 *
 */
public class Sandbar implements TerrainFixture, HasImage {
	/**
	 * @param idNum the ID number.
	 */
	public Sandbar(final long idNum) {
		id = idNum;
	}
	/**
	 * @return a String representation of the sandbar.
	 */
	@Override
	public String toString() {
		return "Sandbar";
	}
	/**
	 * @return an XML representaiton of the sandbar 
	 */
	@Override
	@Deprecated
	public String toXML() {
		return new StringBuilder("<sandbar id=\"").append(id).append("\" />")
				.toString();
	}
	/**
	 * @return the name o an image to represent the sandbar.
	 */
	@Override
	public String getImage() {
		return "sandbar.png";
	}
	/**
	 * @return a z-value for use in determining the top fixture on a tile
	 */
	@Override
	public int getZValue() {
		return 5;
	}
	/**
	 * @param obj an object
	 * @return whether it's equal to this one
	 */
	@Override
	public boolean equals(final Object obj) {
		return obj instanceof Sandbar && id == ((TileFixture) obj).getID();
	}
	/**
	 * @return a hash value for the object
	 */
	@Override
	public int hashCode() {
		return (int) id;
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
	 * ID number.
	 */
	private final long id; // NOPMD
	/**
	 * @return a UID for the fixture.
	 */
	@Override
	public long getID() {
		return id;
	}
	/**
	 * @param fix a fixture
	 * @return whether it's identical to this except ID and DC.
	 */
	@Override
	public boolean equalsIgnoringID(final TileFixture fix) {
		return fix instanceof Sandbar;
	}
	/**
	 * @return The name of the file this is to be written to.
	 */
	@Override
	public String getFile() {
		return file;
	}
	/**
	 * @param fileName the name of the file this should be written to.
	 */
	@Override
	public void setFile(final String fileName) {
		file = fileName;
	}
	/**
	 * The name of the file this is to be written to.
	 */
	private String file;
}
