package controller.map.readerng;

import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import model.map.PlayerCollection;
import model.map.events.MineralEvent;
import controller.map.SPFormatException;
import controller.map.UnwantedChildException;

/**
 * A reader for Minerals.
 * @author Jonathan Lovelace
 *
 */
public class MineralReader implements INodeReader<MineralEvent> {
	/**
	 * @return the type we produce.
	 */
	@Override
	public Class<MineralEvent> represents() {
		return MineralEvent.class;
	}
	/**
	 * Parse a Mineral.
	 * @param element the element to read from
	 * @param stream a stream of more elements
	 * @param players the list of players
	 * @return the parsed mineral
	 * @throws SPFormatException on SP format error
	 */
	@Override
	public MineralEvent parse(final StartElement element,
			final Iterable<XMLEvent> stream, final PlayerCollection players)
			throws SPFormatException {
		final MineralEvent fix = new MineralEvent(XMLHelper.getAttribute(
				element, "kind"), Boolean.parseBoolean(XMLHelper.getAttribute(
				element, "exposed")), Integer.parseInt(XMLHelper.getAttribute(
				element, "dc")));
		for (final XMLEvent event : stream) {
			if (event.isStartElement()) {
				throw new UnwantedChildException("mineral", event
						.asStartElement().getName().getLocalPart(), event
						.getLocation().getLineNumber());
			} else if (event.isEndElement()
					&& "mineral".equalsIgnoreCase(event.asEndElement()
							.getName().getLocalPart())) {
				break;
			}
		}
		return fix;
	}
}
