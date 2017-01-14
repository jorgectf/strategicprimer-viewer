package model.map;

import java.util.Formatter;
import java.util.Objects;
import org.eclipse.jdt.annotation.Nullable;


/**
 * An interface to let us check converted player maps against the main map.
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
 * @param <T> The type itself.
 * @author Jonathan Lovelace
 */
@FunctionalInterface
public interface Subsettable<T> {
	/**
	 * Test whether an object is a strict subset of this one.
	 * @param obj     an object
	 * @param ostream the stream to write details to
	 * @param context a string to print before every line of output, describing the
	 *                context; it should be passed through and appended to. Whenever
	 *                it is put onto ostream, it should probably be followed by a tab.
	 * @return whether it is a strict subset of this object---with no members that aren't
	 * also in this.
	 */
	boolean isSubset(T obj, Formatter ostream, String context);

	/**
	 * A helper method to compare two items and, if they're not equal, report this to
	 * the stream.
	 *
	 * @param ostream  the stream to write to
	 * @param message  message (format string) to write if the two aren't equal
	 * @param parameters	parameters to use with the format string
	 * @param first    the first item
	 * @param second   the second item
	 * @return whether the two items are equal
	 */
	default boolean areObjectsEqual(final Formatter ostream,
									@Nullable final Object first,
									@Nullable final Object second,
									final String message, final Object... parameters) {
		if (Objects.equals(first, second)) {
			return true;
		} else {
			ostream.format(message, parameters);
			return false;
		}
	}

	/**
	 * A helper method to compare two items and, if they're not equal, report this to
	 * the stream.
	 *
	 * @param ostream  the stream to write to
	 * @param message  message (format string) to write if the two aren't equal
	 * @param parameters	parameters to use with the format string
	 * @param first    the first item
	 * @param second   the second item
	 * @return whether the two items are equal
	 */
	default boolean areItemsEqual(final Formatter ostream, final boolean first,
								  final boolean second, final String message,
								  final Object... parameters) {
		if (first == second) {
			return true;
		} else {
			ostream.format(message, parameters);
			return false;
		}
	}

	/**
	 * A helper method to compare two items and, if they're not equal, report this to
	 * the stream.
	 *
	 * @param ostream  the stream to write to
	 * @param message  message (format string) to write if the two aren't equal
	 * @param parameters	parameters to use with the format string
	 * @param first    the first item
	 * @param second   the second item
	 * @return whether the two items are equal
	 */
	default boolean areIntItemsEqual(final Formatter ostream, final int first,
									 final int second, final String message,
									 final Object... parameters) {
		if (first == second) {
			return true;
		} else {
			ostream.format(message, parameters);
			return false;
		}
	}

	/**
	 * A helper method to report a message to the stream if a condition isn't true.
	 *
	 * @param ostream   the stream to write to
	 * @param condition the condition to check
	 * @param message  message (format string) to write if the two aren't equal
	 * @param parameters	parameters to use with the format string
	 * @return whether it's true
	 */
	default boolean isConditionTrue(final Formatter ostream, final boolean condition,
									final String message, final Object... parameters) {
		if (condition) {
			return true;
		} else {
			ostream.format(message, parameters);
			return false;
		}
	}
}
