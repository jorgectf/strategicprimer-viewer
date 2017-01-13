package model.map;

/**
 * If a tile has a river, it could be in any one of several directions. This class
 * enumerates those. Tiles should have a <em>set</em> of these.
 *
 * At present we'll just cover the four cardinal directions.
 *
 * This is part of the Strategic Primer assistive programs suite developed by Jonathan
 * Lovelace.
 *
 * Copyright (C) 2011-2016 Jonathan Lovelace
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of version 3 of the GNU General Public License as published by the Free Software
 * Foundation; see COPYING or
 * <a href="http://www.gnu.org/licenses/">http://www.gnu.org/licenses/</a>.
 *
 * @author Jonathan Lovelace
 */
public enum River {
	/**
	 * North.
	 */
	North("north"),
	/**
	 * East.
	 */
	East("east"),
	/**
	 * South.
	 */
	South("south"),
	/**
	 * West.
	 */
	West("west"),
	/**
	 * A lake (to be depicted as being in the center of the tile).
	 */
	Lake("lake");
	/**
	 * A descriptive string representing the direction.
	 */
	private final String desc;

	/**
	 * Constructor.
	 *
	 * @param description a descriptive string representing the direction
	 */
	River(final String description) {
		desc = description;
	}

	/**
	 * Parse a river direction.
	 *
	 * @param description a string giving the direction
	 * @return the river direction
	 */
	public static River getRiver(final String description) {
		for (final River river : values()) {
			if (river.desc.equals(description)) {
				return river;
			}
		}
		throw new IllegalArgumentException("Unrecognized river direction string");
	}

	/**
	 * The direction of the river.
	 * @return a description of the direction of the river
	 */
	public String getDescription() {
		return desc;
	}
}
