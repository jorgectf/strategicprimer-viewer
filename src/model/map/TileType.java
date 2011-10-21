package model.map;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Possible tile types.
 * 
 * @author Jonathan Lovelace
 * 
 */
public enum TileType {
	/**
	 * Tundra.
	 */
	Tundra(1, 2),
	/**
	 * Desert.
	 */
	Desert(1, 2),
	/**
	 * Mountain. Starting in version 2, this is represented as a plain, steppe,
	 * or desert plus a mountain on the tile.
	 */
	Mountain(1),
	/**
	 * Boreal forest. Starting in version 2, this is represented as a steppe
	 * plus a forest.
	 */
	BorealForest(1),
	/**
	 * Temperate forest. Starting in version 2, this is represented as a plain
	 * plus a forest.
	 */
	TemperateForest(1),
	/**
	 * Ocean.
	 */
	Ocean(1, 2),
	/**
	 * Plains.
	 */
	Plains(1, 2),
	/**
	 * Jungle.
	 */
	Jungle(1, 2),
	/**
	 * Steppe. This is like plains, but higher-latitude and colder. Beginning in
	 * version 2, a temperate forest is plains plus forest, and a boreal forest
	 * is steppe plus forest, while a mountain is either a desert, a plain, or a
	 * steppe plus a mountain.
	 */
	Steppe(2),
	/**
	 * Not visible.
	 */
	NotVisible(1, 2);
	/**
	 * The map versions that support the tile type as such. (For example,
	 * version 2 and later replace forests as a tile type with forests as
	 * something on the tile.)
	 */
	private final List<Integer> versions;
	/**
	 * A cache of the lists of types supported by particular versions.
	 */
	private static final Map<Integer, Set<TileType>> VALS_BY_VER = new HashMap<Integer, Set<TileType>>();
	/**
	 * @param ver a map version
	 * @return a list of all tile-types that version supports.
	 */
	public static Set<TileType> valuesForVersion(final int ver) {
		synchronized (VALS_BY_VER) {
			if (!VALS_BY_VER.containsKey(ver)) {
				final Set<TileType> set = EnumSet.noneOf(TileType.class);
				for (TileType type : values()) {
					if (type.isSupportedByVersion(ver)) {
						set.add(type);
					}
				}
				VALS_BY_VER.put(ver, set);
			}
		}
		return Collections.unmodifiableSet(VALS_BY_VER.get(ver));
	}
	/**
	 * Constructor.
	 * @param vers the map versions that support the tile type.
	 */
	private TileType(final Integer... vers) {
		versions = new ArrayList<Integer>(Arrays.asList(vers));
	}
	/**
	 * @param ver a map version
	 * @return whether that version supports this tile type.
	 */
	public boolean isSupportedByVersion(final int ver) {
		return versions.contains(ver);
	}
	/**
	 * The mapping from descriptive strings to tile types. Used to make
	 * multiple-return-points warnings go away.
	 */
	private static final Map<String, TileType> TILE_TYPE_MAP = new HashMap<String, TileType>(); // NOPMD

	static {
		TILE_TYPE_MAP.put("tundra", TileType.Tundra);
		TILE_TYPE_MAP.put("temperate_forest", TileType.TemperateForest);
		TILE_TYPE_MAP.put("boreal_forest", TileType.BorealForest);
		TILE_TYPE_MAP.put("ocean", TileType.Ocean);
		TILE_TYPE_MAP.put("desert", TileType.Desert);
		TILE_TYPE_MAP.put("plains", TileType.Plains);
		TILE_TYPE_MAP.put("jungle", TileType.Jungle);
		TILE_TYPE_MAP.put("mountain", TileType.Mountain);
		TILE_TYPE_MAP.put("steppe", TileType.Steppe);
	}

	/**
	 * Parse a tile terrain type.
	 * 
	 * @param string
	 *            A string describing the terrain
	 * 
	 * @return the terrain type
	 */
	public static TileType getTileType(final String string) {
		if (TILE_TYPE_MAP.containsKey(string)) {
			return TILE_TYPE_MAP.get(string);
		} // else
		throw new IllegalArgumentException("Unrecognized terrain type string");
	}
}
