package controller.map.stax;

import javax.xml.namespace.QName;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * A class for helper methods we'll want to share between MapReader, TileReader,
 * and any other such classes.
 * 
 * @author kingjon
 * 
 */
@Deprecated
public class XMLHelper {
	/**
	 * Error message for unexpected tag.
	 */
	private static final String UNEXPECTED_TAG = "Unexpected tag ";
	/**
	 * @param elem the element
	 * @param attr the attribute we want
	 * @param defaultValue the default value if the element doesn't have the attribute
	 * @return the value of attribute if it exists, or the default
	 */
	public String getAttributeWithDefault(final StartElement elem,
			final String attr, final String defaultValue) {
		return (elem.getAttributeByName(new QName(attr)) == null) ? defaultValue
				: getAttribute(elem, attr);
	}
	/**
	 * Move along the stream until we hit an end element, but object to any
	 * start elements.
	 * 
	 * @param tag
	 *            what kind of tag we're in (for the error message)
	 * @param reader
	 *            the XML stream we're reading from
	 */
	public void spinUntilEnd(final String tag, final Iterable<XMLEvent> reader) {
		for (XMLEvent event : reader) {
			if (event.isStartElement()) {
				throw new IllegalStateException(UNEXPECTED_TAG
						+ event
								.asStartElement().getName().getLocalPart()
						+ ": " + tag + " can't contain anything yet");
			} else if (event.isEndElement()) {
				break;
			}
		}
	}
	/**
	 * @param startElement
	 *            a tag
	 * @param attribute
	 *            the attribute we want
	 * @return the value of that attribute.
	 */
	public String getAttribute(final StartElement startElement,
			final String attribute) {
		final Attribute attr = startElement.getAttributeByName(new QName(
				attribute));
		if (attr == null) {
			throw new IllegalArgumentException(
					"Element doesn't contain that attribute");
		}
		return attr.getValue();
	}
	/**
	 * Get the tag type of a tag.
	 * 
	 * @param startElement
	 *            the tag to identify
	 * @return the type of tag, in usable (enumerated) form.
	 */
	// ESCA-JAVA0130:
	public static Tag getTagType(final StartElement startElement) { // NOPMD
		return Tag.fromString(startElement.getName().getLocalPart());
	}
	/**
	 * @return a String representation of the object
	 */
	@Override
	public String toString() {
		return "XMLHelper";
	}
}
