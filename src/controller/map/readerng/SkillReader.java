package controller.map.readerng;

import controller.map.formatexceptions.DeprecatedPropertyException;
import controller.map.formatexceptions.SPFormatException;
import controller.map.misc.IDFactory;
import java.util.Collections;
import java.util.List;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import model.map.IMutablePlayerCollection;
import model.map.fixtures.mobile.worker.Skill;
import org.eclipse.jdt.annotation.NonNull;
import util.NullCleaner;
import util.Warning;

/**
 * A reader for Skills.
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
public final class SkillReader implements INodeHandler<@NonNull Skill> {
	/**
	 * @return the class this knows how to write
	 */
	@Override
	public Class<Skill> writtenClass() {
		return Skill.class;
	}

	/**
	 * @return the list of tags this knows how to read
	 */
	@Override
	public List<String> understands() {
		return NullCleaner.assertNotNull(Collections.singletonList("skill"));
	}

	/**
	 * Parse a skill from XML.
	 *
	 * @param element   the current tag
	 * @param stream    the stream to read more tags from
	 * @param players   ignored
	 * @param warner    the Warning instance to report errors on
	 * @param idFactory the ID factory to use to generate IDs.
	 * @return the parsed job
	 * @throws SPFormatException on SP format error
	 */
	@Override
	public Skill parse(final StartElement element,
					final Iterable<XMLEvent> stream,
					final IMutablePlayerCollection players,
					final Warning warner, final IDFactory idFactory)
			throws SPFormatException {
		requireNonEmptyParameter(element, "name", true, warner);
		requireNonEmptyParameter(element, "level", true, warner);
		requireNonEmptyParameter(element, "hours", true, warner);
		spinUntilEnd(NullCleaner.assertNotNull(element.getName()), stream);
		final Skill retval = new Skill(getAttribute(element, "name"),
											getIntegerAttribute(element, "level"),
											getIntegerAttribute(element, "hours"));
		if ("miscellaneous".equals(retval.getName()) && (retval.getLevel() > 0)) {
			warner.warn(
					new DeprecatedPropertyException(element, "miscellaneous", "other"));
		}
		return retval;
	}

	/**
	 * Create an intermediate representation to convert to XML.
	 *
	 * @param obj the object to write
	 * @return the intermediate representation
	 */
	@Override
	public SPIntermediateRepresentation write(final Skill obj) {
		final SPIntermediateRepresentation retval = new SPIntermediateRepresentation(
																							"skill");
		retval.addAttribute("name", obj.getName());
		retval.addIntegerAttribute("level", obj.getLevel());
		retval.addIntegerAttribute("hours", obj.getHours());
		return retval;
	}

	/**
	 * @return a String representation of the object
	 */
	@SuppressWarnings("MethodReturnAlwaysConstant")
	@Override
	public String toString() {
		return "SkillReader";
	}
}
