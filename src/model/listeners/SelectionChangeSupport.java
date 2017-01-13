package model.listeners;

import java.util.ArrayList;
import java.util.Collection;
import model.map.Point;
import org.eclipse.jdt.annotation.Nullable;

/**
 * A helper class to proxy selection-changing calls.
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
public final class SelectionChangeSupport implements SelectionChangeSource {
	/**
	 * The list of listeners to notify.
	 */
	private final Collection<SelectionChangeListener> listeners = new ArrayList<>();

	/**
	 * Notify the given listener of future selection changes.
	 * @param list a listener to add
	 */
	@Override
	public void addSelectionChangeListener(final SelectionChangeListener list) {
		listeners.add(list);
	}

	/**
	 * Stop notifying the given listener of selection changes.
	 * @param list a listener to remove
	 */
	@Override
	public void removeSelectionChangeListener(final SelectionChangeListener list) {
		listeners.remove(list);
	}

	/**
	 * Tell all listeners about a change. All in one like this rather than implementing
	 * {@link SelectionChangeListener} to prevent accidental infinite recursion.
	 *
	 * @param oldPoint the previously selected location
	 * @param newPoint the newly selected location.
	 */
	public void fireChanges(@Nullable final Point oldPoint, final Point newPoint) {
		for (final SelectionChangeListener list : listeners) {
			list.selectedPointChanged(oldPoint, newPoint);
		}
	}

	/**
	 * A trivial toString().
	 * @return a String representation of the object
	 */
	@SuppressWarnings("MethodReturnAlwaysConstant")
	@Override
	public String toString() {
		return "SelectionChangeSupport";
	}
}
