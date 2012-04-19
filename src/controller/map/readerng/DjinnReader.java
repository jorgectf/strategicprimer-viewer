package controller.map.readerng;

import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import util.Warning;

import model.map.PlayerCollection;
import model.map.fixtures.Djinn;
import controller.map.SPFormatException;
import controller.map.UnwantedChildException;
/**
 * A reader for djinn.
 * @author Jonathan Lovelace
 *
 */
public class DjinnReader implements INodeReader<Djinn> {
	/**
	 * @return the class this produces
	 */
	@Override
	public Class<Djinn> represents() {
		return Djinn.class;
	}
	/**
	 * Parse a djinn.
	 * @param element the element to read from
	 * @param stream the stream to read more elements from
	 * @param players the collection of players
	 * @param warner the Warning instance to use for warnings
	 * @return the djinn represented by the element
	 * @throws SPFormatException on SP format error
	 */
	@Override
	public Djinn parse(final StartElement element,
			final Iterable<XMLEvent> stream, final PlayerCollection players, final Warning warner)
			throws SPFormatException {
		for (final XMLEvent event : stream) {
			if (event.isStartElement()) {
				throw new UnwantedChildException("djinn", event
						.asStartElement().getName().getLocalPart(), event
						.getLocation().getLineNumber());
			} else if (event.isEndElement()
					&& "djinn".equalsIgnoreCase(event.asEndElement().getName()
							.getLocalPart())) {
				break;
			}
		}
		return new Djinn();
	}

}
