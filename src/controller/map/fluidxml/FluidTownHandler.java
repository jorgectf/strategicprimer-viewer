package controller.map.fluidxml;

import controller.map.formatexceptions.SPFormatException;
import controller.map.iointerfaces.ISPReader;
import controller.map.misc.IDRegistrar;
import java.util.Random;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import model.map.IPlayerCollection;
import model.map.fixtures.towns.AbstractTown;
import model.map.fixtures.towns.City;
import model.map.fixtures.towns.Fortification;
import model.map.fixtures.towns.Town;
import model.map.fixtures.towns.TownSize;
import model.map.fixtures.towns.Village;
import model.workermgmt.RaceFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import util.NullCleaner;
import util.Warning;

import static controller.map.fluidxml.XMLHelper.getAttribute;
import static controller.map.fluidxml.XMLHelper.getIntegerAttribute;
import static controller.map.fluidxml.XMLHelper.getOrGenerateID;
import static controller.map.fluidxml.XMLHelper.getPlayerOrIndependent;
import static controller.map.fluidxml.XMLHelper.requireNonEmptyAttribute;
import static controller.map.fluidxml.XMLHelper.requireTag;
import static controller.map.fluidxml.XMLHelper.setImage;
import static controller.map.fluidxml.XMLHelper.spinUntilEnd;
import static controller.map.fluidxml.XMLHelper.writeAttribute;
import static controller.map.fluidxml.XMLHelper.writeImage;
import static controller.map.fluidxml.XMLHelper.writeIntegerAttribute;
import static controller.map.fluidxml.XMLHelper.writeNonEmptyAttribute;
import static model.map.fixtures.towns.TownStatus.parseTownStatus;

/**
 * A class to hold XML I/O for towns, other than fortresses.
 *
 * This is part of the Strategic Primer assistive programs suite developed by Jonathan
 * Lovelace.
 *
 * Copyright (C) 2016 Jonathan Lovelace
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of version 3 of the GNU General Public License as published by the Free Software
 * Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this
 * program. If not, see
 * <a href="http://www.gnu.org/licenses/">http://www.gnu.org/licenses/</a>.
 *
 * @author Jonathan Lovelace
 */
