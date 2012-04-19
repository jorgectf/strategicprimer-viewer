package controller.map.readerng;

import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import util.Warning;

import model.map.PlayerCollection;
import model.map.fixtures.Fairy;
import controller.map.SPFormatException;
import controller.map.UnwantedChildException;
/**
 * A reader for Fairies.
 * @author Jonathan Lovelace
 *
 */
public class FairyReader implements INodeReader<Fairy> {
	/**
	 * @return the class this produces
	 */
	@Override
	public Class<Fairy> represents() {
		return Fairy.class;
	}
	/**
	 * Parse a fairy.
	 * @param element the element to read from
	 * @param stream the stream to read more elements from
	 * @param players the collection of players
	 * @return the fairy represented by the element
	 * @param warner the Warning instance to use for warnings
	 * @throws SPFormatException on SP format error
	 */
	@Override
	public Fairy parse(final StartElement element,
			final Iterable<XMLEvent> stream, final PlayerCollection players, final Warning warner)
			throws SPFormatException {
		final Fairy fix = new Fairy(XMLHelper.getAttribute(element, "kind"));
		for (final XMLEvent event : stream) {
			if (event.isStartElement()) {
				throw new UnwantedChildException("fairy", event
						.asStartElement().getName().getLocalPart(), event
						.getLocation().getLineNumber());
			} else if (event.isEndElement()
					&& "fairy".equalsIgnoreCase(event.asEndElement().getName()
							.getLocalPart())) {
				break;
			}
		}
		return fix;
	}

}
