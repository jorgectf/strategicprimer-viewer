package controller.map.cxml;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import model.map.HasKind;
import model.map.PlayerCollection;
import model.map.fixtures.mobile.Animal;
import model.map.fixtures.mobile.Centaur;
import model.map.fixtures.mobile.Djinn;
import model.map.fixtures.mobile.Dragon;
import model.map.fixtures.mobile.Fairy;
import model.map.fixtures.mobile.Giant;
import model.map.fixtures.mobile.Griffin;
import model.map.fixtures.mobile.Minotaur;
import model.map.fixtures.mobile.MobileFixture;
import model.map.fixtures.mobile.Ogre;
import model.map.fixtures.mobile.Phoenix;
import model.map.fixtures.mobile.Simurgh;
import model.map.fixtures.mobile.Sphinx;
import model.map.fixtures.mobile.Troll;
import model.map.fixtures.mobile.Unit;
import util.ArraySet;
import util.IteratorWrapper;
import util.Warning;
import controller.map.SPFormatException;
import controller.map.misc.IDFactory;

/**
 * A reader for tiles, including rivers.
 * @author Jonathan Lovelace
 *
 */
public final class CompactMobileReader extends AbstractCompactReader implements CompactReader<MobileFixture> {
	/**
	 * Singleton.
	 */
	private CompactMobileReader() {
		// Singleton.
	}
	/**
	 * Enumeration of the types we know how to handle.
	 */
	private static enum MobileType {
		/**
		 * Animal.
		 */
		AnimalType("animal"),
		/**
		 * Centaur.
		 */
		CentaurType("centaur"),
		/**
		 * Djinn.
		 */
		DjinnType("djinn"),
		/**
		 * Dragon.
		 */
		DragonType("dragon"),
		/**
		 * Fairy.
		 */
		FairyType("fairy"),
		/**
		 * Giant.
		 */
		GiantType("giant"),
		/**
		 * Griffin.
		 */
		GriffinType("griffin"),
		/**
		 * Minotaur.
		 */
		MinotaurType("minotaur"),
		/**
		 * Ogre.
		 */
		OgreType("ogre"),
		/**
		 * Phoenix.
		 */
		PhoenixType("phoenix"),
		/**
		 * Simurgh.
		 */
		SimurghType("simurgh"),
		/**
		 *Sphinx.
		 */
		SphinxType("sphinx"),
		/**
		 * Troll.
		 */
		TrollType("troll"),
		/**
		 * Unit. (Handled by a different reader, but might get directed here by
		 * mistake, so we 'handle' it anyway.
		 */
		UnitType("unit");
		/**
		 * The tag.
		 */
		public final String tag;
		/**
		 * Constructor.
		 * @param tagString The tag.
		 */
		MobileType(final String tagString) {
			tag = tagString;
		}
	}
	/**
	 * Mapping from tags to enum-tags.
	 */
	private static final Map<String, MobileType> MAP = new HashMap<String, MobileType>(MobileType.values().length);
	/**
	 * List of supported tags.
	 */
	private static final Set<String> SUPP_TAGS;
	/**
	 * Map from types to tags. FIXME: This is brittle and doesn't work well with extensible classes.
	 */
	private static final Map<Class<? extends MobileFixture>, String> TAG_MAP;
	static {
		final Set<String> suppTagsTemp = new ArraySet<String>();
		for (MobileType mt : MobileType.values()) {
			MAP.put(mt.tag, mt);
			suppTagsTemp.add(mt.tag);
		}
		SUPP_TAGS = Collections.unmodifiableSet(suppTagsTemp);
		TAG_MAP = new HashMap<Class<? extends MobileFixture>, String>();
		TAG_MAP.put(Animal.class, "animal");
		TAG_MAP.put(Centaur.class, "centaur");
		TAG_MAP.put(Djinn.class, "djinn");
		TAG_MAP.put(Dragon.class, "dragon");
		TAG_MAP.put(Fairy.class, "fairy");
		TAG_MAP.put(Giant.class, "giant");
		TAG_MAP.put(Griffin.class, "griffin");
		TAG_MAP.put(Minotaur.class, "minotaur");
		TAG_MAP.put(Ogre.class, "ogre");
		TAG_MAP.put(Phoenix.class, "phoenix");
		TAG_MAP.put(Simurgh.class, "simurgh");
		TAG_MAP.put(Sphinx.class, "sphinx");
		TAG_MAP.put(Troll.class, "troll");
		TAG_MAP.put(Unit.class, "unit");
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
	 * Singleton object.
	 */
	public static final CompactMobileReader READER = new CompactMobileReader();
	/**
	 *
	 * @param element the XML element to parse
	 * @param stream the stream to read more elements from
	 * @param players the collection of players
	 * @param warner the Warning instance to use for warnings
	 * @param idFactory the ID factory to use to generate IDs
	 * @return the parsed tile
	 * @throws SPFormatException on SP format problems
	 */
	@Override
	public MobileFixture read(final StartElement element,
			final IteratorWrapper<XMLEvent> stream, final PlayerCollection players,
			final Warning warner, final IDFactory idFactory) throws SPFormatException {
		requireTag(element, "animal", "centaur", "djinn", "dragon", "fairy",
				"giant", "griffin", "minotaur", "ogre", "phoenix", "simurgh",
				"sphinx", "troll", "unit");
		// ESCA-JAVA0177:
		final MobileFixture retval; // NOPMD
		switch (MAP.get(element.getName().getLocalPart())) {
		case UnitType:
			return CompactUnitReader.READER.read(element, stream, players, // NOPMD
					warner, idFactory);
		case AnimalType:
			retval = createAnimal(element,
					getOrGenerateID(element, warner, idFactory),
					getFile(stream));
			break;
		case CentaurType:
			retval = new Centaur(getKind(element),
					getOrGenerateID(element, warner, idFactory),
					getFile(stream));
			break;
		case DjinnType:
			retval = new Djinn(getOrGenerateID(element, warner, idFactory),
					getFile(stream));
			break;
		case DragonType:
			retval = new Dragon(getKind(element),
					getOrGenerateID(element, warner, idFactory),
					getFile(stream));
			break;
		case FairyType:
			retval = new Fairy(getKind(element),
					getOrGenerateID(element, warner, idFactory),
					getFile(stream));
			break;
		case GiantType:
			retval = new Giant(getKind(element),
					getOrGenerateID(element, warner, idFactory),
					getFile(stream));
			break;
		case GriffinType:
			retval = new Griffin(
					getOrGenerateID(element, warner, idFactory),
					getFile(stream));
			break;
		case MinotaurType:
			retval = new Minotaur(getOrGenerateID(element, warner,
					idFactory), getFile(stream));
			break;
		case OgreType:
			retval = new Ogre(getOrGenerateID(element, warner, idFactory),
					getFile(stream));
			break;
		case PhoenixType:
			retval = new Phoenix(
					getOrGenerateID(element, warner, idFactory),
					getFile(stream));
			break;
		case SimurghType:
			retval = new Simurgh(getOrGenerateID(element, warner, idFactory),
					getFile(stream));
			break;
		case SphinxType:
			retval = new Sphinx(getOrGenerateID(element, warner, idFactory),
					getFile(stream));
			break;
		case TrollType:
			retval = new Troll(getOrGenerateID(element, warner, idFactory),
					getFile(stream));
			break;
		default:
			throw new IllegalArgumentException("Shouldn't get here");
		}
		spinUntilEnd(element.getName(), stream);
		return retval;
	}
	/**
	 * @param element the current tag
	 * @return the value of its 'kind' parameter
	 * @throws SPFormatException on SP format error---if the parameter is missing, e.g.
	 */
	private String getKind(final StartElement element) throws SPFormatException {
		return getParameter(element, "kind");
	}
	/**
	 * Create an animal.
	 * @param element the tag we're reading
	 * @param id the ID number to give it
	 * @param file the file it was loaded from
	 * @return the parsed animal
	 * @throws SPFormatException on SP format error
	 */
	private Animal createAnimal(final StartElement element, final int id, // NOPMD
			final String file) throws SPFormatException {
		return new Animal(getKind(element), hasParameter(element,
				"traces"), Boolean.parseBoolean(getParameter(element,
				"talking", "false")), id, file);
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
	public void write(final Writer out, final MobileFixture obj, final String file,
			final boolean inclusion, final int indent) throws IOException {
		if (obj instanceof Unit) {
			CompactUnitReader.READER.write(out, (Unit) obj, file, inclusion, indent);
		} else if (obj instanceof Animal) {
			out.append(indent(indent));
			out.append("<animal kind=\"");
			out.append(((Animal) obj).getKind());
			if (((Animal) obj).isTraces()) {
				out.append("\" traces=\"");
			}
			if (((Animal) obj).isTalking()) {
				out.append("\" talking=\"true");
			}
			out.append("\" id=\"");
			out.append(Integer.toString(obj.getID()));
			out.append("\" />\n");
		} else {
			out.append(indent(indent));
			out.append('<');
			out.append(TAG_MAP.get(obj.getClass()));
			if (obj instanceof HasKind) {
				out.append(" kind=\"");
				out.append(((HasKind) obj).getKind());
				out.append('"');
			}
			out.append(" id=\"");
			out.append(Integer.toString(obj.getID()));
			out.append("\" />\n");
		}
	}
}

