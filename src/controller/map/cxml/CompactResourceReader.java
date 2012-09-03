package controller.map.cxml;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import model.map.PlayerCollection;
import model.map.fixtures.resources.BattlefieldEvent;
import model.map.fixtures.resources.CacheFixture;
import model.map.fixtures.resources.CaveEvent;
import model.map.fixtures.resources.FieldStatus;
import model.map.fixtures.resources.Grove;
import model.map.fixtures.resources.HarvestableFixture;
import model.map.fixtures.resources.Meadow;
import model.map.fixtures.resources.Mine;
import model.map.fixtures.resources.MineralEvent;
import model.map.fixtures.resources.Shrub;
import model.map.fixtures.resources.StoneEvent;
import model.map.fixtures.resources.StoneKind;
import model.map.fixtures.towns.TownStatus;
import util.ArraySet;
import util.IteratorWrapper;
import util.Warning;
import controller.map.DeprecatedPropertyException;
import controller.map.MissingParameterException;
import controller.map.SPFormatException;
import controller.map.misc.IDFactory;

/**
 * A reader for resource-bearing TileFixtures.
 * @author Jonathan Lovelace
 *
 */
public final class CompactResourceReader extends CompactReaderSuperclass implements CompactReader<HarvestableFixture> {
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
		 * @param tagString The tag.
		 */
		HarvestableType(final String tagString) {
			tag = tagString;
		}
	}
	/**
	 * Mapping from tags to enum-tags.
	 */
	private static final Map<String, HarvestableType> MAP = new HashMap<String, HarvestableType>(HarvestableType.values().length);
	/**
	 * List of supported tags.
	 */
	private static final Set<String> SUPP_TAGS;
	static {
		final Set<String> suppTagsTemp = new ArraySet<String>();
		for (HarvestableType mt : HarvestableType.values()) {
			MAP.put(mt.tag, mt);
			suppTagsTemp.add(mt.tag);
		}
		SUPP_TAGS = Collections.unmodifiableSet(suppTagsTemp);
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
	 * @param element the XML element to parse
	 * @param stream the stream to read more elements from
	 * @param players the collection of players
	 * @param warner the Warning instance to use for warnings
	 * @param idFactory the ID factory to use to generate IDs
	 * @return the parsed tile
	 * @throws SPFormatException on SP format problems
	 */
	@Override
	public HarvestableFixture read(final StartElement element,
			final IteratorWrapper<XMLEvent> stream, final PlayerCollection players,
			final Warning warner, final IDFactory idFactory) throws SPFormatException {
		requireTag(element, "battlefield", "cache", "cave", "grove", "orchard",
				"field", "meadow", "mine", "mineral", "shrub", "stone");
		// ESCA-JAVA0177:
		final HarvestableFixture retval; // NOPMD
		switch (MAP.get(element.getName().getLocalPart())) {
		case BattlefieldType:
			retval = new BattlefieldEvent(getDC(element), getOrGenerateID(
					element, warner, idFactory));
			retval.setFile(getFile(stream));
			spinUntilEnd(element.getName(), stream);
			break;
		case CacheType:
			retval = new CacheFixture(getParameter(element, "kind"),
					getParameter(element, "contents"), getOrGenerateID(element,
							warner, idFactory), getFile(stream));
			spinUntilEnd(element.getName(), stream);
			break;
		case CaveType:
			retval = new CaveEvent(getDC(element), getOrGenerateID(element, warner, idFactory));
			retval.setFile(getFile(stream));
			spinUntilEnd(element.getName(), stream);
			break;
		case FieldType:
			retval = createMeadow(element, true,
					getOrGenerateID(element, warner, idFactory),
					getFile(stream), warner);
			spinUntilEnd(element.getName(), stream);
			break;
		case GroveType:
			retval = createGrove(element, false,
					getOrGenerateID(element, warner, idFactory),
					getFile(stream), warner);
			spinUntilEnd(element.getName(), stream);
			break;
		case MeadowType:
			retval = createMeadow(element, false,
					getOrGenerateID(element, warner, idFactory),
					getFile(stream), warner);
			spinUntilEnd(element.getName(), stream);
			break;
		case MineType:
			retval = new Mine(
					getParameterWithDeprecatedForm(element, "kind", "product",
							warner),
					TownStatus.parseTownStatus(getParameter(element, "status")),
					getOrGenerateID(element, warner, idFactory),
					getFile(stream));
			spinUntilEnd(element.getName(), stream);
			break;
		case MineralType:
			retval = new MineralEvent(getParameterWithDeprecatedForm(
					element, "kind", "mineral", warner),
					Boolean.parseBoolean(getParameter(element, "exposed")),
					getDC(element), getOrGenerateID(element, warner, idFactory));
			retval.setFile(getFile(stream));
			spinUntilEnd(element.getName(), stream);
			break;
		case OrchardType:
			retval = createGrove(element, true,
					getOrGenerateID(element, warner, idFactory),
					getFile(stream), warner);
			spinUntilEnd(element.getName(), stream);
			break;
		case ShrubType:
			retval = new Shrub(getParameterWithDeprecatedForm(element,
					"kind", "shrub", warner), getOrGenerateID(element, warner,
					idFactory), getFile(stream));
			spinUntilEnd(element.getName(), stream);
			break;
		case StoneType:
			retval = new StoneEvent(
					StoneKind.parseStoneKind(getParameterWithDeprecatedForm(
							element, "kind", "stone", warner)), getDC(element),
					getOrGenerateID(element, warner, idFactory));
			retval.setFile(getFile(stream));
			spinUntilEnd(element.getName(), stream);
			break;
		default:
			throw new IllegalArgumentException("Shouldn't get here");
		}
		return retval;
	}
	/**
	 * @param element a tag
	 * @return the value of its 'dc' property.
	 * @throws SPFormatException on SP format problem
	 */
	private int getDC(final StartElement element) throws SPFormatException {
		return Integer.parseInt(getParameter(element, "dc"));
	}
	/**
	 * Create a Meadow, to reduce code duplication between 'field' and 'meadow' cases.
	 * @param element the tag we're parsing
	 * @param field whether this is a field (meadow otherwise)
	 * @param id the ID number parsed or generated
	 * @param file the file we're reading from
	 * @param warner the Warning instance to use for warnings
	 * @return the parsed Meadow object.
	 * @throws SPFormatException on SP format problems
	 */
	private Meadow createMeadow(final StartElement element,
			final boolean field, final int id, final String file, final Warning warner)
			throws SPFormatException {
		if (!hasParameter(element, "status")) {
			warner.warn(new MissingParameterException(element.getName()
					.getLocalPart(), "status", element.getLocation()
					.getLineNumber()));
		}
		return new Meadow(getParameter(element, "kind"), field,
				Boolean.parseBoolean(getParameter(element, "cultivated")), id,
				FieldStatus.parse(getParameter(element, "status", FieldStatus
						.random(id).toString())), file);
	}
	/**
	 * Create a Grove, to reduce code duplication between 'grove' and 'meadow' cases.
	 * @param element the tag we're parsing
	 * @param orchard whether this is an orchard, a grove otherwise
	 * @param id the ID number parsed or generated
	 * @param file the file we're reading from
	 * @param warner the Warning instance to use for warnings
	 * @return the parsed Grove object
	 * @throws SPFormatException on SP format problems
	 */
	private Grove createGrove(final StartElement element,
			final boolean orchard, final int id, final String file, final Warning warner)
			throws SPFormatException {
		return new Grove(
				orchard,
				isCultivated(element, warner),
				getParameterWithDeprecatedForm(element, "kind", "tree", warner),
				id, file);
	}
	/**
	 * @param element a tag representing a grove or orchard
	 * @param warner the Warning instance to use
	 * @return whether the grove or orchard is cultivated
	 * @throws SPFormatException on SP format problems: use of 'wild' if warnings are fatal, or if both properties are missing.
	 */
	private boolean isCultivated(final StartElement element, final Warning warner) throws SPFormatException {
		if (hasParameter(element, "cultivated")) {
			return Boolean.parseBoolean(getParameter(element, "cultivated")); // NOPMD
		} else {
			if (hasParameter(element, "wild")) {
				warner.warn(new DeprecatedPropertyException(element.getName()
						.getLocalPart(), "wild", "cultivated", element
						.getLocation().getLineNumber()));
				return !Boolean.parseBoolean(getParameter(element, "wild"));
			} else {
				throw new MissingParameterException(element.getName()
						.getLocalPart(), "cultivated", element.getLocation()
						.getLineNumber());
			}
		}
	}
}

