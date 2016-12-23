package model.map.fixtures.resources;

import model.map.HasKind;
import model.map.IFixture;
import org.eclipse.jdt.annotation.Nullable;

/**
 * An orchard (fruit trees) or grove (other trees) on the map.
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
public class Grove implements HarvestableFixture, HasKind {
	/**
	 * Whether this is a fruit orchard.
	 */
	private final boolean orchard;
	/**
	 * Whether it's wild (if false) or cultivated.
	 */
	private final boolean cultivated;
	/**
	 * ID number.
	 */
	private final int id;
	/**
	 * The name of an image to use for this particular fixture.
	 */
	private String image = "";
	/**
	 * Kind of tree.
	 */
	private final String kind;

	/**
	 * Constructor.
	 *
	 * @param fruit           whether the trees are fruit trees
	 * @param cultivatedGrove whether the trees are cultivated
	 * @param tree            what kind of trees are in the grove
	 * @param idNum           the ID number.
	 */
	public Grove(final boolean fruit, final boolean cultivatedGrove, final String tree,
				 final int idNum) {
		orchard = fruit;
		cultivated = cultivatedGrove;
		kind = tree;
		id = idNum;
	}

	/**
	 * @param zero ignored, as a grove has no sensitive information
	 * @return a copy of this grove
	 */
	@SuppressWarnings("MethodReturnOfConcreteClass")
	@Override
	public Grove copy(final boolean zero) {
		final Grove retval = new Grove(orchard, cultivated, kind, id);
		retval.image = image;
		return retval;
	}

	/**
	 * @return true if this is an orchard, false otherwise
	 */
	public boolean isOrchard() {
		return orchard;
	}

	/**
	 * @return if this is a cultivated grove or orchard, false if it's a wild one
	 */
	public boolean isCultivated() {
		return cultivated;
	}

	/**
	 * @return what kind of trees are in the grove
	 */
	@Override
	public String getKind() {
		return kind;
	}

	/**
	 * @return the name of an image to represent the grove or orchard
	 */
	@Override
	public String getDefaultImage() {
		if (orchard) {
			return "orchard.png";
		} else {
			return "tree.png";
		}
	}

	/**
	 * @return a String representation of the grove or orchard
	 */
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder(19 + kind.length());
		if (cultivated) {
			builder.append("Cultivated ");
		} else {
			builder.append("Wild ");
		}
		builder.append(kind);
		if (orchard) {
			builder.append(" orchard");
		} else {
			builder.append(" grove");
		}
		return builder.toString();
	}

	/**
	 * @param obj an object
	 * @return whether it's equal to this one
	 */
	@Override
	public boolean equals(@Nullable final Object obj) {
		return (this == obj) ||
					   ((obj instanceof Grove) && kind.equals(((Grove) obj).kind) &&
								(orchard == ((Grove) obj).orchard) &&
								(cultivated == ((Grove) obj).cultivated) &&
								(id == ((Grove) obj).id));
	}

	/**
	 * @return a hash value for the object
	 */
	@Override
	public int hashCode() {
		return id;
	}

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
		return (fix instanceof Grove) && kind.equals(((Grove) fix).kind) &&
					   (orchard == ((Grove) fix).orchard) &&
					   (cultivated == ((Grove) fix).cultivated);
	}

	/**
	 * @return the name of an image to use for this particular fixture.
	 */
	@Override
	public String getImage() {
		return image;
	}

	/**
	 * @param img the name of an image to use for this particular fixture
	 */
	@Override
	public void setImage(final String img) {
		image = img;
	}

	/**
	 * @return a string describing all groves and orchards as a class.
	 */
	@Override
	public String plural() {
		return "Groves and orchards";
	}

	/**
	 * @return a short description of the fixture
	 */
	@Override
	public String shortDesc() {
		return toString();
	}
}
