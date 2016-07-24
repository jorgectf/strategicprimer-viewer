package controller.map.cxml;

import controller.map.formatexceptions.SPFormatException;
import controller.map.formatexceptions.UnsupportedTagException;
import controller.map.misc.IDRegistrar;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import model.map.IEvent;
import model.map.IMutablePlayerCollection;
import model.map.fixtures.explorable.Battlefield;
import model.map.fixtures.explorable.Cave;
import model.map.fixtures.explorable.ExplorableFixture;
import util.ArraySet;
import util.NullCleaner;
import util.Warning;

/**
 * A reader for Caves and Battlefields.
 *
 * This is part of the Strategic Primer assistive programs suite developed by Jonathan
 * Lovelace.
 *
 * Copyright (C) 2015-2015 Jonathan Lovelace
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
 * @deprecated CompactXML is deprecated in favor of FluidXML
 */
@Deprecated
public final class CompactExplorableReader
		extends AbstractCompactReader<ExplorableFixture> {
	/**
	 * Singleton object.
	 */
	public static final CompactReader<ExplorableFixture> READER =
			new CompactExplorableReader();
	/**
	 * List of supported tags.
	 */
	private static final Set<String> SUPP_TAGS;

	static {
		final Set<String> suppTagsTemp = new ArraySet<>();
		suppTagsTemp.add("cave");
		suppTagsTemp.add("battlefield");
		SUPP_TAGS = NullCleaner.assertNotNull(Collections.unmodifiableSet(suppTagsTemp));
	}

	/**
	 * @param tag a tag
	 * @return whether we support it
	 */
	@Override
	public boolean isSupportedTag(final String tag) {
		return SUPP_TAGS.contains(tag);
	}

	/**
	 * @param element a tag
	 * @return the value of its 'dc' property.
	 * @throws SPFormatException on SP format problem
	 */
	private static int getDC(final StartElement element)
			throws SPFormatException {
		return getIntegerParameter(element, "dc");
	}

	/**
	 * @param element      the XML element to parse
	 * @param stream    the stream to read more elements from
	 * @param players   the collection of players
	 * @param warner    the Warning instance to use for warnings
	 * @param idFactory the ID factory to use to generate IDs
	 * @return the parsed resource
	 * @throws SPFormatException on SP format problems
	 */
	@Override
	public ExplorableFixture read(final StartElement element,
								final Iterable<XMLEvent> stream,
								final IMutablePlayerCollection players,
								final Warning warner, final IDRegistrar idFactory)
			throws SPFormatException {
		requireTag(element, "battlefield", "cave");
		final int idNum = getOrGenerateID(element, warner, idFactory);
		final ExplorableFixture retval;
		final String tag = element.getName().getLocalPart();
		if ("battlefield".equalsIgnoreCase(tag)) {
			retval = new Battlefield(getDC(element), idNum);
		} else if ("cave".equalsIgnoreCase(tag)) {
			retval = new Cave(getDC(element), idNum);
		} else {
			throw new UnsupportedTagException(element);
		}
		spinUntilEnd(NullCleaner.assertNotNull(element.getName()), stream);
		retval.setImage(getParameter(element, "image", ""));
		return retval;
	}

	/**
	 * Write an object to a stream.
	 *
	 * @param ostream The stream to write to.
	 * @param obj     The object to write.
	 * @param indent  The current indentation level.
	 * @throws IOException on I/O error
	 */
	@Override
	public void write(final Appendable ostream, final ExplorableFixture obj,
					final int indent) throws IOException {
		if (obj instanceof Battlefield) {
			writeTag(ostream, "battlefield", indent);
		} else if (obj instanceof Cave) {
			writeTag(ostream, "cave", indent);
		} else {
			throw new IllegalStateException("Unhandled ExplorableFixture subtype");
		}
		ostream.append(" dc=\"");
		ostream.append(Integer.toString(((IEvent) obj).getDC()));
		ostream.append("\" id=\"");
		ostream.append(Integer.toString(obj.getID()));
		ostream.append('"');
		ostream.append(imageXML(obj));
		ostream.append(" />");
		ostream.append(LINE_SEP);
	}

	/**
	 * @return a string representation of this class
	 */
	@SuppressWarnings("MethodReturnAlwaysConstant")
	@Override
	public String toString() {
		return "CompactExplorableReader";
	}
	/**
	 * @param obj an object
	 * @return whether we can write it
	 */
	@Override
	public boolean canWrite(final Object obj) {
		return (obj instanceof Battlefield) || (obj instanceof Cave);
	}

}
