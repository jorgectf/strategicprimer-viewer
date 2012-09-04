package controller.map.cxml;

import java.io.IOException;
import java.io.Writer;

import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import model.map.IMap;
import model.map.MapView;
import model.map.Player;
import model.map.PlayerCollection;
import model.map.PointFactory;
import model.map.SPMap;
import model.map.Tile;
import util.EqualsAny;
import util.IteratorWrapper;
import util.Warning;
import controller.map.ISPReader;
import controller.map.MissingChildException;
import controller.map.SPFormatException;
import controller.map.UnsupportedTagException;
import controller.map.UnwantedChildException;
import controller.map.misc.IDFactory;

/**
 * A reader for maps.
 * @author Jonathan Lovelace
 *
 */
public final class CompactMapReader extends CompactReaderSuperclass implements CompactReader<IMap> {
	/**
	 * The 'map' tag.
	 */
	private static final String MAP_TAG = "map";
	/**
	 * Read a map from XML.
	 * @param element the element we're parsing
	 * @param stream the source to read more elements from
	 * @param players The collection to put players in
	 * @param warner the Warning instance to use for warnings
	 * @param idFactory the ID factory to use to generate IDs.
	 * @return the parsed map
	 * @throws SPFormatException on SP format problem
	 */
	@Override
	public IMap read(final StartElement element,
			final IteratorWrapper<XMLEvent> stream, final PlayerCollection players,
			final Warning warner, final IDFactory idFactory) throws SPFormatException {
		requireTag(element, MAP_TAG, "view");
		if ("view".equalsIgnoreCase(element.getName().getLocalPart())) {
			final StartElement mapElement = getFirstStartElement(stream,
					element.getLocation().getLineNumber());
			if (!MAP_TAG.equalsIgnoreCase(mapElement.getName().getLocalPart())) {
				throw new UnwantedChildException(element.getName()
						.getLocalPart(), mapElement.getName().getLocalPart(),
						mapElement.getLocation().getLineNumber());
			}
			final MapView retval = new MapView((SPMap) read(mapElement, stream, players,
					warner, idFactory), Integer.parseInt(getParameter(element,
					"current_player")), Integer.parseInt(getParameter(element,
					"current_turn")), getFile(stream));
			spinUntilEnd(element.getName(), stream);
			return retval; // NOPMD: TODO: Perhaps split this into parseMap and parseView?
		} else {
			final SPMap retval = new SPMap(Integer.parseInt(getParameter(
					element, "version", "1")), Integer.parseInt(getParameter(
					element, "rows")), Integer.parseInt(getParameter(element,
					"columns")), getFile(stream));
			for (final XMLEvent event : stream) {
				if (event.isStartElement()) {
					parseChild(stream, warner, retval, event.asStartElement(), idFactory);
				} else if (event.isEndElement() && element.getName().equals(event.asEndElement().getName())) {
					break;
				}
			}
			if (hasParameter(element, "current_player")) {
				retval.getPlayers()
						.getPlayer(
								Integer.parseInt(getParameter(element,
										"current_player"))).setCurrent(true);
			}
			return retval;
		}
	}
	/**
	 * Parse a child element of a map tag.
	 * @param stream the stream to read more elements from
	 * @param warner the Warning instance to use for warnings
	 * @param map the map to add tiles and players to
	 * @param elem the tag to parse
	 * @param idFactory the ID factory to use to generate IDs
	 * @throws SPFormatException on SP format problem
	 */
	private static void parseChild(final IteratorWrapper<XMLEvent> stream, final Warning warner,
			final SPMap map, final StartElement elem, final IDFactory idFactory) throws SPFormatException {
		final String tag = elem.getName().getLocalPart();
		if ("player".equalsIgnoreCase(tag)) {
			map.addPlayer(CompactPlayerReader.READER.read(elem, stream,
					map.getPlayers(), warner, idFactory));
		} else if ("tile".equalsIgnoreCase(tag)) {
			map.addTile(CompactTileReader.READER.read(elem, stream, map.getPlayers(), warner, idFactory));
		} else if (EqualsAny.equalsAny(tag, ISPReader.FUTURE)) {
			warner.warn(new UnsupportedTagException(tag, elem.getLocation().getLineNumber()));
		} else if (!"row".equalsIgnoreCase(tag)) {
			throw new UnwantedChildException(MAP_TAG, tag, elem.getLocation().getLineNumber());
		}
	}
	/**
	 * @param stream a stream of XMLEvents
	 * @param line the line the parent tag is on
	 * @return the first StartElement in the stream
	 * @throws SPFormatException if there is no child tag
	 */
	private static StartElement getFirstStartElement(
			final IteratorWrapper<XMLEvent> stream,			final int line) throws SPFormatException {
		for (XMLEvent event : stream) {
			if (event.isStartElement()) {
				return event.asStartElement();
			}
		}
		throw new MissingChildException(MAP_TAG, line);
	}
	/**
	 * Singleton.
	 */
	private CompactMapReader() {
		// Singleton.
	}
	/**
	 * Singleton instance.
	 */
	public static final CompactMapReader READER = new CompactMapReader();
	/**
	 * @param tag a tag
	 * @return whether it's one we support
	 */
	@Override
	public boolean isSupportedTag(final String tag) {
		return MAP_TAG.equalsIgnoreCase(tag) || "view".equalsIgnoreCase(tag);
	}
	/**
	 * Write an object to a stream.
	 * @param out The stream to write to.
	 * @param obj The object to write.
	 * @param file The file we're writing to.
	 * @param inclusion Whether to change files if a sub-object was read from a different file
	 * @param indent The current indentation level.
	 * @throws IOException on I/O error
	 */
	@Override
	public void write(final Writer out, final IMap obj, final String file, final boolean inclusion,
			final int indent) throws IOException {
		out.append(indent(indent));
		if (obj instanceof MapView) {
			out.append("<view current_player=\"");
			out.append(Integer.toString(obj.getPlayers().getCurrentPlayer().getPlayerId()));
			out.append("\" current_turn=\"");
			out.append(Integer.toString(((MapView) obj).getCurrentTurn()));
			out.append("\">\n");
			CompactReaderAdapter.ADAPTER.write(out, ((MapView) obj).getMap(), file, inclusion, indent + 1);
			out.append(indent(indent));
			out.append("</view>\n");
		} else if (obj instanceof SPMap) {
			out.append("<map version=\"");
			out.append(Integer.toString(((SPMap) obj).getVersion()));
			out.append("\" rows=\"");
			out.append(Integer.toString(((SPMap) obj).rows()));
			out.append("\" columns=\"");
			out.append(Integer.toString(((SPMap) obj).cols()));
			if (!obj.getPlayers().getCurrentPlayer().getName().isEmpty()) {
				out.append("\" current_player=\"");
				out.append(Integer.toString(obj.getPlayers().getCurrentPlayer().getPlayerId()));
			}
			out.append("\">\n");
			for (Player player : obj.getPlayers()) {
				CompactReaderAdapter.ADAPTER.write(out, player, file, inclusion, indent + 1);
			}
			for (int i = 0; i < obj.rows(); i++) {
				boolean rowEmpty = true;
				for (int j = 0; j < obj.cols(); j++) {
					final Tile tile = obj.getTile(PointFactory.point(i, j));
					if (!tile.isEmpty() && rowEmpty) {
						out.append(indent(indent + 1));
						out.append("<row index=\"");
						out.append(Integer.toString(i));
						out.append("\">\n");
						rowEmpty = false;
					}
					CompactReaderAdapter.ADAPTER.write(out, obj.getTile(PointFactory.point(i, j)), file, inclusion, indent + 2);
				}
				if (!rowEmpty) {
					out.append(indent(indent + 1));
					out.append("</row>\n");
				}
			}
			out.append(indent(indent));
			out.append("</map>\n");
		}
	}
}
