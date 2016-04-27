package model.map.fixtures.mobile;

import java.io.IOException;

import org.eclipse.jdt.annotation.Nullable;

import model.map.HasMutableImage;
import model.map.IFixture;
import model.map.TileFixture;
import model.map.fixtures.UnitMember;

/**
 * A djinn. TODO: should probably be a unit, or something.
 *
 * This is part of the Strategic Primer assistive programs suite developed by Jonathan
 * Lovelace.
 *
 * Copyright (C) 2012-2015 Jonathan Lovelace
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of version 3 of the GNU General Public License as published by the Free Software
 * Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this
 * program. If not, see
 * <a href="http://www.gnu.org/licenses/">http://www.gnu.org/licenses/</a>.
 *
 * @author Jonathan Lovelace
 */
public class Djinn implements MobileFixture, HasMutableImage, UnitMember {
	/**
	 * The name of an image to use for this particular fixture.
	 */
	private String image = "";

	/**
	 * @param idNum the ID number.
	 */
	public Djinn(final int idNum) {
		id = idNum;
	}

	/**
	 * @param zero ignored, as a djinn has no sensitive information
	 * @return a copy of this djinn
	 */
	@Override
	public Djinn copy(final boolean zero) {
		final Djinn retval = new Djinn(id);
		retval.image = image;
		return retval;
	}

	/**
	 * @return a String representation of the djinn
	 */
	@SuppressWarnings("MethodReturnAlwaysConstant")
	@Override
	public String toString() {
		return "djinn";
	}

	/**
	 * @return the name of an image to represent the djinn
	 */
	@Override
	public String getDefaultImage() {
		return "djinn.png";
	}

	/**
	 * @return a z-value for use in determining the top fixture on a tile
	 */
	@SuppressWarnings("MagicNumber")
	@Override
	public int getZValue() {
		return 45;
	}

	/**
	 * @param obj an object
	 * @return whether it's equal to this one
	 */
	@Override
	public boolean equals(@Nullable final Object obj) {
		return (this == obj) || ((obj instanceof Djinn)
				                         && (((TileFixture) obj).getID() == id));
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
		return fix instanceof Djinn;
	}

	/**
	 * @param obj     another UnitMember
	 * @param ostream a stream to report an explanation on
	 * @param context a string to print before every line of output, describing the
	 *                context
	 * @return whether that member equals this one
	 * @throws IOException on I/O error writing output to the stream
	 */
	@Override
	public boolean isSubset(final IFixture obj, final Appendable ostream,
	                        final String context) throws IOException {
		if (obj.getID() == id) {
			return isConditionTrue(ostream, obj instanceof Djinn, context, "\tFor ID #",
					Integer.toString(id), ", different kinds of members\n");
		} else {
			ostream.append(context);
			ostream.append("\tCalled with different IDs, #");
			ostream.append(Integer.toString(id));
			ostream.append(" and #");
			ostream.append(Integer.toString(obj.getID()));
			ostream.append('\n');
			return false;
		}
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
	 * @return a string describing all djinni as a class
	 */
	@Override
	public String plural() {
		return "Djinni";
	}

	/**
	 * @return a short description of the fixture
	 */
	@Override
	public String shortDesc() {
		return "a djinn";
	}
}
