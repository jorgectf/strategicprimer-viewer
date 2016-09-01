package model.map.fixtures.mobile;

import java.io.IOException;
import model.map.HasMutableImage;
import model.map.HasMutableKind;
import model.map.IFixture;
import model.map.fixtures.UnitMember;
import org.eclipse.jdt.annotation.Nullable;
import util.LineEnd;

/**
 * A fairy. TODO: should probably be a unit, or something.
 *
 * This is part of the Strategic Primer assistive programs suite developed by Jonathan
 * Lovelace.
 *
 * Copyright (C) 2012-2016 Jonathan Lovelace
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of version 3 of the GNU General Public License as published by the Free Software
 * Foundation; see COPYING or
 * <a href="http://www.gnu.org/licenses/">http://www.gnu.org/licenses/</a>.
 *
 * @author Jonathan Lovelace
 */
public class Fairy implements MobileFixture, HasMutableImage, HasMutableKind, UnitMember {
	/**
	 * What kind of fairy (great, lesser, snow ...).
	 */
	private String kind;

	/**
	 * The name of an image to use for this particular fixture.
	 */
	private String image = "";

	/**
	 * Constructor.
	 *
	 * @param fKind the kind of fairy
	 * @param idNum the ID number.
	 */
	public Fairy(final String fKind, final int idNum) {
		kind = fKind;
		id = idNum;
	}

	/**
	 * @param zero ignored, as a fairy has no sensitive information
	 * @return a copy of this fairy
	 */
	@SuppressWarnings("MethodReturnOfConcreteClass")
	@Override
	public Fairy copy(final boolean zero) {
		final Fairy retval = new Fairy(kind, id);
		retval.image = image;
		return retval;
	}

	/**
	 * @return the kind of fairy
	 */
	@Override
	public String getKind() {
		return kind;
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
	public String getDefaultImage() {
		return "fairy.png";
	}

	/**
	 * @return a z-value for use in determining the top fixture on a tile
	 */
	@SuppressWarnings("MagicNumber")
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
		return (this == obj) ||
					((obj instanceof Fairy) && kind.equals(((Fairy) obj).kind) &&
								(id == ((Fairy) obj).id));
	}

	/**
	 * @return a hash value for the object
	 */
	@Override
	public int hashCode() {
		return id;
	}

	/**
	 * ID number.
	 */
	private final int id;

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
	@SuppressWarnings("CastToConcreteClass")
	@Override
	public boolean equalsIgnoringID(final IFixture fix) {
		return (fix instanceof Fairy) && ((Fairy) fix).kind.equals(kind);
	}

	/**
	 * @param obj     another UnitMember
	 * @param ostream a stream to report an explanation on
	 * @param context a string to print before every line of output, describing the
	 *                context
	 * @return whether that member equals this one
	 * @throws IOException on I/O error writing output to the stream
	 */
	@SuppressWarnings("CastToConcreteClass")
	@Override
	public boolean isSubset(final IFixture obj, final Appendable ostream,
							final String context) throws IOException {
		if (obj.getID() == id) {
			if (obj instanceof Fairy) {
				return areObjectsEqual(ostream, kind, ((Fairy) obj).kind, context,
						"\tDifferent kinds of fairy for ID #", Integer.toString(id),
						LineEnd.LINE_SEP);
			} else {
				ostream.append(context);
				ostream.append("\tFor ID #");
				ostream.append(Integer.toString(id));
				ostream.append(", different kinds of members");
				return false;
			}
		} else {
			ostream.append(context);
			ostream.append("Called with different IDs, #");
			ostream.append(Integer.toString(id));
			ostream.append(" and #");
			ostream.append(Integer.toString(obj.getID()));
			return false;
		}
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
	 * @return a string describing all fairies as a class
	 */
	@Override
	public String plural() {
		return "Fairies";
	}

	/**
	 * @return a short description of the fixture
	 */
	@Override
	public String shortDesc() {
		return toString();
	}
}
