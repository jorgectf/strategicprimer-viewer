package controller.map.formatexceptions;

import javax.xml.namespace.QName;
import javax.xml.stream.events.StartElement;

import util.NullCleaner;

/**
 * A custom exception for cases where a tag has a property it doesn't support.
 *
 * This is part of the Strategic Primer assistive programs suite developed by Jonathan
 * Lovelace.
 *
 * Copyright (C) 2013-2014 Jonathan Lovelace
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of version 3 of the GNU General Public License as published by the Free Software
 * Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this
 * program. If not, see
 * <a href="http://www.gnu.org/licenses/">http://www.gnu.org/licenses/</a>.
 *
 * @author Jonathan Lovelace
 */
public final class UnsupportedPropertyException extends SPFormatException {

	/**
	 * The current tag.
	 */
	private final QName context;
	/**
	 * The unsupported parameter.
	 */
	private final String param;

	/**
	 * @param tag       the current tag
	 * @param parameter the unsupported parameter
	 */
	public UnsupportedPropertyException(final StartElement tag,
	                                    final String parameter) {
		super("Unsupported property " + parameter + " in tag "
				+ tag.getName().getLocalPart(),
				NullCleaner.assertNotNull(tag.getLocation()));
		context = NullCleaner.assertNotNull(tag.getName());
		param = parameter;
	}
	/**
	 * @return the current tag
	 */
	public QName getTag() {
		return context;
	}

	/**
	 * @return the unsupported parameter
	 */
	public String getParam() {
		return param;
	}
}
