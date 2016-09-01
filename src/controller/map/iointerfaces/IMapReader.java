package controller.map.iointerfaces;

import controller.map.formatexceptions.SPFormatException;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import javax.xml.stream.XMLStreamException;
import model.map.IMutableMapNG;
import util.Warning;

/**
 * An interface for map readers.
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
public interface IMapReader {
	/**
	 * Read the map view contained in a file.
	 *
	 * @param file   the file to read
	 * @param warner the Warning instance to use for warnings.
	 * @return the map view it contains
	 * @throws IOException        if there are other I/O errors, i.e. opening the file
	 * @throws XMLStreamException if there are XML errors
	 * @throws SPFormatException  if the reader can't handle this map version or doesn't
	 *                            recognize the map format
	 */
	IMutableMapNG readMap(Path file, Warning warner)
			throws IOException, XMLStreamException, SPFormatException;

	/**
	 * Read the map contained in a reader.
	 *
	 * @param file    the name of the file the stream represents
	 * @param istream the reader to read from
	 * @param warner  the Warning instance to use for warnings.
	 * @return the map it contains
	 * @throws XMLStreamException if there are XML errors
	 * @throws SPFormatException  if the reader can't handle this map version or doesn't
	 *                            recognize the map format
	 */
	IMutableMapNG readMap(Path file, Reader istream, Warning warner)
			throws XMLStreamException, SPFormatException;
}
