package controller.map.cxml;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.eclipse.jdt.annotation.Nullable;

import model.map.HasImage;
import model.map.IEvent;
import model.map.PlayerCollection;
import model.map.fixtures.resources.Battlefield;
import model.map.fixtures.resources.CacheFixture;
import model.map.fixtures.resources.Cave;
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
import util.ArraySet;
import util.IteratorWrapper;
import util.Warning;
import controller.map.formatexceptions.DeprecatedPropertyException;
import controller.map.formatexceptions.MissingPropertyException;
import controller.map.formatexceptions.SPFormatException;
import controller.map.misc.IDFactory;

/**
 * A reader for resource-bearing TileFixtures.
 *
 * @author Jonathan Lovelace
 *
 */
public final class CompactResourceReader extends AbstractCompactReader
		implements CompactReader<HarvestableFixture> {
	/**
	 * The parameter giving the status of a fixture.
	 */
	private static final String STATUS_PARAM = "status";
	/**
	 * The parameter saying what kind of thing is in a HarvestableFixture.
	 */
	private static final String KIND_PARAM = "kind";
	/**
	 * The parameter saying whether a grove or field or orchard or meadow is
	 * cultivated.
	 */
	private static final String CULTIVATED_PARAM = "cultivated";

	/**
	 * Singleton.
	 */
	private CompactResourceReader() {
		// Singleton.
	}

	/**
	 * Singleton object.
	 */
	public static final CompactResourceReader READER = new CompactResourceReader();

	/**
	 * Enumeration of the types we know how to handle.
	 */
	private static enum HarvestableType {
		/**
		 * Battlefield.
		 */
		BattlefieldType("battlefield"),
		/**
		 * Cache.
		 */
		CacheType("cache"),
		/**
		 * Cave.
		 */
		CaveType("cave"),
		/**
		 * Grove.
		 */
		GroveType("grove"),
		/**
		 * Orchard.
		 */
		OrchardType("orchard"),
		/**
		 * Field.
		 */
		FieldType("field"),
		/**
		 * Meadow.
		 */
		MeadowType("meadow"),
		/**
		 * Mine.
		 */
		MineType("mine"),
		/**
		 * Mineral.
		 */
		MineralType("mineral"),
		/**
		 * Shrub.
		 */
		ShrubType("shrub"),
		/**
		 * Stone.
		 */
		StoneType("stone");
		/**
		 * The tag.
		 */
		public final String tag;

		/**
		 * Constructor.
		 *
		 * @param tagString The tag.
		 */
		private HarvestableType(final String tagString) {
			tag = tagString;
		}
	}

	/**
	 * Mapping from tags to enum-tags.
	 */
	private static final Map<String, HarvestableType> MAP = new HashMap<>(
			HarvestableType.values().length);
	/**
	 * List of supported tags.
	 */
	private static final Set<String> SUPP_TAGS;
	static {
		final Set<String> suppTagsTemp = new ArraySet<>();
		for (final HarvestableType mt : HarvestableType.values()) {
			MAP.put(mt.tag, mt);
			suppTagsTemp.add(mt.tag);
		}
		final Set<String> temp = Collections.unmodifiableSet(suppTagsTemp);
		assert temp != null;
		SUPP_TAGS = temp;
	}

	/**
	 * @param tag a tag
	 * @return whether we support it
	 */
	@Override
	public boolean isSupportedTag(@Nullable final String tag) {
		return SUPP_TAGS.contains(tag);
	}

	/**
	 * @param element the XML element to parse
	 * @param stream the stream to read more elements from
	 * @param players the collection of players
	 * @param warner the Warning instance to use for warnings
	 * @param idFactory the ID factory to use to generate IDs
	 * @return the parsed resource
	 * @throws SPFormatException on SP format problems
	 */
	@Override
	public HarvestableFixture read(final StartElement element,
			final IteratorWrapper<XMLEvent> stream,
			final PlayerCollection players, final Warning warner,
			final IDFactory idFactory) throws SPFormatException {
		requireTag(element, "battlefield", "cache", "cave", "grove", "orchard",
				"field", "meadow", "mine", "mineral", "shrub", "stone");
		final int idNum = getOrGenerateID(element, warner, idFactory);
		// ESCA-JAVA0177:
		final HarvestableFixture retval; // NOPMD
		switch (MAP.get(element.getName().getLocalPart())) {
		case BattlefieldType:
			retval = new Battlefield(getDC(element), idNum);
			break;
		case CacheType:
			retval = new CacheFixture(getParameter(element, KIND_PARAM),
					getParameter(element, "contents"), idNum);
			break;
		case CaveType:
			retval = new Cave(getDC(element), idNum);
			break;
		case FieldType:
			retval = createMeadow(element, true, idNum, warner);
			break;
		case GroveType:
			retval = createGrove(element, false, idNum, warner);
			break;
		case MeadowType:
			retval = createMeadow(element, false, idNum, warner);
			break;
		case MineType:
			retval = new Mine(getParameterWithDeprecatedForm(element,
					KIND_PARAM, "product", warner),
					TownStatus.parseTownStatus(getParameter(element,
							STATUS_PARAM)), idNum);
			break;
		case MineralType:
			retval = new MineralVein(getParameterWithDeprecatedForm(element,
					KIND_PARAM, "mineral", warner),
					Boolean.parseBoolean(getParameter(element, "exposed")),
					getDC(element), idNum);
			break;
		case OrchardType:
			retval = createGrove(element, true, idNum, warner);
			break;
		case ShrubType:
			retval = new Shrub(getParameterWithDeprecatedForm(element,
					KIND_PARAM, "shrub", warner), idNum);
			break;
		case StoneType:
			retval = new StoneDeposit(
					StoneKind.parseStoneKind(getParameterWithDeprecatedForm(
							element, KIND_PARAM, "stone", warner)),
					getDC(element), idNum);
			break;
		default:
			throw new IllegalArgumentException("Shouldn't get here");
		}
		spinUntilEnd(assertNotNullQName(element.getName()), stream);
		if (retval instanceof HasImage) {
			((HasImage) retval).setImage(getParameter(element, "image", ""));
		}
		return retval;
	}

	/**
	 * @param element a tag
	 * @return the value of its 'dc' property.
	 * @throws SPFormatException on SP format problem
	 */
	private static int getDC(final StartElement element)
			throws SPFormatException {
		return Integer.parseInt(getParameter(element, "dc"));
	}

	/**
	 * Create a Meadow, to reduce code duplication between 'field' and 'meadow'
	 * cases.
	 *
	 * @param element the tag we're parsing
	 * @param field whether this is a field (meadow otherwise)
	 * @param idNum the ID number parsed or generated
	 * @param warner the Warning instance to use for warnings
	 * @return the parsed Meadow object.
	 * @throws SPFormatException on SP format problems
	 */
	private static Meadow createMeadow(final StartElement element,
			final boolean field, final int idNum, final Warning warner)
			throws SPFormatException {
		if (!hasParameter(element, STATUS_PARAM)) {
			final String local = element.getName().getLocalPart();
			assert local != null;
			warner.warn(new MissingPropertyException(local, STATUS_PARAM,
					element.getLocation().getLineNumber()));
		}
		return new Meadow(getParameter(element, KIND_PARAM), field,
				Boolean.parseBoolean(getParameter(element, CULTIVATED_PARAM)),
				idNum, FieldStatus.parse(getParameter(element, STATUS_PARAM,
						FieldStatus.random(idNum).toString())));
	}

	/**
	 * Create a Grove, to reduce code duplication between 'grove' and 'orchard'
	 * cases.
	 *
	 * @param element the tag we're parsing
	 * @param orchard whether this is an orchard, a grove otherwise
	 * @param idNum the ID number parsed or generated
	 * @param warner the Warning instance to use for warnings
	 * @return the parsed Grove object
	 * @throws SPFormatException on SP format problems
	 */
	private static Grove createGrove(final StartElement element,
			final boolean orchard, final int idNum, final Warning warner)
			throws SPFormatException {
		return new Grove(orchard, isCultivated(element, warner),
				getParameterWithDeprecatedForm(element, KIND_PARAM, "tree",
						warner), idNum);
	}

	/**
	 * @param element a tag representing a grove or orchard
	 * @param warner the Warning instance to use
	 * @return whether the grove or orchard is cultivated
	 * @throws SPFormatException on SP format problems: use of 'wild' if
	 *         warnings are fatal, or if both properties are missing.
	 */
	private static boolean isCultivated(final StartElement element,
			final Warning warner) throws SPFormatException {
		if (hasParameter(element, CULTIVATED_PARAM)) {
			return Boolean
					.parseBoolean(getParameter(element, CULTIVATED_PARAM)); // NOPMD
		} else {
			final String local = element.getName().getLocalPart();
			assert local != null;
			if (hasParameter(element, "wild")) {
				warner.warn(new DeprecatedPropertyException(local, "wild",
						CULTIVATED_PARAM, element.getLocation().getLineNumber()));
				return !Boolean.parseBoolean(getParameter(element, "wild"));
			} else {
				throw new MissingPropertyException(local, CULTIVATED_PARAM,
						element.getLocation().getLineNumber());
			}
		}
	}

	/**
	 * Write an object to a stream. TODO: Some way of simplifying this?
	 *
	 * @param out The stream to write to.
	 * @param obj The object to write.
	 * @param indent The current indentation level.
	 * @throws IOException on I/O error
	 */
	@Override
	public void write(final Writer out, final HarvestableFixture obj,
			final int indent) throws IOException {
		out.append(indent(indent));
		if (obj instanceof CacheFixture) {
			out.append("<cache kind=\"");
			out.append(((CacheFixture) obj).getKind());
			out.append("\" contents=\"");
			out.append(((CacheFixture) obj).getContents());
		} else if (obj instanceof Meadow) {
			out.append('<');
			out.append(getMeadowTag((Meadow) obj));
			out.append(" kind=\"");
			out.append(((Meadow) obj).getKind());
			out.append("\" cultivated=\"");
			out.append(Boolean.toString(((Meadow) obj).isCultivated()));
			out.append("\" status=\"");
			out.append(((Meadow) obj).getStatus().toString());
		} else if (obj instanceof Grove) {
			out.append('<');
			out.append(getGroveTag((Grove) obj));
			out.append(" cultivated=\"");
			out.append(Boolean.toString(((Grove) obj).isCultivated()));
			out.append("\" kind=\"");
			out.append(((Grove) obj).getKind());
		} else if (obj instanceof Mine) {
			out.append("<mine kind=\"");
			out.append(((Mine) obj).getKind());
			out.append("\" status=\"");
			out.append(((Mine) obj).getStatus().toString());
		} else if (obj instanceof MineralVein) {
			out.append("<mineral kind=\"");
			out.append(((MineralVein) obj).getKind());
			out.append("\" exposed=\"");
			out.append(Boolean.toString(((MineralVein) obj).isExposed()));
			out.append("\" dc=\"");
			out.append(Integer.toString(((IEvent) obj).getDC()));
		} else if (obj instanceof Shrub) {
			out.append("<shrub kind=\"");
			out.append(((Shrub) obj).getKind());
		} else if (obj instanceof StoneDeposit) {
			out.append("<stone kind=\"");
			out.append(((StoneDeposit) obj).stone().toString());
			out.append("\" dc=\"");
			out.append(Integer.toString(((StoneDeposit) obj).getDC()));
		} else if (obj instanceof IEvent) {
			writeSimpleEvent(out, (IEvent) obj);
		}
		out.append("\" id=\"");
		out.append(Integer.toString(obj.getID()));
		out.append('"');
		if (obj instanceof HasImage) {
			out.append(imageXML((HasImage) obj));
		}
		out.append(" />\n");
	}

	/**
	 * @param meadow a meadow or field
	 * @return the proper tag for it
	 */
	private static String getMeadowTag(final Meadow meadow) {
		return meadow.isField() ? "field" : "meadow";
	}

	/**
	 * @param grove a grove or orchard
	 * @return the proper tag for it
	 */
	private static String getGroveTag(final Grove grove) {
		return grove.isOrchard() ? "orchard" : "grove";
	}

	/**
	 * Serialize a very simple Event.
	 *
	 * @param out the stream to write (most of) it to
	 * @param event a simple (DC- and ID-only) IEvent
	 * @throws IOException on I/O error
	 */
	private static void writeSimpleEvent(final Writer out, final IEvent event)
			throws IOException {
		if (event instanceof Battlefield) {
			out.append("<battlefield ");
		} else if (event instanceof Cave) {
			out.append("<cave ");
		} else {
			throw new IllegalStateException("Unhandled IEvent subtype");
		}
		out.append("dc=\"");
		out.append(Integer.toString(event.getDC()));
	}
	/**
	 * @return a String representation of the object
	 */
	@Override
	public String toString() {
		return "CompactResourceReader";
	}
}
