package controller.map.formatexceptions;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import javax.xml.stream.Location;

/**
 * A custom exception for XML format errors.
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
public class SPFormatException extends Exception {
	/**
	 * The line of the XML file containing the mistake.
	 */
	private final Location location;

	/**
	 * @return the line of the XML file containing the mistake
	 */
	public int getLine() {
		return location.getLineNumber();
	}
	/**
	 * @return the location in the XML file containing the mistake
	 */
	public Location getLocation() {
		return location;
	}
	/**
	 * @param message   a message describing what's wrong with the XML
	 * @param errorLoc  the location of the text causing the error
	 * @param cause     the "initial cause" of this
	 */
	protected SPFormatException(final String message, final Location errorLoc,
								final Throwable cause) {
		super("Incorrect SP XML at line " + errorLoc.getLineNumber() + ", column " +
					errorLoc.getColumnNumber() + ": " + message, cause);
		location = errorLoc;
	}
	/**
	 * Constructor.
	 *
	 * @param message   a message describing what's wrong with the XML.
	 * @param errorLoc  the location containing the error.
	 */
	protected SPFormatException(final String message, final Location errorLoc) {
		super("Incorrect SP XML at line " + errorLoc.getLineNumber() + ", column " +
					errorLoc.getColumnNumber() + ": " + message);
		location = errorLoc;
	}
	/**
	 * Prevent serialization.
	 * @param out ignored
	 * @throws IOException always
	 */
	@SuppressWarnings({ "unused", "static-method" })
	private void writeObject(final ObjectOutputStream out) throws IOException {
		throw new NotSerializableException("Serialization is not allowed");
	}
	/**
	 * Prevent serialization
	 * @param in ignored
	 * @throws IOException always
	 * @throws ClassNotFoundException never
	 */
	@SuppressWarnings({ "unused", "static-method" })
	private void readObject(final ObjectInputStream in)
			throws IOException, ClassNotFoundException {
		throw new NotSerializableException("Serialization is not allowed");
	}
}
