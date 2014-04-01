package controller.map.cxml;

import static java.util.Collections.unmodifiableList;
import static util.NullCleaner.assertNotNull;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import model.map.IMutableTile;
import model.map.IPlayerCollection;
import model.map.ITile;
import model.map.Point;
import model.map.River;
import model.map.Tile;
import model.map.TileFixture;
import model.map.TileType;
import model.map.fixtures.RiverFixture;
import model.map.fixtures.TextFixture;

import org.eclipse.jdt.annotation.Nullable;

import util.IteratorWrapper;
import util.Warning;
import controller.map.formatexceptions.SPFormatException;
import controller.map.formatexceptions.UnwantedChildException;
import controller.map.misc.IDFactory;

/**
 * A reader for tiles, including rivers.
 *
 * @author Jonathan Lovelace
 *
 */
public final class CompactTileReader extends AbstractCompactReader<ITile> {
	/**
	 * List of readers we'll try subtags on.
	 */
	private final List<CompactReader<? extends TileFixture>> readers =
			assertNotNull(unmodifiableList(new ArrayList<CompactReader<? extends TileFixture>>(
					Arrays.asList(CompactMobileReader.READER,
							CompactResourceReader.READER,
							CompactTerrainReader.READER,
							CompactTextReader.READER, CompactTownReader.READER,
							CompactGroundReader.READER))));

	/**
	 * Singleton object.
	 */
	public static final CompactTileReader READER = new CompactTileReader();

	/**
	 * Singleton.
	 */
	private CompactTileReader() {
		// Do nothing.
	}

	/**
	 *
	 * @param element the XML element to parse
	 * @param stream the stream to read more elements from
	 * @param players the collection of players
	 * @param warner the Warning instance to use for warnings
	 * @param idFactory the ID factory to use to generate IDs
	 * @return the parsed tile
	 * @throws SPFormatException on SP format problem
	 */
	@Override
	public IMutableTile read(final StartElement element,
			final IteratorWrapper<XMLEvent> stream,
			final IPlayerCollection players, final Warning warner,
			final IDFactory idFactory) throws SPFormatException {
		final IMutableTile retval = new Tile(
				TileType.getTileType(getParamWithDeprecatedForm(element,
						"kind", "type", warner)));
		for (final XMLEvent event : stream) {
			if (event.isStartElement()) {
				if (isRiver(assertNotNullQName(event.asStartElement().getName()))) {
					retval.addFixture(new RiverFixture(parseRiver(// NOPMD
							assertNotNullStartElement(event.asStartElement()),
							warner)));
					spinUntilEnd(assertNotNullQName(event.asStartElement()
							.getName()), stream);
				} else {
					retval.addFixture(parseFixture(
							assertNotNullStartElement(event.asStartElement()),
							stream, players, idFactory, warner));
				}
			} else if (event.isCharacters()) {
				final String text = event.asCharacters().getData().trim();
				if (text != null && !text.isEmpty()) {
					warner.warn(new UnwantedChildException("tile", // NOPMD
							"arbitrary text", event.getLocation()
									.getLineNumber()));
					retval.addFixture(new TextFixture(text, -1)); // NOPMD
				}
			} else if (event.isEndElement()
					&& element.getName().equals(event.asEndElement().getName())) {
				break;
			}
		}
		return retval;
	}

	/**
	 * Parse what should be a TileFixture from the XML.
	 *
	 * @param element the XML element to parse
	 * @param stream the stream to read more elements from
	 * @param players the collection of players
	 * @param idFactory the ID factory to generate IDs with
	 * @param warner the Warning instance to use for warnings
	 * @return the parsed fixture.
	 * @throws SPFormatException on SP format problem
	 */
	private TileFixture parseFixture(final StartElement element,
			final IteratorWrapper<XMLEvent> stream,
			final IPlayerCollection players, final IDFactory idFactory,
			final Warning warner) throws SPFormatException {
		final String name =
				assertNotNull(element.getName().getLocalPart());
		for (final CompactReader<? extends TileFixture> item : readers) {
			if (item.isSupportedTag(name)) {
				return item.read(element, stream, players, warner, idFactory);
			}
		}
		throw new UnwantedChildException("tile", name, element.getLocation()
				.getLineNumber());
	}

