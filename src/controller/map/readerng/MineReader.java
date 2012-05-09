package controller.map.readerng;

import static controller.map.readerng.XMLHelper.getOrGenerateID;

import java.util.Collections;
import java.util.List;

import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import model.map.PlayerCollection;
import model.map.events.TownStatus;
import model.map.fixtures.Mine;
import util.Warning;
import controller.map.SPFormatException;
import controller.map.misc.IDFactory;

/**
 * A reader for Mines.
 * @author Jonathan Lovelace
 *
 */
public class MineReader implements INodeReader<Mine> {
	/**
	 * Parse a mine.
	 * @param element the element to read from
	 * @param stream the stream to read more elements from
	 * @param players the collection of players
	 * @param warner the Warning instance to use for warnings
	 * @param idFactory the factory to use to register ID numbers and generate new ones as needed
	 * @return the mine represented by the element
	 * @throws SPFormatException on SP format error
	 */
	@Override
	public Mine parse(final StartElement element,
			final Iterable<XMLEvent> stream, final PlayerCollection players,
			final Warning warner, final IDFactory idFactory) throws SPFormatException {
		final Mine fix = new Mine(XMLHelper.getAttributeWithDeprecatedForm(
				element, "kind", "product", warner),
				TownStatus.parseTownStatus(XMLHelper.getAttribute(element,
						"status")), getOrGenerateID(element, warner, idFactory));
		XMLHelper.spinUntilEnd(element.getName(), stream);
		return fix;
	}
	/**
	 * @return a list of the tags this reader understands
	 */
	@Override
	public List<String> understands() {
		return Collections.singletonList("mine");
	}
}
