package controller.map.simplexml;

import java.util.HashMap;
import java.util.Map;

import util.EqualsAny;
import controller.map.SPFormatException;
import controller.map.simplexml.node.AbstractChildNode;
import controller.map.simplexml.node.AnimalNode;
import controller.map.simplexml.node.CacheNode;
import controller.map.simplexml.node.CentaurNode;
import controller.map.simplexml.node.DjinnNode;
import controller.map.simplexml.node.DragonNode;
import controller.map.simplexml.node.EventNode;
import controller.map.simplexml.node.FairyNode;
import controller.map.simplexml.node.ForestNode;
import controller.map.simplexml.node.FortressNode;
import controller.map.simplexml.node.GiantNode;
import controller.map.simplexml.node.GriffinNode;
import controller.map.simplexml.node.GroundNode;
import controller.map.simplexml.node.GroveNode;
import controller.map.simplexml.node.HillNode;
import controller.map.simplexml.node.MapNode;
import controller.map.simplexml.node.MeadowNode;
import controller.map.simplexml.node.MineNode;
import controller.map.simplexml.node.MinotaurNode;
import controller.map.simplexml.node.MountainNode;
import controller.map.simplexml.node.OasisNode;
import controller.map.simplexml.node.PlayerNode;
import controller.map.simplexml.node.RiverNode;
import controller.map.simplexml.node.SandbarNode;
import controller.map.simplexml.node.ShrubNode;
import controller.map.simplexml.node.SkippableNode;
import controller.map.simplexml.node.TextNode;
import controller.map.simplexml.node.TileNode;
import controller.map.simplexml.node.UnitNode;
import controller.map.simplexml.node.VillageNode;

/**
 * A class to create properly-typed Nodes (but *not* their contents) based on
 * the tags that represent them. TODO: Actually implement <include>---and that
 * will entail making this no longer Singleton.
 * 
 * @author Jonathan Lovelace
 * 
 */
public final class NodeFactory { // NOPMD
	/**
	 * The name for the property for what kind of event an event is. We create
	 * this property when setting up nodes for events that had their own unique
	 * tags.
	 */
	private static final String EVENT_KIND_PROP = "kind";

	/**
	 * Do not instantiate.
	 */
	private NodeFactory() {
		// Do nothing.
	}

	/**
	 * A mapping from XML tags to tag types.
	 */
	private static final Map<String, Tag> TAGS = new HashMap<String, Tag>();

