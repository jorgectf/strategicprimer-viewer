package controller.map.readerng;

import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import model.map.PlayerCollection;
import model.map.fixtures.Meadow;
import controller.map.SPFormatException;
import controller.map.UnwantedChildException;
/**
 * A reader for Meadows.
 * @author Jonathan Lovelace
 *
 */
public class MeadowReader implements INodeReader<Meadow> {
	/**
	 * @return the class this produces
	 */
	@Override
	public Class<Meadow> represents() {
		return Meadow.class;
	}
	/**
	 * Parse a meadow.
	 * @param element the element to read from
	 * @param stream the stream to read more elements from
	 * @param players the collection of players
	 * @return the meadow represented by the element
	 * @throws SPFormatException on SP format error
	 */
	@Override
	public Meadow parse(final StartElement element,
			final Iterable<XMLEvent> stream, final PlayerCollection players)
			throws SPFormatException {
		final Meadow fix = new Meadow(XMLHelper.getAttribute(
				element, "kind"), "field".equalsIgnoreCase(element
				.getName().getLocalPart()), Boolean.parseBoolean(XMLHelper
				.getAttribute(element, "cultivated")));
		for (final XMLEvent event : stream) {
			if (event.isStartElement()) {
				throw new UnwantedChildException(element.getName()
						.getLocalPart(), event.asStartElement().getName()
						.getLocalPart(), event.getLocation().getLineNumber());
			} else if (event.isEndElement()
					&& element.getName().equals(event.asEndElement().getName())) {
				break;
			}
		}
		return fix;
	}
}
