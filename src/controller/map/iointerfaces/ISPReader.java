package controller.map.iointerfaces;

import controller.map.formatexceptions.SPFormatException;
import java.io.Reader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import javax.xml.stream.XMLStreamException;
import util.Warning;

/**
 * An interface for readers of any SP model type.
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
public interface ISPReader {
	/**
	 * Tags we expect to use in the future; they are skipped for now and we'll warn if
	 * they're used.
	 */
	Iterable<String> FUTURE = Collections.unmodifiableList(
			Arrays.asList("future", "explorer", "building", "resource", "changeset",
					"change", "move", "work", "discover", "submap", "futuretag",
					"futureTag"));
	/**
	 * The namespace that SP XML will use.
	 */
	String NAMESPACE = "https://github.com/kingjon3377/strategicprimer-viewer";

	/**
	 * @param <T>     A supertype of the object the XML represents
	 * @param file    the name of the file being read from
	 * @param istream a reader from which to read the XML
	 * @param type    The type of the object the XML represents
	 * @param warner  a Warning instance to use for warnings
	 * @return the object contained in that stream
	 * @throws XMLStreamException if XML isn't well-formed.
	 * @throws SPFormatException  if the data is invalid.
	 */
	<T> T readXML(Path file, Reader istream, Class<T> type, Warning warner)
			throws XMLStreamException, SPFormatException;
}
