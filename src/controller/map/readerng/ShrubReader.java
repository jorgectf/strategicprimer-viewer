package controller.map.readerng;

import java.util.Collections;
import java.util.List;

import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import model.map.PlayerCollection;
import model.map.fixtures.Shrub;
import util.Warning;
import controller.map.MissingParameterException;
import controller.map.SPFormatException;
import controller.map.misc.IDFactory;
/**
 * A reader for Shrubs.
 * @author Jonathan Lovelace
 *
 */
public class ShrubReader implements INodeReader<Shrub> {
	/**
	 * Parse a shrub.
	 * @param element the element to read from
	 * @param stream the stream to read more elements from
	 * @param players the collection of players
	 * @param warner the Warning instance to use for warnings
	 * @return the shrub represented by the element
	 * @throws SPFormatException on SP format error
	 */
	@Override
	public Shrub parse(final StartElement element,
			final Iterable<XMLEvent> stream, final PlayerCollection players, final Warning warner)
			throws SPFormatException {
		// ESCA-JAVA0177:
		long id; // NOPMD
		if (XMLHelper.hasAttribute(element, "id")) {
			id = IDFactory.FACTORY.register(
					Long.parseLong(XMLHelper.getAttribute(element, "id")));
		} else {
			warner.warn(new MissingParameterException(element.getName()
					.getLocalPart(), "id", element.getLocation()
					.getLineNumber()));
			id = IDFactory.FACTORY.getID();
		}
		final Shrub fix = new Shrub(XMLHelper.getAttributeWithDeprecatedForm(
				element, "kind", "shrub", warner), id);
		XMLHelper.spinUntilEnd(element.getName(), stream);
		return fix;
	}
	/**
	 * @return a list of the tags this reader understands
	 */
	@Override
	public List<String> understands() {
		return Collections.singletonList("shrub");
	}

}
