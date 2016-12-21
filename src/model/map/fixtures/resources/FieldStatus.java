package model.map.fixtures.resources;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Possible status of fields (and meadows, and orchards ...) Fields should rotate between
 * these, at a rate determined by the kind of field.
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
public enum FieldStatus {
	/**
	 * Fallow: waiting to be planted, or waiting to start growing.
	 */
	Fallow("fallow"),
	/**
	 * Seeding: being planted, by human or animal activity.
	 */
	Seeding("seeding"),
	/**
	 * Growing.
	 */
	Growing("growing"),
	/**
	 * Bearing: ready to be harvested.
	 */
	Bearing("bearing");
	/**
	 * A mapping from strings to FieldStatus.
	 */
	private static final Map<String, FieldStatus> FST_MAP = new HashMap<>();
	/**
	 * A string representing the FieldStatus.
	 */
	private final String str;

	static {
		for (final FieldStatus status : values()) {
			FST_MAP.put(status.str, status);
		}
	}

	/**
	 * Constructor.
	 *
	 * @param desc a string representing the status.
	 */
	FieldStatus(final String desc) {
		str = desc;
	}

	/**
	 * @param desc a string representing a FieldStatus
	 * @return the FieldStatus it represents
	 */
	public static FieldStatus parse(final String desc) {
		if (FST_MAP.containsKey(desc)) {
			return FST_MAP.get(desc);
		} else {
			throw new IllegalArgumentException("Not a FieldStatus we recognize");
		}
	}

	/**
	 * @param seed a number to use to seed the RNG
	 * @return a random status
	 */
	public static FieldStatus random(final int seed) {
		return values()[new Random(seed).nextInt(values().length)];
	}

	/**
	 * @return a string representation of the status
	 */
	@Override
	public String toString() {
		return str;
	}
}
