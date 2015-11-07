package controller.map.formatexceptions;

/**
 * An exception to throw when the map's version is too old.
 *
 * This is part of the Strategic Primer assistive programs suite developed by
 * Jonathan Lovelace.
 *
 * Copyright (C) 2013-2013 Jonathan Lovelace
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
 */
public final class MapVersionException extends SPFormatException {
	/**
	 * Constructor.
	 *
	 * @param message the message to show the user if this isn't caught.
	 */
	public MapVersionException(final String message) {
		super(message, 0);
	}

	/**
	 * Constructor.
	 *
	 * @param message the message to show the user if this isn't caught.
	 * @param line the line of the map tag.
	 */
	public MapVersionException(final String message, final int line) {
		super(message, line);
	}
}
