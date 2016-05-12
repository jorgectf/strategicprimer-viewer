package controller.map.cxml;

import controller.map.formatexceptions.DeprecatedPropertyException;
import controller.map.formatexceptions.MissingPropertyException;
import controller.map.formatexceptions.SPFormatException;
import controller.map.misc.IDFactory;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import model.map.IEvent;
import model.map.IMutablePlayerCollection;
import model.map.fixtures.resources.CacheFixture;
import model.map.fixtures.resources.FieldStatus;
import model.map.fixtures.resources.Grove;
import model.map.fixtures.resources.HarvestableFixture;
import model.map.fixtures.resources.Meadow;
import model.map.fixtures.resources.Mine;
import model.map.fixtures.resources.MineralVein;
import model.map.fixtures.resources.Shrub;
import model.map.fixtures.resources.StoneDeposit;
import model.map.fixtures.resources.StoneKind;
import model.map.fixtures.towns.TownStatus;
import util.IteratorWrapper;
import util.NullCleaner;
import util.Warning;

import static java.lang.Boolean.parseBoolean;

/**
 * A reader for resource-bearing TileFixtures.
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
 * @deprecated CompactXML is deprecated in favor of FluidXML
 */
@Deprecated
public final class CompactResourceReader extends
		AbstractCompactReader<HarvestableFixture> {
	/**
	 * Singleton object.
	 */
	public static final CompactResourceReader READER = new CompactResourceReader();
	/**
	 * The parameter giving the status of a fixture.
	 */
	private static final String STATUS_PAR = "status";
	/**
	 * The parameter saying what kind of thing is in a HarvestableFixture.
	 */
	private static final String KIND_PAR = "kind";
	/**
	 * The parameter saying whether a grove or field or orchard or meadow is cultivated.
	 */
	private static final String CULTIVATED_PARAM = "cultivated";

	/**
	 * List of supported tags.
	 */
	private static final Set<String> SUPP_TAGS =
			NullCleaner.assertNotNull(Collections.unmodifiableSet(new HashSet<>(
					Arrays.asList("cache", "grove", "orchard", "field",
							"meadow", "mine", "mineral", "shrub", "stone"))));

	/**
	 * Singleton.
	 */
	private CompactResourceReader() {
		// Singleton.
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
	 * @param element      the XML element to parse
	 * @param stream    the stream to read more elements from
	 * @param players   the collection of players
	 * @param warner    the Warning instance to use for warnings
	 * @param idFactory the ID factory to use to generate IDs
	 * @return the parsed resource
	 * @throws SPFormatException on SP format problems
	 */
	@Override
	public HarvestableFixture read(final StartElement element,
								final IteratorWrapper<XMLEvent> stream,
								final IMutablePlayerCollection players,
								final Warning warner,
								final IDFactory idFactory) throws SPFormatException {
		requireTag(element, "cache", "grove", "orchard",
				"field", "meadow", "mine", "mineral", "shrub", "stone");
		final int idNum = getOrGenerateID(element, warner, idFactory);
		final HarvestableFixture retval; // NOPMD
		switch (element.getName().getLocalPart().toLowerCase()) {
		case "cache":
			retval = new CacheFixture(getParameter(element, KIND_PAR),
											getParameter(element, "contents"), idNum);
			break;
		case "field":
			retval = createMeadow(element, true, idNum, warner);
			break;
		case "grove":
			retval = createGrove(element, false, idNum, warner);
			break;
		case "meadow":
			retval = createMeadow(element, false, idNum, warner);
			break;
		case "mine":
			retval = new Mine(getParamWithDeprecatedForm(element, KIND_PAR,
					"product", warner),
									TownStatus.parseTownStatus(
											getParameter(element, STATUS_PAR)),
									idNum);
			break;
		case "mineral":
			retval = new MineralVein(getParamWithDeprecatedForm(element, KIND_PAR,
					"mineral", warner), parseBoolean(getParameter(element,
					"exposed")), getDC(element), idNum);
			break;
		case "orchard":
			retval = createGrove(element, true, idNum, warner);
			break;
		case "shrub":
			retval = new Shrub(getParamWithDeprecatedForm(element, KIND_PAR,
					"shrub", warner), idNum);
			break;
		case "stone":
			retval = new StoneDeposit(StoneKind.parseStoneKind(
					getParamWithDeprecatedForm(element,
							KIND_PAR, "stone", warner)), getDC(element), idNum);
			break;
		default:
			throw new IllegalArgumentException("Unhandled harvestable tag");
		}
		spinUntilEnd(NullCleaner.assertNotNull(element.getName()), stream);
		retval.setImage(getParameter(element, "image", ""));
		return retval;
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
	 * Create a Meadow, to reduce code duplication between 'field' and 'meadow' cases.
	 *
	 * @param element the tag we're parsing
	 * @param field   whether this is a field (meadow otherwise)
	 * @param idNum   the ID number parsed or generated
	 * @param warner  the Warning instance to use for warnings
	 * @return the parsed Meadow object.
	 * @throws SPFormatException on SP format problems
	 */
	private static HarvestableFixture createMeadow(final StartElement element,
												final boolean field, final int idNum,
												final Warning warner)
			throws SPFormatException {
		if (!hasParameter(element, STATUS_PAR)) {
			warner.warn(new MissingPropertyException(element, STATUS_PAR));
		}
		return new Meadow(getParameter(element, KIND_PAR), field,
								parseBoolean(getParameter(element, CULTIVATED_PARAM)),
								idNum,
								FieldStatus.parse(getParameter(element, STATUS_PAR,
										FieldStatus.random(idNum).toString())));
	}

	/**
	 * Create a Grove, to reduce code duplication between 'grove' and 'orchard' cases.
	 *
	 * @param element the tag we're parsing
	 * @param orchard whether this is an orchard, a grove otherwise
	 * @param idNum   the ID number parsed or generated
	 * @param warner  the Warning instance to use for warnings
	 * @return the parsed Grove object
	 * @throws SPFormatException on SP format problems
	 */
	private static HarvestableFixture createGrove(final StartElement element,
												final boolean orchard, final int idNum,
												final Warning warner)
			throws SPFormatException {
		return new Grove(orchard, isCultivated(element, warner),
								getParamWithDeprecatedForm(element, KIND_PAR, "tree",
										warner),
								idNum);
	}

	/**
	 * @param element a tag representing a grove or orchard
	 * @param warner  the Warning instance to use
	 * @return whether the grove or orchard is cultivated
	 * @throws SPFormatException on SP format problems: use of 'wild' if warnings are
	 *                           fatal, or if both properties are missing.
	 */
	private static boolean isCultivated(final StartElement element,
										final Warning warner) throws SPFormatException {
		if (hasParameter(element, CULTIVATED_PARAM)) {
			return parseBoolean(getParameter(element, CULTIVATED_PARAM)); // NOPMD
		} else {
			if (hasParameter(element, "wild")) {
				warner.warn(new DeprecatedPropertyException(element, "wild",
																CULTIVATED_PARAM));
				return !parseBoolean(getParameter(element, "wild"));
			} else {
				throw new MissingPropertyException(element, CULTIVATED_PARAM);
			}
		}
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
	public void write(final Appendable ostream, final HarvestableFixture obj,
					final int indent) throws IOException {
		if (obj instanceof CacheFixture) {
			writeTag(ostream, "cache", indent);
			ostream.append(" kind=\"");
			ostream.append(((CacheFixture) obj).getKind());
			ostream.append("\" contents=\"");
			ostream.append(((CacheFixture) obj).getContents());
		} else if (obj instanceof Meadow) {
			writeTag(ostream, getMeadowTag((Meadow) obj), indent);
			ostream.append(" kind=\"");
			ostream.append(((Meadow) obj).getKind());
			ostream.append("\" cultivated=\"");
			ostream.append(Boolean.toString(((Meadow) obj).isCultivated()));
			ostream.append("\" status=\"");
			ostream.append(((Meadow) obj).getStatus().toString());
		} else if (obj instanceof Grove) {
			writeTag(ostream, getGroveTag((Grove) obj), indent);
			ostream.append(" cultivated=\"");
			ostream.append(Boolean.toString(((Grove) obj).isCultivated()));
			ostream.append("\" kind=\"");
			ostream.append(((Grove) obj).getKind());
		} else if (obj instanceof Mine) {
			writeTag(ostream, "mine", indent);
			ostream.append(" kind=\"");
			ostream.append(((Mine) obj).getKind());
			ostream.append("\" status=\"");
			ostream.append(((Mine) obj).getStatus().toString());
		} else if (obj instanceof MineralVein) {
			writeTag(ostream, "mineral", indent);
			ostream.append(" kind=\"");
			ostream.append(((MineralVein) obj).getKind());
			ostream.append("\" exposed=\"");
			ostream.append(Boolean.toString(((MineralVein) obj).isExposed()));
			ostream.append("\" dc=\"");
			ostream.append(Integer.toString(((IEvent) obj).getDC()));
		} else if (obj instanceof Shrub) {
			writeTag(ostream, "shrub", indent);
			ostream.append(" kind=\"");
			ostream.append(((Shrub) obj).getKind());
		} else if (obj instanceof StoneDeposit) {
			writeTag(ostream, "stone", indent);
			ostream.append(" kind=\"");
			ostream.append(((StoneDeposit) obj).stone().toString());
			ostream.append("\" dc=\"");
			ostream.append(Integer.toString(((StoneDeposit) obj).getDC()));
		} else {
			throw new IllegalStateException("Unhandled HarvestableFixture subtype");
		}
		ostream.append("\" id=\"");
		ostream.append(Integer.toString(obj.getID()));
		ostream.append('"');
		ostream.append(imageXML(obj));
		ostream.append(" />\n");
	}

	/**
	 * @param meadow a meadow or field
	 * @return the proper tag for it
	 */
	@SuppressWarnings("TypeMayBeWeakened")
	private static String getMeadowTag(final Meadow meadow) {
		if (meadow.isField()) {
			return "field"; // NOPMD
		} else {
			return "meadow";
		}
	}

	/**
	 * @param grove a grove or orchard
	 * @return the proper tag for it
	 */
	@SuppressWarnings("TypeMayBeWeakened")
	private static String getGroveTag(final Grove grove) {
		if (grove.isOrchard()) {
			return "orchard"; // NOPMD
		} else {
			return "grove";
		}
	}

	/**
	 * @return a String representation of the object
	 */
	@SuppressWarnings("MethodReturnAlwaysConstant")
	@Override
	public String toString() {
		return "CompactResourceReader";
	}
	/**
	 * @param obj an object
	 * @return whether we can write it
	 */
	@Override
	public boolean canWrite(final Object obj) {
		return obj instanceof HarvestableFixture;
	}
}
