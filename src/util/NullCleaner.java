package util;

import org.eclipse.jdt.annotation.Nullable;

/**
 * A class to remove the "taint" of null from values.
 *
 * This is part of the Strategic Primer assistive programs suite developed by
 * Jonathan Lovelace.
 *
 * Copyright (C) 2014-2014 Jonathan Lovelace
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of version 3 of the GNU General Public License as published by the
 * Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * @author Jonathan Lovelace
 */
public final class NullCleaner {
	/**
	 * Do not instantiate.
	 */
	private NullCleaner() {
		// Static-only class.
	}
	/**
	 * @param <T> the type of thing we're dealing with
	 * @param val a value
	 * @param def a default value
	 * @return val if it isn't null, def if val is null
	 */
	public static <T> T valueOrDefault(@Nullable final T val, final T def) {
		if (val == null) {
			return def;
		} else {
			return val;
		}
	}
	/**
	 * Assert that a value isn't null.
	 * @param <T> the type of the value
	 * @param val the value
	 * @return it, if it isn't null.
	 */
	public static <T> T assertNotNull(@Nullable final T val) {
		assert val != null;
		return val;
	}
	/**
	 * Assert that an array is not nullable.
	 * @param <T> the type of the array
	 * @param array the array
	 * @return it
	 */
	public static <T> T[] assertNotNullArray(final T @Nullable [] array) {
		assert array != null;
		for (@Nullable T item : array) {
			assert item != null;
		}
		return array;
	}
}
