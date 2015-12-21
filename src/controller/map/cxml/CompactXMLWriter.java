package controller.map.cxml;

import controller.map.iointerfaces.SPWriter;
import model.map.IMapNG;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * CompactXML's Writer implementation.
 *
 * This is part of the Strategic Primer assistive programs suite developed by Jonathan
 * Lovelace.
 *
 * Copyright (C) 2012-2015 Jonathan Lovelace
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
public final class CompactXMLWriter implements SPWriter {
	/**
	 * Write a map to file.
	 *
	 * @param file The file to write to
	 * @param map  the map to write
	 * @throws IOException on I/O error
	 */
	@Override
	public void write(final File file, final IMapNG map) throws IOException {
		writeSPObject(file, map);
	}

	/**
	 * Write a map to a stream.
	 *
	 * @param ostream the stream to write to
	 * @param map     the map to write
	 * @throws IOException on I/O error
	 */
	@Override
	public void write(final Appendable ostream, final IMapNG map)
			throws IOException {
		writeSPObject(ostream, map);
	}

	/**
	 * Write an object to file.
	 *
	 * @param filename the file to write to
	 * @param obj      the object to write
	 * @throws IOException on I/O error
	 */
	public static void writeObject(final String filename, final Object obj)
			throws IOException {
		writeSPObject(new File(filename), obj);
	}

	/**
	 * Write an object to file.
	 *
	 * @param file the file to write to
	 * @param obj  the object to write
	 * @throws IOException on I/O error
	 */
	public static void writeSPObject(final File file, final Object obj)
			throws IOException {
		try (final Writer writer = new FileWriter(file)) {
			writeSPObject(writer, obj);
		}
	}

	/**
	 * Write an object to a stream.
	 *
	 * @param ostream the stream to write to
	 * @param obj     the object to write
	 * @throws IOException on I/O error
	 */
	public static void writeSPObject(final Appendable ostream, final Object obj)
			throws IOException {
		CompactReaderAdapter.write(ostream, obj, 0);
	}

	/**
	 * @return a String representation of the object
	 */
	@Override
	public String toString() {
		return "CompactXMLWriter";
	}
}
