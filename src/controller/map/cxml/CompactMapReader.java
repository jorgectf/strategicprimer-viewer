package controller.map.cxml;

import static util.NullCleaner.assertNotNull;

import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;

import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import controller.map.formatexceptions.MissingChildException;
import controller.map.formatexceptions.SPFormatException;
import controller.map.formatexceptions.SPMalformedInputException;
import controller.map.formatexceptions.UnsupportedTagException;
import controller.map.formatexceptions.UnwantedChildException;
import controller.map.iointerfaces.ISPReader;
import controller.map.misc.IDFactory;
import model.map.IMap;
import model.map.IMapView;
import model.map.IMutablePlayerCollection;
import model.map.ITile;
import model.map.MapDimensions;
import model.map.MapView;
import model.map.Player;
import model.map.Point;
import model.map.PointFactory;
import model.map.SPMap;
import util.EqualsAny;
import util.IteratorWrapper;
import util.NullCleaner;
import util.Warning;

/**
 * A reader for maps.
 *
 * This is part of the Strategic Primer assistive programs suite developed by
 * Jonathan Lovelace.
 *
 * Copyright (C) 2012-2015 Jonathan Lovelace
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of version 3 of the GNU General Public License as published by the
 * Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * @author Jonathan Lovelace
 *
 */
@SuppressWarnings("deprecation")
public final class CompactMapReader extends AbstractCompactReader<IMap> {
	/**
	 * Singleton instance.
	 */
	public static final CompactMapReader READER = new CompactMapReader();

	/**
	 * The 'map' tag.
	 */
	private static final String MAP_TAG = "map";

	/**
	 * Read a map from XML.
	 *
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
			final IteratorWrapper<XMLEvent> stream,
			final IMutablePlayerCollection players, final Warning warner,
			final IDFactory idFactory) throws SPFormatException {
		requireTag(element, MAP_TAG, "view");
		if ("view".equalsIgnoreCase(element.getName().getLocalPart())) {
			final StartElement mapElement = getFirstStartElement(stream,
					element.getLocation().getLineNumber());
			if (!MAP_TAG.equalsIgnoreCase(mapElement.getName().getLocalPart())) {
				throw new UnwantedChildException(assertNotNull(assertNotNull(
						element.getName()).getLocalPart()),
						assertNotNull(assertNotNull(mapElement.getName())
								.getLocalPart()), mapElement.getLocation()
								.getLineNumber());
			}
			final NumberFormat numParser = NumberFormat.getIntegerInstance();
			try {
				final MapView retval =
						new MapView(read(mapElement, stream, players, warner,
								idFactory), numParser.parse(
								getParameter(element, "current_player"))
								.intValue(), numParser.parse(
								getParameter(element, "current_turn"))
								.intValue());
				spinUntilEnd(assertNotNull(element.getName()), stream);
				return retval; // NOPMD:
			} catch (ParseException e) {
				throw new SPMalformedInputException(mapElement.getLocation()
						.getLineNumber(), e);
			}
			// TODO: Perhaps split this into parseMap/parseView?
		} else {
			final SPMap retval = new SPMap(new MapDimensions(
					getIntegerParameter(element, "rows"),
					getIntegerParameter(element, "columns"),
					getIntegerParameter(element, "version", 1)));
			for (final XMLEvent event : stream) {
				if (event.isStartElement()) {
					parseChild(stream, warner, retval,
							NullCleaner.assertNotNull(event.asStartElement()),
							idFactory);
				} else if (event.isEndElement()
						&& element.getName().equals(
								event.asEndElement().getName())) {
					break;
				}
			}
			if (hasParameter(element, "current_player")) {
				retval.getPlayers()
						.getPlayer(
								getIntegerParameter(element, "current_player"))
						.setCurrent(true);
			}
			return retval;
		}
	}

	/**
	 * Parse a child element of a map tag.
	 *
	 * @param stream the stream to read more elements from
	 * @param warner the Warning instance to use for warnings
	 * @param map the map to add tiles and players to
	 * @param elem the tag to parse
	 * @param idFactory the ID factory to use to generate IDs
	 * @throws SPFormatException on SP format problem
	 */
	private static void parseChild(final IteratorWrapper<XMLEvent> stream,
			final Warning warner, final SPMap map, final StartElement elem,
			final IDFactory idFactory) throws SPFormatException {
		final String tag = elem.getName().getLocalPart();
		if (tag == null) {
			throw new IllegalStateException("Null tag");
		} else if ("player".equalsIgnoreCase(tag)) {
			map.addPlayer(CompactPlayerReader.READER.read(elem, stream,
					map.getPlayers(), warner, idFactory));
		} else if ("tile".equalsIgnoreCase(tag)) {
			try {
				final int row = NumberFormat.getIntegerInstance()
						.parse(getParameter(elem, "row")).intValue();
				final int col =
						NumberFormat.getIntegerInstance()
								.parse(getParameter(elem, "column")).intValue();
				final Point loc = PointFactory.point(row, col);
				map.addTile(
						loc,
						CompactTileReader.READER.read(elem, stream,
								map.getPlayers(), warner, idFactory));
			} catch (ParseException e) {
				throw new SPMalformedInputException(elem.getLocation()
						.getLineNumber(), e);
			}
		} else if (EqualsAny.equalsAny(tag, ISPReader.FUTURE)) {
			warner.warn(new UnsupportedTagException(tag, elem.getLocation()
					.getLineNumber()));
		} else if (!"row".equalsIgnoreCase(tag)) {
			throw new UnwantedChildException(MAP_TAG, tag, elem.getLocation()
					.getLineNumber());
		}
	}

