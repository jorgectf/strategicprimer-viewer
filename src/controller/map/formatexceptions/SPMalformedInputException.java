package controller.map.formatexceptions;

import javax.xml.stream.Location;

/**
 * For cases of malformed input where we can't use XMLStreamException.
 *
 * This is part of the Strategic Primer assistive programs suite developed by Jonathan
 * Lovelace.
 *
 * Copyright (C) 2014-2016 Jonathan Lovelace
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of version 3 of the GNU General Public License as published by the Free Software
 * Foundation; see COPYING or
 * <a href="http://www.gnu.org/licenses/">http://www.gnu.org/licenses/</a>.
 *
 * @author Jonathan Lovelace
 */
public final class SPMalformedInputException extends SPFormatException {
	/**
	 * Constructor.
	 *
	 * @param location where this occurred
	 */
	public SPMalformedInputException(final Location location) {
		super("Malformed input", location);
	}

	/**
	 * @param location  where this occurred
	 * @param cause     the underlying exception
	 */
	public SPMalformedInputException(final Location location, final Throwable cause) {
		super("Malformed input", location, cause);
	}
}
