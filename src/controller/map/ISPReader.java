package controller.map;

import java.io.Reader;

import javax.xml.stream.XMLStreamException;

import util.Warning;
/**
 * An interface for readers of any SP model type.
 * @author Jonathan Lovelace
 *
 */
public interface ISPReader {
	/**
	 * Tags we expect to use in the future; they are skipped for now and
	 * we'll warn if they're used.
	 */
	String[] FUTURE = { "future", "worker", "explorer",
			"building", "resource", "animal", "changeset", "change",
			"move", "work", "discover" };
	/**
	 * @param <T> The type of the object the XML represents
	 * @param istream
	 *            a reader from which to read the XML
	 * @param type The type of the object the XML represents
	 * @param warner a Warning instance to use for warnings
	 * @return the object contained in that stream
	 * @throws XMLStreamException
	 *             if XML isn't well-formed.
	 * @throws SPFormatException
	 *             if the data is invalid.
	 */
	<T> T readXML(final Reader istream, final Class<T> type,
			final Warning warner) throws XMLStreamException, SPFormatException;

	/**
	 * @param <T> The type of the object the XML represents
	 * @param istream
	 *            a reader from which to read the XML
	 * @param type The type of the object the XML represents
	 * @param warner a Warning instance to use for warnings
	 * @param reflection In a reader that may or may not use reflection, whether to use it. May be ignored if irrelevant.
	 * @return the object contained in that stream
	 * @throws XMLStreamException
	 *             if XML isn't well-formed.
	 * @throws SPFormatException
	 *             if the data is invalid.
	 */
	<T> T readXML(final Reader istream, final Class<T> type,
			final boolean reflection, final Warning warner)
			throws XMLStreamException, SPFormatException;

}
