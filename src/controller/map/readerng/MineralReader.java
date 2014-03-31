package controller.map.readerng;

import static controller.map.readerng.XMLHelper.getAttribute;
import static controller.map.readerng.XMLHelper.getAttributeWithDeprecatedForm;
import static controller.map.readerng.XMLHelper.getOrGenerateID;
import static controller.map.readerng.XMLHelper.spinUntilEnd;

import java.util.Collections;
import java.util.List;

import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import model.map.IPlayerCollection;
import model.map.fixtures.resources.MineralVein;
import util.NullCleaner;
import util.Pair;
import util.Warning;
import controller.map.formatexceptions.SPFormatException;
import controller.map.misc.IDFactory;

/**
 * A reader for Minerals.
 *
 * @author Jonathan Lovelace
 * @deprecated ReaderNG is deprecated
 */
@Deprecated
public class MineralReader implements INodeHandler<MineralVein> {
	/**
	 * Parse a Mineral.
	 *
	 * @param element the element to read from
	 * @param stream a stream of more elements
	 * @param players the list of players
	 * @param warner the Warning instance to use for warnings
	 * @param idFactory the factory to use to register ID numbers and generate
	 *        new ones as needed
	 * @return the parsed mineral
	 * @throws SPFormatException on SP format error
	 */
	@Override
	public MineralVein parse(final StartElement element,
			final Iterable<XMLEvent> stream, final IPlayerCollection players,
			final Warning warner, final IDFactory idFactory)
			throws SPFormatException {
		spinUntilEnd(NullCleaner.assertNotNull(element.getName()), stream);
		final MineralVein fix = new MineralVein(
				getAttributeWithDeprecatedForm(element, "kind", "mineral",
						warner),
				Boolean.parseBoolean(getAttribute(element, "exposed")),
				Integer.parseInt(getAttribute(element, "dc")), getOrGenerateID(
						element, warner, idFactory));
		XMLHelper.addImage(element, fix);
		return fix;
	}

	/**
	 * @return a list of the tags this reader understands
	 */
	@Override
	public List<String> understands() {
		return NullCleaner.assertNotNull(Collections.singletonList("mineral"));
	}

	/**
	 * @return the class we know how to write
	 */
	@Override
	public Class<MineralVein> writes() {
		return MineralVein.class;
	}

	/**
	 * Create an intermediate representation to write to a Writer.
	 *
	 * @param obj the object to write
	 * @return an intermediate representation
	 */
	@Override
	public SPIntermediateRepresentation write(final MineralVein obj) {
		final SPIntermediateRepresentation retval =
				new SPIntermediateRepresentation("mineral", Pair.of("kind",
						obj.getKind()), Pair.of("exposed", NullCleaner
						.assertNotNull(Boolean.toString(obj.isExposed()))),
						Pair.of("dc", NullCleaner.assertNotNull(Integer
								.toString(obj.getDC()))));
		retval.addIdAttribute(obj.getID());
		retval.addImageAttribute(obj);
		return retval;
	}

	/**
	 * @return a String representation of the object
	 */
	@Override
	public String toString() {
		return "MineralReader";
	}
}
