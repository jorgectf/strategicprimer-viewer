package common.map.fixtures.mobile;

import common.map.IFixture;
import common.map.HasMutableImage;
import common.map.HasKind;

import java.util.function.Consumer;

/**
 * A centaur.
 */
public class Centaur implements Immortal, HasMutableImage, HasKind {
	/**
	 * @param kind what kind of centaur
	 * @param id ID number
	 */
	public Centaur(final String kind, final int id) {
		this.kind = kind;
		this.id = id;
	}

	/**
	 * What kind of centaur.
	 */
	private final String kind;

	/**
	 * What kind of centaur.
	 */
	@Override
	public String getKind() {
		return kind;
	}

	/**
	 * ID number.
	 */
	private final int id;

	/**
	 * ID number.
	 */
	@Override
	public int getId() {
		return id;
	}

	/**
	 * The filename of an image to use as an icon for this instance.
	 */
	private String image = "";

	/**
	 * The filename of an image to use as an icon for this instance.
	 */
	@Override
	public String getImage() {
		return image;
	}

	/**
	 * @param image The filename of an image to use as an icon for this instance.
	 */
	@Override
	public void setImage(final String image) {
		this.image = image;
	}

	@Override
	public Centaur copy(final CopyBehavior zero) {
		final Centaur retval = new Centaur(kind, id);
		retval.setImage(image);
		return retval;
	}

	@Override
	public String getShortDescription() {
		return kind + " centaur";
	}

	@Override
	public String toString() {
		return getShortDescription();
	}

	@Override
	public String getDefaultImage() {
		return "centaur.png";
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof Centaur c) {
			return c.getKind().equals(kind) && c.getId() == id;
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return id;
	}

	@Override
	public boolean equalsIgnoringID(final IFixture fixture) {
		if (fixture instanceof Centaur c) {
			return c.getKind().equals(kind);
		} else {
			return false;
		}
	}

	@Override
	public boolean isSubset(final IFixture obj, final Consumer<String> report) {
		if (obj.getId() == id) {
			if (obj instanceof Centaur c) {
				if (c.getKind().equals(kind)) {
					return true;
				} else {
					report.accept("\tDifferent kinds of centaur for ID #" + id);
					return false;
				}
			} else {
				report.accept(String.format("\tFor ID #%d, different kinds of members", id));
				return false;
			}
		} else {
			report.accept(String.format("\tCalled with different IDs, #%d and %d",
				id, obj.getId()));
			return false;
		}
	}

	@Override
	public String getPlural() {
		return "Centaurs";
	}

	@Override
	public int getDC() {
		return 20;
	}
}
