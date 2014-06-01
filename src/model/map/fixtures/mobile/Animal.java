package model.map.fixtures.mobile;

import java.io.IOException;

import model.map.HasImage;
import model.map.HasKind;
import model.map.IFixture;
import model.map.TileFixture;
import model.map.fixtures.UnitMember;

import org.eclipse.jdt.annotation.Nullable;

import util.NullCleaner;

/**
 * An animal or group of animals.
 *
 * TODO: Add more features (population, to start with).
 *
 * @author Jonathan Lovelace
 *
 */
public class Animal implements MobileFixture, HasImage, HasKind, UnitMember {
	/**
	 * ID number.
	 */
	private final int id; // NOPMD

	/**
	 * The name of an image to use for this particular fixture.
	 */
	private String image = "";

	/**
	 * Whether this is really the animal, or only traces.
	 */
	private final boolean traces;
	/**
	 * Kind of animal.
	 */
	private String kind;
	/**
	 * Whether this is a talking animal.
	 */
	private final boolean talking;
	/**
	 * The domestication status of the animal.
	 *
	 * TODO: Should this be an enumerated type?
	 */
	private final String status;

	/**
	 * @return a UID for the fixture.
	 */
	@Override
	public int getID() {
		return id;
	}

	/**
	 * Constructor.
	 *
	 * @param animal what kind of animal
	 * @param tracks whether this is really the animal, or only tracks
	 * @param talks whether this is a talking animal.
	 * @param dStatus domestication status
	 * @param idNum the ID number.
	 */
	public Animal(final String animal, final boolean tracks,
			final boolean talks, final String dStatus, final int idNum) {
		super();
		kind = animal;
		traces = tracks;
		talking = talks;
		status = dStatus;
		id = idNum;
	}

	/**
	 * @return true if this is only traces or tracks, false if this is really
	 *         the animal
	 */
	public boolean isTraces() {
		return traces;
	}

	/**
	 * @return whether the animal is a talking animal
	 */
	public boolean isTalking() {
		return talking;
	}

	/**
	 * @return what kind of animal this is
	 */
	@Override
	public String getKind() {
		return kind;
	}

	/**
	 * @return the domestication status of the animal
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * @return a String representation of the animal
	 */
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder(18 + kind.length());
		if (traces) {
			builder.append("traces of ");
		}
		if (talking) {
			builder.append("talking ");
		}
		builder.append(kind);
		return NullCleaner.assertNotNull(builder.toString());
	}

	/**
	 * TODO: Should depend on the kind of animal.
	 *
	 * @return the name of an image to represent the animal
	 */
	@Override
	public String getDefaultImage() {
		return "animal.png";
	}

	/**
	 * TODO: Should depend on the kind of animal ...
	 *
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
	public boolean equals(@Nullable final Object obj) {
		return this == obj || obj instanceof Animal
				&& kind.equals(((Animal) obj).kind)
				&& ((Animal) obj).traces == traces
				&& ((Animal) obj).talking == talking
				&& status.equals(((Animal) obj).status)
				&& ((Animal) obj).id == id;
	}

	/**
	 * @param obj another UnitMember
	 * @param ostream a stream to report an explanation on
	 * @return whether that member is a subset of this one
	 * @throws IOException on I/O error writing output to the stream
	 */
	@Override
	public boolean isSubset(final IFixture obj, final Appendable ostream)
			throws IOException {
		if (obj.getID() == id) {
			if (obj instanceof Animal) {
				if (!kind.equals(((Animal) obj).getKind())) {
					ostream.append("Different kinds of animal for ID #");
					ostream.append(Integer.toString(id));
					return false;
				} else if (!talking && ((Animal) obj).talking) {
					ostream.append("In animal ID #");
					ostream.append(Integer.toString(id));
					ostream.append(", submap is talking and master doesn't\n");
					return false;
				} else if (traces && !((Animal) obj).traces) {
					ostream.append("In animal ID #");
					ostream.append(Integer.toString(id));
					ostream.append(", submap has animal and master only tracks\n");
					return false;
				} else if (!status.equals(((Animal) obj).status)) {
					ostream.append("Domestication status of animal differs at ID #");
					ostream.append(Integer.toString(id));
					ostream.append('\n');
					return false;
				} else {
					return true;
				}
			} else {
				ostream.append("For ID #");
				ostream.append(Integer.toString(id));
				ostream.append(", different kinds of members");
				return false;
			}
		} else {
			ostream.append("Called with different IDs, #");
			ostream.append(Integer.toString(id));
			ostream.append(" and #");
			ostream.append(Integer.toString(obj.getID()));
			ostream.append('\n');
			return false;
		}
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
	public int compareTo(final TileFixture fix) {
		return fix.hashCode() - hashCode();
	}

	/**
	 * @param fix a fixture
	 * @return whether it's identical to this except ID and DC.
	 */
	@Override
	public boolean equalsIgnoringID(final IFixture fix) {
		return fix instanceof Animal && ((Animal) fix).kind.equals(kind)
				&& ((Animal) fix).traces == traces
				&& ((Animal) fix).status.equals(status)
				&& ((Animal) fix).talking == talking;
	}

	/**
	 * @param nKind the new kind
	 */
	@Override
	public final void setKind(final String nKind) {
		kind = nKind;
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
	 * @return a string describing all animals as a class
	 */
	@Override
	public String plural() {
		return "Animals";
	}
	/**
	 * @return a short description of the fixture
	 */
	@Override
	public String shortDesc() {
		return toString();
	}
}