	/**
	 * Set up a tag.
	 * 
	 * @param string
	 *            an XML tag
	 * @param tag
	 *            a corresponding tag category
	 */
	private static void addTag(final String string, final Tag tag) {
		TAGS.put(string, tag);
	}
	/**
	 * Tags we expect to use in the future; they are SkippableNodes for now and
	 * we'll warn if they're used.
	 */
	private static final String[] FUTURE = { "include", "worker", "explorer",
			"building", "resource", "animal", "changeset", "change",
			"move", "work", "discover" };
	/**
	 * Set up the mappings from tags to node types. And just in case we didn't
	 * remove a tag from FUTURE, we handle those before the tags we *do* handle.
	 */
	static {
		for (final String string : FUTURE) {
			addTag(string, Tag.Skippable);
		}
		addTag("map", Tag.Map);
		addTag("row", Tag.Skippable);
		addTag("tile", Tag.Tile);
		addTag("player", Tag.Player);
		addTag("fortress", Tag.Fortress);
		addTag("unit", Tag.Unit);
		addTag("river", Tag.River);
		addTag("lake", Tag.Lake);
		addTag("event", Tag.Event);
		addTag("battlefield", Tag.Battlefield);
		addTag("cave", Tag.Cave);
		addTag("city", Tag.City);
		addTag("fortification", Tag.Fortification);
		addTag("stone", Tag.Stone);
		addTag("mineral", Tag.Mineral);
		addTag("town", Tag.Town);
		addTag("forest", Tag.Forest);
		addTag("mountain", Tag.Mountain);
		addTag("ground", Tag.Ground);
		addTag("shrub", Tag.Shrub);
		addTag("oasis", Tag.Oasis);
		addTag("grove", Tag.Grove);
		addTag("orchard", Tag.Grove);
		addTag("mine", Tag.Mine);
		addTag("animal", Tag.Animal);
		addTag("field", Tag.Meadow);
		addTag("meadow", Tag.Meadow);
		addTag("hill", Tag.Hill);
		addTag("village", Tag.Village);
		addTag("cache", Tag.Cache);
		addTag("sandbar", Tag.Sandbar);
		addTag("text", Tag.Text);
		addTag("centaur", Tag.Centaur);
		addTag("fairy", Tag.Fairy);
		addTag("djinn", Tag.Djinn);
		addTag("giant", Tag.Giant);
		addTag("dragon", Tag.Dragon);
		addTag("griffin", Tag.Griffin);
		addTag("minotaur", Tag.Minotaur);
	}
	/**
	 * Create a Node from a tag using reflection.
	 * @param tag
	 *            the tag.
	 * @param line
	 *            the line of the file it's on ... just in case.
	 * @return a Node representing the tag, but not its contents yet.
	 * @throws SPFormatException
	 *             on unrecognized tag
	 * @throws IllegalAccessException thrown by reflection
	 * @throws InstantiationException thrown by reflection
	 */
	public static AbstractChildNode<?> createReflection(final String tag,
			final int line) throws SPFormatException, InstantiationException,
			IllegalAccessException {
		final Tag localtag = getTag(tag, line);
		final AbstractChildNode<?> node = localtag.getTagClass().newInstance();
		if (EqualsAny.equalsAny(localtag, Tag.Battlefield, Tag.Cave, Tag.City,
				Tag.Fortification, Tag.Mineral, Tag.Stone, Tag.Town)) {
			node.addProperty(EVENT_KIND_PROP, tag);
		} else if (Tag.Lake.equals(localtag)) {
			node.addProperty("direction", "lake");
		} else if (EqualsAny.equalsAny(localtag, Tag.Grove, Tag.Meadow)) {
			node.addProperty("tag", tag);
		}
		node.addProperty("line", Integer.toString(line));
		return node;
	}
	/**
	 * @param tag An XML tag
	 * @param line the line it occurs on
	 * @return the Tag object representing it
	 * @throws SPFormatException if there is no Tag object representing it
	 */
	protected static Tag getTag(final String tag, final int line)
			throws SPFormatException {
		if (!TAGS.containsKey(tag)) {
			throw new SPFormatException("Unknown tag " + tag, line);
		}
		return TAGS.get(tag);
	}
	/**
	 * Create a Node from a tag.
	 * 
	 * @param tag
	 *            the tag.
	 * @param line
	 *            the line of the file it's on ... just in case.
	 * @return a Node representing the tag, but not its contents yet.
	 * @throws SPFormatException
	 *             on unrecognized tag
	 */
	public static AbstractChildNode<?> create(final String tag, final int line) // NOPMD
			throws SPFormatException {
		// ESCA-JAVA0177:
		final AbstractChildNode<?> node; // NOPMD
		// ESCA-JAVA0040:
		switch (getTag(tag, line)) {
		case Battlefield:
			node = new EventNode();
			node.addProperty(EVENT_KIND_PROP, "battlefield");
			break;
		case Cave:
			node = new EventNode();
			node.addProperty(EVENT_KIND_PROP, "cave");
			break;
		case City:
			node = new EventNode();
			node.addProperty(EVENT_KIND_PROP, "city");
			break;
		case Event:
			node = new EventNode();
			break;
		case Fortification:
			node = new EventNode();
			node.addProperty(EVENT_KIND_PROP, "fortification");
			break;
		case Fortress:
			node = new FortressNode();
			break;
		case Lake:
			node = new RiverNode();
			node.addProperty("direction", "lake");
			break;
		case Map:
			node = new MapNode();
			break;
		case Mineral:
			node = new EventNode();
			node.addProperty(EVENT_KIND_PROP, "mineral");
			break;
		case Player:
			node = new PlayerNode();
			break;
		case River:
			node = new RiverNode();
			break;
		case Skippable:
			node = new SkippableNode(tag, line);
			break;
		case Stone:
			node = new EventNode();
			node.addProperty(EVENT_KIND_PROP, "stone");
			break;
		case Tile:
			node = new TileNode();
			break;
		case Town:
			node = new EventNode();
			node.addProperty(EVENT_KIND_PROP, "town");
			break;
		case Unit:
			node = new UnitNode();
			break;
		case Forest:
			node = new ForestNode();
			break;
		case Mountain:
			node = new MountainNode();
			break;
		case Ground:
			node = new GroundNode();
			break;
		case Shrub:
			node = new ShrubNode();
			break;
		case Oasis:
			node = new OasisNode();
			break;
		case Grove:
			node = new GroveNode();
			node.addProperty("tag", tag);
			break;
		case Mine:
			node = new MineNode();
			break;
		case Animal:
			node = new AnimalNode();
			break;
		case Meadow:
			node = new MeadowNode();
			node.addProperty("tag", tag);
			break;
		case Hill:
			node = new HillNode();
			break;
		case Village:
			node = new VillageNode();
			break;
		case Cache:
			node = new CacheNode();
			break;
		case Sandbar:
			node = new SandbarNode();
			break;
		case Text:
			node = new TextNode();
			break;
		case Centaur:
			node = new CentaurNode();
			break;
		case Fairy:
			node = new FairyNode();
			break;
		case Djinn:
			node = new DjinnNode();
			break;
		case Dragon:
			node = new DragonNode();
			break;
		case Giant:
			node = new GiantNode();
			break;
		case Griffin:
			node = new GriffinNode();
			break;
		case Minotaur:
			node = new MinotaurNode();
			break;
		default:
			throw new IllegalStateException("Shouldn't get here!");
		}
		node.addProperty("line", Integer.toString(line));
		return node;
	}
}
