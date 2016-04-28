package controller.map.readerng;

import controller.map.formatexceptions.SPFormatException;
import controller.map.misc.IDFactory;
import java.util.Collections;
import java.util.List;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import model.map.IMutablePlayerCollection;
import model.map.fixtures.resources.MineralVein;
import org.eclipse.jdt.annotation.NonNull;
import util.NullCleaner;
import util.Pair;
import util.Warning;

/**
 * A reader for Minerals.
 *
 * This is part of the Strategic Primer assistive programs suite developed by Jonathan
 * Lovelace.
 *
 * Copyright (C) 2012-2015 Jonathan Lovelace
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
 * @deprecated ReaderNG is deprecated
 */
@Deprecated
public final class MineralReader implements INodeHandler<@NonNull MineralVein> {
	/**
	 * Parse a Mineral.
	 *
	 * @param element   the element to read from
	 * @param stream    a stream of more elements
	 * @param players   the list of players
	 * @param warner    the Warning instance to use for warnings
	 * @param idFactory the factory to use to register ID numbers and generate new
	 *                     ones as
	 *                  needed
	 * @return the parsed mineral
	 * @throws SPFormatException on SP format error
	 */
	@Override
	public MineralVein parse(final StartElement element,
							final Iterable<XMLEvent> stream,
							final IMutablePlayerCollection players,
							final Warning warner, final IDFactory idFactory)
			throws SPFormatException {
		spinUntilEnd(NullCleaner.assertNotNull(element.getName()), stream);
		final MineralVein fix =
				new MineralVein(getAttributeWithDeprecatedForm(element, "kind",
						"mineral",
						warner), Boolean.parseBoolean(getAttribute(element, "exposed")),
									getIntegerAttribute(element, "dc"),
									getOrGenerateID(element, warner, idFactory));
		addImage(element, fix);
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
	public Class<MineralVein> writtenClass() {
		return MineralVein.class;
	}

	/**
	 * Create an intermediate representation to convert to XML.
	 *
	 * @param obj the object to write
	 * @return an intermediate representation
	 */
	@Override
	public SPIntermediateRepresentation write(final MineralVein obj) {
		// TODO: Statically import assertNotNull
		final SPIntermediateRepresentation retval =
				new SPIntermediateRepresentation("mineral",
														Pair.of("kind", obj.getKind()),
														Pair.of("exposed", NullCleaner
																				.assertNotNull(
																						Boolean.toString(
																								obj.isExposed()))),
														Pair.of("dc", NullCleaner
																			.assertNotNull(
																					Integer.toString(
																							obj.getDC()))));
		retval.addIdAttribute(obj.getID());
		retval.addImageAttribute(obj);
		return retval;
	}

	/**
	 * @return a String representation of the object
	 */
	@SuppressWarnings("MethodReturnAlwaysConstant")
	@Override
	public String toString() {
		return "MineralReader";
	}
}