public final class FluidTownHandler {
	/**
	 * Do not instantiate.
	 */
	private FluidTownHandler() {
		// Do not instantiate
	}
	/**
	 * Parse a town.
	 *
	 * @param element   the XML element to parse
	 * @param stream    the stream to read more elements from
	 * @param players   the collection of players in the map
	 * @param warner    the Warning instance to use for warnings
	 * @param idFactory the ID factory to use to generate IDs
	 * @return the parsed town
	 * @throws SPFormatException on SP format problems
	 */
	public static Town readTown(final StartElement element,
								final Iterable<XMLEvent> stream,
								final IPlayerCollection players,
								final Warning warner,
								final IDRegistrar idFactory) throws SPFormatException {
		requireTag(element, "town");
		requireNonEmptyAttribute(element, "name", false, warner);
		spinUntilEnd(NullCleaner.assertNotNull(element.getName()), stream);
		final Town fix =
				new Town(parseTownStatus(getAttribute(element, "status")),
								TownSize.parseTownSize(getAttribute(element, "size")),
								getIntegerAttribute(element, "dc"),
								getAttribute(element, "name", ""),
								getOrGenerateID(element, warner, idFactory),
								getPlayerOrIndependent(element, warner, players));
		fix.setPortrait(getAttribute(element, "portrait", ""));
		return setImage(fix, element, warner);
	}
	/**
	 * Parse a fortification.
	 *
	 * @param element   the XML element to parse
	 * @param stream    the stream to read more elements from
	 * @param players   the collection of players in the map
	 * @param warner    the Warning instance to use for warnings
	 * @param idFactory the ID factory to use to generate IDs
	 * @return the parsed fortification
	 * @throws SPFormatException on SP format problems
	 */
	public static Fortification readFortification(final StartElement element,
												  final Iterable<XMLEvent> stream,
												  final IPlayerCollection players,
												  final Warning warner,
												  final IDRegistrar idFactory) throws SPFormatException {
		requireTag(element, "fortification");
		requireNonEmptyAttribute(element, "name", false, warner);
		spinUntilEnd(NullCleaner.assertNotNull(element.getName()), stream);
		final Fortification fix =
				new Fortification(parseTownStatus(getAttribute(element, "status")),
								TownSize.parseTownSize(getAttribute(element, "size")),
								getIntegerAttribute(element, "dc"),
								getAttribute(element, "name", ""),
								getOrGenerateID(element, warner, idFactory),
								getPlayerOrIndependent(element, warner, players));
		fix.setPortrait(getAttribute(element, "portrait", ""));
		return setImage(fix, element, warner);
	}
	/**
	 * Parse a city.
	 *
	 * @param element   the XML element to parse
	 * @param stream    the stream to read more elements from
	 * @param players   the collection of players in the map
	 * @param warner    the Warning instance to use for warnings
	 * @param idFactory the ID factory to use to generate IDs
	 * @return the parsed city
	 * @throws SPFormatException on SP format problems
	 */
	public static City readCity(final StartElement element,
								final Iterable<XMLEvent> stream,
								final IPlayerCollection players,
								final Warning warner,
								final IDRegistrar idFactory) throws SPFormatException {
		requireTag(element, "city");
		requireNonEmptyAttribute(element, "name", false, warner);
		spinUntilEnd(NullCleaner.assertNotNull(element.getName()), stream);
		final City fix =
				new City(parseTownStatus(getAttribute(element, "status")),
								TownSize.parseTownSize(getAttribute(element, "size")),
								getIntegerAttribute(element, "dc"),
								getAttribute(element, "name", ""),
								getOrGenerateID(element, warner, idFactory),
								getPlayerOrIndependent(element, warner, players));
		fix.setPortrait(getAttribute(element, "portrait", ""));
		return setImage(fix, element, warner);
	}
	/**
	 * Parse a village.
	 *
	 * @param element   the XML element to parse
	 * @param stream    the stream to read more elements from
	 * @param players   the collection of players in the map
	 * @param warner    the Warning instance to use for warnings
	 * @param idFactory the ID factory to use to generate IDs
	 * @return the parsed village
	 * @throws SPFormatException on SP format problems
	 */
	public static Village readVillage(final StartElement element,
									  final Iterable<XMLEvent> stream,
									  final IPlayerCollection players,
									  final Warning warner,
									  final IDRegistrar idFactory)
			throws SPFormatException {
		requireTag(element, "village");
		requireNonEmptyAttribute(element, "name", false, warner);
		spinUntilEnd(NullCleaner.assertNotNull(element.getName()), stream);
		final int idNum = getOrGenerateID(element, warner, idFactory);
		final Village retval =
				new Village(parseTownStatus(getAttribute(element, "status")),
								   getAttribute(element, "name", ""), idNum,
								   getPlayerOrIndependent(element, warner, players),
								   getAttribute(element, "race",
										   RaceFactory.getRace(new Random(idNum))));
		retval.setPortrait(getAttribute(element, "portrait", ""));
		return setImage(retval, element, warner);
	}
	/**
	 * Write a Village to XML.
	 * @param obj the object to write. Must be a Village.
	 * @param document the Document object, used to get new Elements
	 * @param parent the parent tag, to which the subtree should be attached
	 * @throws IllegalArgumentException if obj is not the type we expect
	 */
	public static void writeVillage(final Document document, final Node parent,
									Object obj) {
		if (!(obj instanceof Village)) {
			throw new IllegalArgumentException("Can only write Village");
		}
		final Village fix = (Village) obj;
		final Element element = document.createElementNS(ISPReader.NAMESPACE, "village");
		writeAttribute(element, "status", fix.status().toString());
		writeNonEmptyAttribute(element, "name", fix.getName());
		writeIntegerAttribute(element, "id", fix.getID());
		writeIntegerAttribute(element, "owner", fix.getOwner().getPlayerId());
		writeAttribute(element, "race", fix.getRace());
		writeImage(element, fix);
		writeNonEmptyAttribute(element, "portrait", fix.getPortrait());
		parent.appendChild(element);
	}
	/**
	 * Write an AbstractTown to XML.
	 * @param obj the object to write. Must be an AbstractTown.
	 * @param document the Document object, used to get new Elements
	 * @param parent the parent tag, to which the subtree should be attached
	 * @throws IllegalArgumentException if obj is not the type we expect
	 */
	public static void writeTown(final Document document, final Node parent,
								 Object obj) {
		if (!(obj instanceof AbstractTown)) {
			throw new IllegalArgumentException("Can only write AbstractTown");
		}
		final AbstractTown fix = (AbstractTown) obj;
		final Element element;
		if (fix instanceof Fortification) {
			element = document.createElementNS(ISPReader.NAMESPACE, "fortification");
		} else if (fix instanceof Town) {
			element = document.createElementNS(ISPReader.NAMESPACE, "town");
		} else if (fix instanceof City) {
			element = document.createElementNS(ISPReader.NAMESPACE, "city");
		} else {
			throw new IllegalStateException("Unknown AbstractTownEvent type");
		}
		writeAttribute(element, "status", fix.status().toString());
		writeAttribute(element, "size", fix.size().toString());
		writeIntegerAttribute(element, "dc", fix.getDC());
		writeNonEmptyAttribute(element, "name", fix.getName());
		writeIntegerAttribute(element, "id", fix.getID());
		writeIntegerAttribute(element, "owner", fix.getOwner().getPlayerId());
		writeImage(element, fix);
		writeNonEmptyAttribute(element, "portrait", fix.getPortrait());
		parent.appendChild(element);
	}
}
