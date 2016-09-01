package controller.map.formatexceptions;

import javax.xml.stream.Location;

/**
 * An exception to throw when an "include" tag references a nonexistent file. We need it
 * because we can't throw FileNotFound from tag-processing functions, only
 * SPFormatExceptions.
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
public final class MissingIncludeException extends SPFormatException {
	/**
	 * Constructor.
	 *
	 * @param file     the missing file
	 * @param cause    the exception that caused this one to be thrown.
	 * @param location the location of the "include" tag.
	 */
	public MissingIncludeException(final String file, final Throwable cause,
								final Location location) {
		super("File " + file + ", referenced by <include> tag on line " +
					location.getLineNumber() + ", does not exist", location, cause);
	}
}
