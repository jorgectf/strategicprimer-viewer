package model.map.fixtures;

import java.io.IOException;
import model.map.HasMutableImage;
import model.map.HasMutableKind;
import model.map.IFixture;
import org.eclipse.jdt.annotation.Nullable;

/**
 * A piece of equipment.
 *
 * This is part of the Strategic Primer assistive programs suite developed by Jonathan
 * Lovelace.
 *
 * Copyright (C) 2013-2015 Jonathan Lovelace
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
 *
 *         TODO: more members
 */
public class Implement
		implements UnitMember, FortressMember, HasMutableKind, HasMutableImage {
	/**
	 * The ID # of the implement.
	 */
	private final int id;
	/**
	 * The "kind" of the implement.
	 */
	private String kind;
	/**
	 * The image to use for the implement.
	 */
	private String image = "";

	/**
	 * @param implKind the "kind" of implement
	 * @param idNum    an ID # for the implement
	 */
	public Implement(final String implKind, final int idNum) {
		id = idNum;
		kind = implKind;
	}

	/**
	 * @return the ID # of the implement
	 */
	@Override
	public int getID() {
		return id;
	}

	/**
	 * @param fix a fixture
	 * @return whether it equals this one except for ID
	 */
	@SuppressWarnings("CastToConcreteClass")
	@Override
	public boolean equalsIgnoringID(final IFixture fix) {
		return (fix instanceof Implement) && kind.equals(((Implement) fix).kind);
	}

	/**
	 * @param ostream the stream to report errors to
	 * @param context the context to report before errors
	 * @param obj     a fixture
	 * @return whether it's a subset of (i.e. equal to) this one
	 * @throws IOException on I/O error writing to ostream
	 */
	@SuppressWarnings("CastToConcreteClass")
	@Override
	public boolean isSubset(final IFixture obj, final Appendable ostream,
							final String context) throws IOException {
		if (obj.getID() != id) {
			ostream.append(context);
			ostream.append("\tIDs differ");
			return false;
		} else if (obj instanceof Implement) {
			return areObjectsEqual(ostream, kind, ((Implement) obj).kind, context,
					"\tIn Implement ID #", Integer.toString(id), ": Kinds differ\n");
		} else {
			ostream.append(context);
			ostream.append("\tDifferent fixture types given for ID #");
			ostream.append(Integer.toString(id));
			return false;
		}
	}

	/**
	 * @param zero whether to "zero out" sensitive information
	 * @return a copy of this Implement
	 */
	@Override
	public Implement copy(final boolean zero) {
		return new Implement(kind, id);
	}

	/**
	 * @return the "kind" of implement
	 */
	@Override
	public String getKind() {
		return kind;
	}

	/**
	 * @param nKind the new "kind" for the implement
	 */
	@Override
	public void setKind(final String nKind) {
		kind = nKind;
	}

	/**
	 * @return the filename of an image to use for implements by default
	 */
	@Override
	public String getDefaultImage() {
		return "implement.png";
	}

	/**
	 * @param img the filename of an image to use for this implement
	 */
	@Override
	public void setImage(final String img) {
		image = img;
	}

	/**
	 * @return the filename of an image to use for this implement
	 */
	@Override
	public String getImage() {
		return image;
	}

	/**
	 * @param obj an object
	 * @return whether it's equal to this one
	 */
	@Override
	public boolean equals(@Nullable final Object obj) {
		return (this == obj) ||
					((obj instanceof Implement) && (((Implement) obj).id == id) &&
								((Implement) obj).kind.equals(kind));
	}

	/**
	 * @return a hash value for this object
	 */
	@Override
	public int hashCode() {
		return id | kind.hashCode();
	}

	/**
	 * @return a String representation of the implement
	 */
	@Override
	public String toString() {
		return "An implement of kind " + kind;
	}
}
