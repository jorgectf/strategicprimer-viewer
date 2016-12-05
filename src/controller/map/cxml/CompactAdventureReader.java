package controller.map.cxml;

import controller.map.formatexceptions.SPFormatException;
import controller.map.misc.IDRegistrar;
import java.io.IOException;
import javax.xml.namespace.QName;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import model.map.IMutablePlayerCollection;
import model.map.Player;
import model.map.fixtures.explorable.AdventureFixture;
import util.LineEnd;
import util.NullCleaner;
import util.Warning;

/**
 * A reader for adventure hooks.
 *
 * This is part of the Strategic Primer assistive programs suite developed by Jonathan
 * Lovelace.
 *
 * Copyright (C) 2014-2016 Jonathan Lovelace
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of version 3 of the GNU General Public License as published by the Free Software
 * Foundation; see COPYING or
 * <a href="http://www.gnu.org/licenses/">http://www.gnu.org/licenses/</a>.
 *
 * @author Jonathan Lovelace
 * @deprecated CompactXML is deprecated in favor of FluidXML
 */
@SuppressWarnings("ClassHasNoToStringMethod")
@Deprecated
public final class CompactAdventureReader extends
		AbstractCompactReader<AdventureFixture> {
	/**
	 * Singleton object.
	 */
	public static final CompactReader<AdventureFixture> READER =
			new CompactAdventureReader();

	/**
	 * Read an adventure from XML.
	 *
	 * @param element   The XML element to parse
	 * @param parent    the parent tag
	 * @param players   the collection of players
	 * @param warner    the Warning instance to use for warnings
	 * @param idFactory the ID factory to use to generate IDs
	 * @param stream    the stream to read more elements from
	 * @return the parsed adventure
	 * @throws SPFormatException on SP format problems
	 */
	@Override
	public AdventureFixture read(final StartElement element,
								 final QName parent,
								 final IMutablePlayerCollection players,
								 final Warning warner, final IDRegistrar idFactory,
								 final Iterable<XMLEvent> stream)
			throws SPFormatException {
		requireTag(element, parent, "adventure");
		Player player = players.getIndependent();
		if (hasParameter(element, "owner")) {
			player = players.getPlayer(getIntegerParameter(element, "owner"));
		}
		final AdventureFixture retval =
				new AdventureFixture(player,
											getParameter(element, "brief", ""),
											getParameter(
													element, "full", ""),
											getOrGenerateID(element,
													warner, idFactory));
		retval.setImage(getParameter(element, "image", ""));
		spinUntilEnd(NullCleaner.assertNotNull(element.getName()), stream);
		return retval;
	}

	/**
	 * Write an adventure to XML.
	 *
	 * @param ostream the stream to write to
	 * @param obj     the adventure to write
	 * @param indent  the current indentation level
	 * @throws IOException on I/O error
	 */
	@Override
	public void write(final Appendable ostream, final AdventureFixture obj,
					  final int indent) throws IOException {
		writeTag(ostream, "adventure", indent);
		writeProperty(ostream, "id", Integer.toString(obj.getID()));
		if (!obj.getOwner().isIndependent()) {
			writeProperty(ostream, "owner",
					Integer.toString(obj.getOwner().getPlayerId()));
		}
		if (!obj.getBriefDescription().isEmpty()) {
			writeProperty(ostream, "brief", obj.getBriefDescription());
		}
		if (!obj.getFullDescription().isEmpty()) {
			writeProperty(ostream, "full", obj.getFullDescription());
		}
		ostream.append(imageXML(obj));
		ostream.append(" />");
		ostream.append(LineEnd.LINE_SEP);
	}

	/**
	 * @param tag a tag
	 * @return whether it is one we support
	 */
	@Override
	public boolean isSupportedTag(final String tag) {
		return "adventure".equalsIgnoreCase(tag);
	}

	/**
	 * @param obj an object
	 * @return whether we can write it
	 */
	@Override
	public boolean canWrite(final Object obj) {
		return obj instanceof AdventureFixture;
	}
}
