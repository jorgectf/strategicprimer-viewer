package model.map.fixtures.towns;

import java.util.HashMap;
import java.util.Map;
import util.NullCleaner;

/**
 * Possible status of towns, fortifications, and cities.
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
public enum TownStatus {
	/**
	 * The town is inhabited.
	 */
	Active("active"),
	/**
	 * The town is abandoned.
	 */
	Abandoned("abandoned"),
	/**
	 * The town is burned-out.
	 */
	Burned("burned"),
	/**
	 * The town is in ruins.
	 */
	Ruined("ruined");
	/**
	 * A mapping from strings to TownStatus.
	 */
	private static final Map<String, TownStatus> TST_MAP = new HashMap<>();
	/**
	 * A string representing the TownStatus.
	 */
	private final String str;

	static {
		for (final TownStatus status : values()) {
			TST_MAP.put(status.str, status);
		}
	}

	/**
	 * Constructor.
	 *
	 * @param description a string representing the status.
	 */
	TownStatus(final String description) {
		str = description;
	}

	/**
	 * @param description a string representing a TownStatus
	 * @return the TownStatus it represents
	 */
	public static TownStatus parseTownStatus(final String description) {
		if (TST_MAP.containsKey(description)) {
			return NullCleaner.assertNotNull(TST_MAP.get(description));
		} else {
			throw new IllegalArgumentException("No such town status");
		}
	}

	/**
	 * @return a string representation of the status
	 */
	@Override
	public String toString() {
		return str;
	}
}
