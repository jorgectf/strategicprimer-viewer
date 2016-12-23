package util;

import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * A class to count instances of enums.
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
 * @param <T> the enumerated type we're counting
 * @author Jonathan Lovelace
 */
public final class EnumCounter<T extends Enum<T>> {
	/**
	 * The map we use as a backend.
	 */
	private final Map<T, Accumulator> map;

	/**
	 * Constructor.
	 *
	 * @param type the enumerated type we're dealing with
	 */
	public EnumCounter(final Class<T> type) {
		map = new EnumMap<>(type);
	}

	/**
	 * Increment the counter for a value.
	 * @param value a value to increase the counter for
	 */
	private void count(final T value) {
		MultiMapHelper.getMapValue(map, value, key -> new IntHolder(0)).add(1);
	}

	/**
	 * Count the items in a sequence.
	 * @param values a sequence of values to count.
	 */
	@SafeVarargs
	public final void countMany(final T... values) {
		Stream.of(values).forEach(this::count);
	}

	/**
	 * Get the count for a given value.
	 * @param value a value we want a count for
	 * @return the count for the specified value
	 */
	public int getCount(final T value) {
		if (map.containsKey(value)) {
			return map.get(value).getValue();
		} else {
			return 0;
		}
	}

	/**
	 * A trivial toString().
	 * @return a String representation of the object
	 */
	@SuppressWarnings("MethodReturnAlwaysConstant")
	@Override
	public String toString() {
		return "EnumCounter";
	}
}
