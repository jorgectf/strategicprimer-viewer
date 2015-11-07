package model.listeners;

/**
 * An interface for things that can fire notifications of the current player
 * changing.
 *
 * This is part of the Strategic Primer assistive programs suite developed by
 * Jonathan Lovelace.
 *
 * Copyright (C) 2013-2014 Jonathan Lovelace
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
 * this program. If not, see <a href="http://www.gnu.org/licenses/">http://www.gnu.org/licenses/</a>.
 *
 * @author Jonathan Lovelace
 *
 */
public interface PlayerChangeSource {
	/**
	 * Add a PlayerChangeListener.
	 *
	 * @param list the listener to add
	 */
	void addPlayerChangeListener(PlayerChangeListener list);

	/**
	 * Remove a PlayerChangeListener.
	 *
	 * @param list the listener to remove
	 */
	void removePlayerChangeListener(PlayerChangeListener list);
}
