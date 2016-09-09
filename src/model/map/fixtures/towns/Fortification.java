package model.map.fixtures.towns;

import model.map.Player;

/**
 * An abandoned, ruined, or burned-out fortification.
 *
 * FIXME: We want this to share a tag, and model code, with Fortress. Maybe an active
 * Fortification is a Fortress, and a non-active Fortress is a Fortification?
 *
 * This is part of the Strategic Primer assistive programs suite developed by Jonathan
 * Lovelace.
 *
 * Copyright (C) 2013-2016 Jonathan Lovelace
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of version 3 of the GNU General Public License as published by the Free Software
 * Foundation; see COPYING or
 * <a href="http://www.gnu.org/licenses/">http://www.gnu.org/licenses/</a>.
 *
 * @author Jonathan Lovelace
 */
@SuppressWarnings("ClassHasNoToStringMethod")
public final class Fortification extends AbstractTown {
	/**
	 * Constructor.
	 *
	 * @param tStatus The status of the fortification
	 * @param tSize   The size of the fortification
	 * @param discoverDC  The DC to discover it.
	 * @param tName   the name of the town, fortress, or city
	 * @param idNum   the ID number.
	 * @param player  the owner of the fortification
	 */
	public Fortification(final TownStatus tStatus, final TownSize tSize,
						final int discoverDC, final String tName, final int idNum,
						final Player player) {
		super(tStatus, tSize, tName, player);
		dc = discoverDC;
		id = idNum;
	}

	/**
	 * TODO: Should we "zero out" the name or owner?
	 *
	 * @param zero whether to zero out the DC
	 * @return a copy of this fortification
	 */
	@SuppressWarnings("MethodReturnOfConcreteClass")
	@Override
	public Fortification copy(final boolean zero) {
		final Fortification retval;
		if (zero) {
			retval = new Fortification(status(), size(), 0, getName(), id, getOwner());
		} else {
			retval = new Fortification(status(), size(), dc, getName(), id, getOwner());
		}
		retval.setImage(getImage());
		return retval;
	}

	/**
	 * The DC to discover the fortification. TODO: Should perhaps be mutable.
	 */
	private final int dc;

	/**
	 * @return the DC to discover the event.
	 */
	@Override
	public int getDC() {
		return dc;
	}

	/**
	 * @return the name of an image to represent the event.
	 */
	@Override
	public String getDefaultImage() {
		return "fortification.png";
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
	 * @return a string describing all fortifications as a class
	 */
	@Override
	public String plural() {
		return "Fortifications";
	}

	/**
	 * @return that this is a fortification
	 */
	@Override
	public String kind() {
		return "fortification";
	}
}
