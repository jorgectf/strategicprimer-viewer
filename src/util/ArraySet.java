package util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.Nullable;

/**
 * A Set implementation that's just an ArrayList with Set semantics.
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
 * @param <U> the type of thing stored in the set
 */
public final class ArraySet<U> implements Set<U> {
	/**
	 * The backing array.
	 */
	private final List<U> impl = new ArrayList<>();
	/**
	 * The running total of the hash code.
	 */
	private int hash = 0;

	/**
	 * @return the size of the set
	 */
	@Override
	public int size() {
		return impl.size();
	}

	/**
	 * @return whether the set is empty
	 */
	@Override
	public boolean isEmpty() {
		return impl.isEmpty();
	}

	/**
	 * @param obj an object
	 * @return whether we contain it
	 */
	@SuppressWarnings("ParameterNameDiffersFromOverriddenParameter")
	@Override
	public boolean contains(@Nullable final Object obj) {
		return impl.contains(obj);
	}

	/**
	 * @return an iterator over the set
	 */
	@Override
	public Iterator<U> iterator() {
		return NullCleaner.assertNotNull(impl.iterator());
	}

	/**
	 * @return an array view of the set
	 */
	@Override
	public Object[] toArray() {
		return NullCleaner.assertNotNull(impl.toArray());
	}

	/**
	 * @param <T> the type
	 * @param array  an array of that type
	 * @return the contents of the set in that array
	 */
	@SuppressWarnings(
			{"SuspiciousToArrayCall", "ParameterNameDiffersFromOverriddenParameter"})
	@Override
	public <T> T[] toArray(final T @Nullable [] array) {
		return NullCleaner.assertNotNullArray(impl.toArray(array));
	}

	/**
	 * @param elem an element
	 * @return the result of adding it to the set.
	 */
	@SuppressWarnings("ParameterNameDiffersFromOverriddenParameter")
	@Override
	public boolean add(final U elem) {
		if (contains(elem)) {
			return false;
		} else {
			impl.add(elem);
			hash += Objects.hashCode(elem);
			return true;
		}
	}

	/**
	 * @param obj an object
	 * @return the result of removing it from the set
	 */
	@SuppressWarnings("ParameterNameDiffersFromOverriddenParameter")
	@Override
	public boolean remove(@Nullable final Object obj) {
		final boolean retval = impl.remove(obj);
		if (retval && (obj != null)) {
			hash -= obj.hashCode();
		}
		return retval;
	}

	/**
	 * @param coll a collection
	 * @return whether the set contains all its elements
	 */
	@SuppressWarnings("ParameterNameDiffersFromOverriddenParameter")
	@Override
	public boolean containsAll(@Nullable final Collection<?> coll) {
		return impl.containsAll(NullCleaner.assertNotNull(coll));
	}

	/**
	 * @param coll a collection
	 * @return the result of adding all of them
	 */
	@SuppressWarnings("ParameterNameDiffersFromOverriddenParameter")
	@Override
	public boolean addAll(@Nullable final Collection<? extends U> coll) {
		boolean retval = false;
		if (coll != null) {
			for (final U obj : coll) {
				if (add(obj)) {
					retval = true;
				}
			}
		}
		return retval;
	}

	/**
	 * @param coll a collection
	 * @return the result of removing everything not in it
	 */
	@SuppressWarnings("ParameterNameDiffersFromOverriddenParameter")
	@Override
	public boolean retainAll(@Nullable final Collection<?> coll) {
		final boolean retval = impl.retainAll(coll);
		if (retval) {
			hash = impl.stream().collect(Collectors.summingInt(Object::hashCode))
						.intValue();
		}
		return retval;
	}

	/**
	 * @param coll a collection
	 * @return the result of removing everything in it
	 */
	@SuppressWarnings("ParameterNameDiffersFromOverriddenParameter")
	@Override
	public boolean removeAll(@Nullable final Collection<?> coll) {
		final boolean retval = impl.removeAll(coll);
		if (retval) {
			hash = impl.stream().collect(Collectors.summingInt(Object::hashCode))
						.intValue();
		}
		return retval;
	}

	/**
	 * Empty the set.
	 */
	@Override
	public void clear() {
		impl.clear();
		hash = 0;
	}

	/**
	 * Note that since this is a cached value, computed by adding to the cached value
	 * whenever something is added to the set and subtracting whenever something is
	 * removed, and only recomputed on removeAll and retainAll operations, if a member's
	 * hash value changes after it is added, this will no longer be correct according to
	 * the Set contract. But having a hash code that changes is usually going to cause
	 * bugs with a Set anyway.
	 *
	 * @return a hash-value for the set.
	 */
	@Override
	public int hashCode() {
		return hash;
	}

	/**
	 * Another object is equal to this one if it is a set that contains the same
	 * elements.
	 *
	 * @param obj another object
	 * @return whether it's equal to this
	 */
	@Override
	public boolean equals(@Nullable final Object obj) {
		return (this == obj) || ((obj instanceof Set)
										&& areSetsEqual(this, (Set<?>) obj));
	}

	/**
	 * @param firstSet  one set
	 * @param secondSet another set
	 * @return whether they are equal according to the Set contract.
	 */
	@SuppressWarnings("TypeMayBeWeakened")
	private static boolean areSetsEqual(final Set<?> firstSet, final Set<?> secondSet) {
		return firstSet.containsAll(secondSet) && secondSet.containsAll(firstSet);
	}

	/**
	 * @return a String representation of the object
	 */
	@Override
	public String toString() {
		return "ArraySet " + impl;
	}
}