	/**
	 * @param name the name associated with an element
	 * @return whether it represents a river.
	 */
	private static boolean isRiver(final QName name) {
		return "river".equalsIgnoreCase(name.getLocalPart())
				|| "lake".equalsIgnoreCase(name.getLocalPart());
	}

	/**
	 * Parse a river from XML. The caller is now responsible for getting past
	 * the closing tag.
	 *
	 * @param element the element to parse
	 * @param warner the Warning instance to use as needed
	 * @return the parsed river
	 * @throws SPFormatException on SP format problem
	 */
	public static River parseRiver(final StartElement element,
			final Warning warner) throws SPFormatException {
		requireTag(element, "river", "lake");
		if ("lake".equalsIgnoreCase(element.getName().getLocalPart())) {
			return River.Lake; // NOPMD
		} else {
			requireNonEmptyParameter(element, "direction", true, warner);
			return River.getRiver(getParameter(element, "direction"));
		}
	}

	/**
	 * @param tag a tag
	 * @return whether it's one we can read
	 */
	@Override
	public boolean isSupportedTag(@Nullable final String tag) {
		return "tile".equalsIgnoreCase(tag);
	}

	/**
	 * Write an object to a stream.
	 *
	 * @param ostream The stream to write to.
	 * @param obj The object to write.
	 * @param indent The current indentation level.
	 * @throws IOException on I/O error
	 */
	@Override
	public void write(final Writer ostream, final ITile obj, final int indent)
			throws IOException {
		throw new IllegalStateException(
				"Don't call this; call writeTile() instead");
	}

	/**
	 * Write a tile to a stream.
	 *
	 * @param ostream the stream to write to
	 * @param obj the tile to write
	 * @param point the location of the tile
	 * @param indent the current indentation level
	 * @throws IOException on I/O error
	 */
	public static void writeTile(final Writer ostream, final Point point,
			final ITile obj, final int indent) throws IOException {
		if (!obj.isEmpty()) {
			ostream.append(indent(indent));
			ostream.append("<tile row=\"");
			ostream.append(Integer.toString(point.row));
			ostream.append("\" column=\"");
			ostream.append(Integer.toString(point.col));
			if (!TileType.NotVisible.equals(obj.getTerrain())) {
				ostream.append("\" kind=\"");
				ostream.append(obj.getTerrain().toXML());
			}
			ostream.append("\">");
			if (obj.iterator().hasNext()) {
				ostream.append('\n');
				for (final TileFixture fix : obj) {
					if (fix != null) {
						CompactReaderAdapter.write(ostream, fix, indent + 1);
					}
				}
				ostream.append(indent(indent));
			}
			ostream.append("</tile>\n");
		}
	}

	/**
	 * Write a river.
	 *
	 * @param ostream the stream we're writing to
	 * @param obj the river to write
	 * @param indent the indentation level
	 * @throws IOException on I/O error
	 */
	public static void writeRiver(final Writer ostream, final River obj,
			final int indent) throws IOException {
		for (int i = 0; i < indent; i++) {
			ostream.append('\t');
		}
		if (River.Lake.equals(obj)) {
			ostream.append("<lake />");
		} else {
			ostream.append("<river direction=\"");
			ostream.append(obj.getDescription());
			ostream.append("\" />");
		}
		ostream.append('\n');
	}

	/**
	 * Write a series of rivers.
	 *
	 * @param ostream the stream to write to
	 * @param iter a series of rivers to write
	 * @param indent the indentation level
	 * @throws IOException on I/O error
	 */
	public static void writeRivers(final Writer ostream,
			final Iterable<River> iter, final int indent) throws IOException {
		for (final River river : iter) {
			if (river != null) {
				writeRiver(ostream, river, indent);
			}
		}
	}
	/**
	 * @return a String representation of the object
	 */
	@Override
	public String toString() {
		return "CompactTileReader";
	}
}