	/**
	 * @param stream a stream of XMLEvents
	 * @param line the line the parent tag is on
	 * @return the first StartElement in the stream
	 * @throws SPFormatException if there is no child tag
	 */
	private static StartElement getFirstStartElement(
			final IteratorWrapper<XMLEvent> stream, final int line)
			throws SPFormatException {
		for (final XMLEvent event : stream) {
			if (event.isStartElement()) {
				return NullCleaner.assertNotNull(event.asStartElement());
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
	 * @param tag a tag
	 * @return whether it's one we support
	 */
	@Override
	public boolean isSupportedTag(final String tag) {
		return MAP_TAG.equalsIgnoreCase(tag) || "view".equalsIgnoreCase(tag);
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
	public void write(final Appendable ostream, final IMap obj, final int indent)
			throws IOException {
		ostream.append(indent(indent));
		if (obj instanceof MapView) {
			ostream.append("<view current_player=\"");
			ostream.append(Integer.toString(obj.getPlayers().getCurrentPlayer()
					.getPlayerId()));
			ostream.append("\" current_turn=\"");
			ostream.append(Integer.toString(((IMapView) obj).getCurrentTurn()));
			ostream.append("\">\n");
			write(ostream, ((IMapView) obj).getMap(), indent + 1);
			ostream.append(indent(indent));
			ostream.append("</view>\n");
		} else if (obj instanceof SPMap) {
			writeMap(ostream, (SPMap) obj, indent);
		}
	}

	/**
	 * @param ostream the stream to write to
	 * @param obj the map to write
	 * @param indent the current indentation level
	 * @throws IOException on I/O error
	 */
	private static void writeMap(final Appendable ostream, final SPMap obj,
			final int indent) throws IOException {
		final MapDimensions dim = obj.getDimensions();
		ostream.append("<map version=\"");
		ostream.append(Integer.toString(dim.version));
		ostream.append("\" rows=\"");
		ostream.append(Integer.toString(dim.rows));
		ostream.append("\" columns=\"");
		ostream.append(Integer.toString(dim.cols));
		if (!obj.getPlayers().getCurrentPlayer().getName().isEmpty()) {
			ostream.append("\" current_player=\"");
			ostream.append(Integer.toString(obj.getPlayers().getCurrentPlayer()
					.getPlayerId()));
		}
		ostream.append("\">\n");
		for (final Player player : obj.getPlayers()) {
			CompactPlayerReader.READER.write(ostream, player, indent + 1);
		}
		for (int i = 0; i < dim.rows; i++) {
			boolean rowEmpty = true;
			for (int j = 0; j < dim.cols; j++) {
				final ITile tile = obj.getTile(PointFactory.point(i, j));
				if (!tile.isEmpty() && rowEmpty) {
					ostream.append(indent(indent + 1));
					ostream.append("<row index=\"");
					ostream.append(Integer.toString(i));
					ostream.append("\">\n");
					rowEmpty = false;
				}
				final Point point = PointFactory.point(i, j);
				CompactTileReader.writeTile(ostream, point, obj.getTile(point),
						indent + 2);
			}
			if (!rowEmpty) {
				ostream.append(indent(indent + 1));
				ostream.append("</row>\n");
			}
		}
		ostream.append(indent(indent));
		ostream.append("</map>\n");
	}
	/**
	 * @return a String representation of the object
	 */
	@Override
	public String toString() {
		return "CompactMapReader";
	}
}
