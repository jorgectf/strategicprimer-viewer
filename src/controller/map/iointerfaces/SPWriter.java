package controller.map.iointerfaces;

import java.io.IOException;
import java.nio.file.Path;
import javax.xml.stream.XMLStreamException;
import model.map.IMapNG;

/**
 * An interface for map (and other SP XML) writers.
 *
 * This is part of the Strategic Primer assistive programs suite developed by Jonathan
 * Lovelace.
 *
 * Copyright (C) 2013-2015 Jonathan Lovelace
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
public interface SPWriter {
	/**
	 * Write a map.
	 *
	 * @param file the file to write to
	 * @param map  the map to write.
	 * @throws IOException on error opening the file
	 * @throws XMLStreamException on error creating the XML to write to the file
	 */
	void write(Path file, IMapNG map) throws IOException, XMLStreamException;

	/**
	 * Write a map.
	 *
	 * @param ostream the writer to write to
	 * @param map     the map to write
	 * @throws IOException on error in writing
	 * @throws XMLStreamException on error creating the XML to write to the file
	 */
	void write(Appendable ostream, IMapNG map) throws IOException, XMLStreamException;

}
