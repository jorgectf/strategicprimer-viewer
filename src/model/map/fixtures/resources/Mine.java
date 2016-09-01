package model.map.fixtures.resources;

import org.eclipse.jdt.annotation.Nullable;

import model.map.HasMutableKind;
import model.map.IFixture;
import model.map.fixtures.towns.TownStatus;

/**
 * A mine---a source of mineral resources.
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
public class Mine implements HarvestableFixture, HasMutableKind {
	/**
	 * The name of an image to use for this particular fixture.
	 */
	private String image = "";

	/**
	 * What the mine produces.
	 */
	private String kind;
	/**
	 * The status of the mine.
	 */
	private final TownStatus status;

	/**
	 * Constructor.
	 *
	 * @param mineral what mineral this produces
	 * @param stat    the status of the mine
	 * @param idNum   the ID number.
	 */
	public Mine(final String mineral, final TownStatus stat, final int idNum) {
		kind = mineral;
		status = stat;
		id = idNum;
	}

	/**
	 * @param zero ignored; there isn't any sensitive information
	 * @return a copy of this mine
	 */
	@SuppressWarnings("MethodReturnOfConcreteClass")
	@Override
	public Mine copy(final boolean zero) {
		final Mine retval = new Mine(kind, status, id);
		retval.image = image;
		return retval;
	}

	/**
	 * @return what the mine produces
	 */
	@Override
	public String getKind() {
		return kind;
	}

	/**
	 * @return the status of the mine
	 */
	public TownStatus getStatus() {
		return status;
	}

	/**
	 * @return the name of an image to represent the mine
	 */
	@Override
	public String getDefaultImage() {
		return "mine.png";
	}

	/**
	 * @return a string representation of the mine
	 */
	@Override
	public String toString() {
		return status + " mine of " + kind;
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
		return (this == obj) || ((obj instanceof Mine)
										&& kind.equals(((Mine) obj).kind)
										&& (status == ((Mine) obj).status) &&
										(id == ((Mine) obj).id));
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
		return (fix instanceof Mine) && kind.equals(((Mine) fix).kind)
					&& (status == ((Mine) fix).status);
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
	 * @return a string describing all mines as a class
	 */
	@Override
	public String plural() {
		return "Mines";
	}

	/**
	 * @return a short description of the fixture
	 */
	@Override
	public String shortDesc() {
		return status.toString() + ' ' + kind + " mine";
	}
}
