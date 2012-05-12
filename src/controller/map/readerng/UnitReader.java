package controller.map.readerng;

import static controller.map.readerng.XMLHelper.getAttribute;
import static controller.map.readerng.XMLHelper.getAttributeWithDeprecatedForm;
import static controller.map.readerng.XMLHelper.getOrGenerateID;
import static controller.map.readerng.XMLHelper.requireNonEmptyParameter;
import static controller.map.readerng.XMLHelper.spinUntilEnd;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.List;

import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import model.map.PlayerCollection;
import model.map.fixtures.Unit;
import util.Warning;
import controller.map.MissingParameterException;
import controller.map.SPFormatException;
import controller.map.misc.IDFactory;

/**
 * A reader for Units.
 * 
 * @author Jonathan Lovelace
 * 
 */
public class UnitReader implements INodeHandler<Unit> {
	/**
	 * The name of the property telling what kind of unit.
	 */
	private static final String KIND_PROPERTY = "kind";
	/**
	 * Parse a unit.
	 * 
	 * @param element
	 *            the element to start with
	 * @param stream
	 *            the stream to read more elements from
	 * @param players
	 *            the collection of players
	 * @param warner the Warning instance to use for warnings
	 * @param idFactory the factory to use to register ID numbers and generate new ones as needed
	 * @return the fortress
	 * @throws SPFormatException
	 *             on SP format error
	 */
	@Override
	public Unit parse(final StartElement element,
			final Iterable<XMLEvent> stream, final PlayerCollection players,
			final Warning warner, final IDFactory idFactory) throws SPFormatException {
		requireNonEmptyParameter(element, "owner", false, warner);
		requireNonEmptyParameter(element, "name", false, warner);
		spinUntilEnd(element.getName(), stream);
		return new Unit(players.getPlayer(Integer
				.parseInt(ensureNumeric(getAttribute(
						element, "owner", "-1")))), parseKind(element, warner),
				getAttribute(element, "name", ""),
				getOrGenerateID(element, warner, idFactory));
	}

	/**
	 * Parse the kind of unit, from the "kind" or "type" parameter---default the empty string. 
	 * @param element the current element
	 * @param warner the Warning instance to use
	 * @return the kind of unit
	 * @throws SPFormatException on SP format error.
	 */
	private static String parseKind(final StartElement element, final Warning warner)
			throws SPFormatException {
		String retval = "";
		try {
			retval = getAttributeWithDeprecatedForm(element, // NOPMD
					KIND_PROPERTY, "type", warner);
		} catch (final MissingParameterException except) {
			warner.warn(except);
			return ""; // NOPMD
		}
		if (retval.isEmpty()) {
			warner.warn(new MissingParameterException(element.getName()
					.getLocalPart(), KIND_PROPERTY, element.getLocation()
					.getLineNumber()));
		}
		return retval;
	}
	/**
	 * @param string a string that may be either numeric or empty.
	 * @return it, or "-1" if it's empty.
	 */
	private static String ensureNumeric(final String string) {
		return string.isEmpty() ? "-1" : string;
	}
	/**
	 * @return a list of the tags this reader understands
	 */
	@Override
	public List<String> understands() {
		return Collections.singletonList("unit");
	}

	/**
	 * Write an instance of the type to a Writer.
	 * @param <S> the actual type of the object
	 * @param obj
	 *            the object to write
	 * @param writer
	 *            the Writer we're currently writing to
	 * @param inclusion
	 *            ignored
	 * @throws IOException
	 *             on I/O error while writing
	 */
	@Override
	public <S extends Unit> void write(final S obj, final Writer writer, final boolean inclusion)
			throws IOException {
		writer.append("<unit owner=\"");
		writer.append(Integer.toString(obj.getOwner().getId()));
		if (!obj.getKind().isEmpty()) {
			writer.append("\" kind=\"");
			writer.append(obj.getKind());
		}
		if (!obj.getName().isEmpty()) {
			writer.append("\" name=\"");
			writer.append(obj.getName());
		}
		writer.append("\" id=\"");
		writer.append(Long.toString(obj.getID()));
		writer.append("\" />");
	}
	/**
	 * @return The type we know how to write
	 */
	@Override
	public Class<Unit> writes() {
		return Unit.class;
	}
}
