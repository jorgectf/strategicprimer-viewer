package model.map.fixtures.towns;

import model.map.Player;

/**
 * An abandoned, ruined, or burned-out town.
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
 */
@SuppressWarnings("ClassHasNoToStringMethod")
public final class Town extends AbstractTown {
	/**
	 * Constructor.
	 *
	 * @param tStatus The status of the town
	 * @param tSize   The size of the town
	 * @param discoverDC  The DC to discover it.
	 * @param tName   the name of the town, fortress, or city
	 * @param idNum   the ID number.
	 * @param player  the owner of the town
	 */
	public Town(final TownStatus tStatus, final TownSize tSize, final int discoverDC,
				final String tName, final int idNum, final Player player) {
		super(tStatus, tSize, tName, player);
		dc = discoverDC;
		id = idNum;
	}

	/**
	 * TODO: Should we "zero out" the name or owner?
	 *
	 * @param zero whether to zero out the DC
	 * @return a copy of this town
	 */
	@SuppressWarnings("MethodReturnOfConcreteClass")
	@Override
	public Town copy(final boolean zero) {
		final Town retval;
		if (zero) {
			retval = new Town(status(), size(), 0, getName(), id, getOwner());
		} else {
			retval = new Town(status(), size(), dc, getName(), id, getOwner());
		}
		retval.setImage(getImage());
		return retval;
	}

	/**
	 * The DC to discover the town. TODO: Should perhaps be mutable.
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
	 * @return a string describing all towns as a class
	 */
	@Override
	public String plural() {
		return "Towns";
	}

	/**
	 * @return that this is a town
	 */
	@Override
	public String kind() {
		return "town";
	}
}
