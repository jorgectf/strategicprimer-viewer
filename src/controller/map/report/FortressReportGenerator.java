package controller.map.report;

import java.util.EnumSet;
import java.util.Set;

import model.map.IFixture;
import model.map.Player;
import model.map.Point;
import model.map.River;
import model.map.Tile;
import model.map.TileCollection;
import model.map.TileFixture;
import model.map.fixtures.mobile.Unit;
import model.map.fixtures.terrain.Forest;
import model.map.fixtures.terrain.Hill;
import model.map.fixtures.terrain.Mountain;
import model.map.fixtures.terrain.Oasis;
import model.map.fixtures.towns.Fortress;
import util.IntMap;
import util.Pair;

/**
 * A report generator for fortresses.
 *
 * @author Jonathan Lovelace
 */
public class FortressReportGenerator extends AbstractReportGenerator<Fortress> {
	/**
	 * Instance we use.
	 */
	private final UnitReportGenerator urg = new UnitReportGenerator();
	/**
	 * All fixtures referred to in this report are removed from the collection.
	 *
	 * @param fixtures the set of fixtures
	 * @param currentPlayer the player for whom the report is being produced
	 * @param tiles the tiles in the map (needed to get terrain information)
	 * @return the part of the report dealing with fortresses
	 */
	@Override
	public String produce(final IntMap<Pair<Point, IFixture>> fixtures,
			final TileCollection tiles, final Player currentPlayer) {
		final StringBuilder builder = new StringBuilder(
				"<h4>Fortresses in the map:</h4>\n");
		boolean anyforts = false;
		for (final Pair<Point, IFixture> pair : fixtures.values()) {
			if (pair.second() instanceof Fortress) {
				anyforts = true;
				builder.append(produce(fixtures, tiles, currentPlayer, (Fortress) pair.second(), pair.first()));
			}
		}
		return anyforts ? builder.toString() : "";
	}

	/**
	 * @param tile a tile
	 * @param fixtures the set of fixtures, so we can schedule the removal the
	 *        terrain fixtures from it
	 * @return a String describing the terrain on it
	 */
	private static String getTerrain(final Tile tile,
			final IntMap<Pair<Point, IFixture>> fixtures) {
		final StringBuilder builder = new StringBuilder("Surrounding terrain: ")
				.append(tile.getTerrain().toXML().replace('_', ' '));
		boolean hasForest = false;
		for (final TileFixture fix : tile) {
			if (fix instanceof Forest) {
				if (!hasForest) {
					hasForest = true;
					builder.append(", forested with "
							+ ((Forest) fix).getKind());
				}
				fixtures.remove(Integer.valueOf(fix.getID()));
			} else if (fix instanceof Mountain) {
				builder.append(", mountainous");
				fixtures.remove(Integer.valueOf(fix.getID()));
			} else if (fix instanceof Hill) {
				builder.append(", hilly");
				fixtures.remove(Integer.valueOf(fix.getID()));
			} else if (fix instanceof Oasis) {
				builder.append(", with a nearby oasis");
				fixtures.remove(Integer.valueOf(fix.getID()));
			}
		}
		return builder.toString();
	}

	/**
	 * @param rivers a collection of rivers
	 * @return an equivalent string.
	 */
	private static String riversToString(final Set<River> rivers) {
		final StringBuilder builder = new StringBuilder();
		if (rivers.contains(River.Lake)) {
			builder.append("<li>There is a nearby lake.</li>\n");
			rivers.remove(River.Lake);
		}
		if (!rivers.isEmpty()) {
			builder.append(OPEN_LIST_ITEM)
					.append("There is a river on the tile, flowing through the following borders: ");
			boolean first = true;
			for (final River river : rivers) {
				if (first) {
					first = false;
				} else {
					builder.append(", ");
				}
				builder.append(river.getDescription());
			}
			builder.append(CLOSE_LIST_ITEM);
		}
		return builder.toString();
	}
	/**
	 * All fixtures referred to in this report are removed from the collection.
	 * @param item the fortress to report on
	 * @param loc its location
	 * @param fixtures the set of fixtures
	 * @param tiles the tiles in the map (needed to get terrain information)
	 * @param currentPlayer the player for whom the report is being produced
	 * @return the part of the report dealing with fortresses
	 */
	@Override
	public String produce(final IntMap<Pair<Point, IFixture>> fixtures,
			final TileCollection tiles, final Player currentPlayer, final Fortress item, final Point loc) {
		final StringBuilder builder = new StringBuilder("<h5>Fortress ")
				.append(item.getName()).append(" belonging to ")
				.append(item.getOwner().toString()).append("</h5>\n")
				.append(OPEN_LIST).append(OPEN_LIST_ITEM).append("Located at ")
				.append(loc).append(CLOSE_LIST_ITEM).append(OPEN_LIST_ITEM);
		final Tile tile = tiles.getTile(loc);
		builder.append(getTerrain(tile, fixtures)).append(CLOSE_LIST_ITEM);
		if (tile.hasRiver()) {
			builder.append(riversToString(EnumSet.copyOf(tile.getRivers().getRivers())));
		}
		if (item.iterator().hasNext()) {
			builder.append("Units on the tile:\n").append(OPEN_LIST);
			for (final Unit unit : item) {
				builder.append(OPEN_LIST_ITEM)
						.append(urg.produce(fixtures, tiles, currentPlayer, unit, loc))
						.append(CLOSE_LIST_ITEM);
			}
			builder.append(CLOSE_LIST).append(CLOSE_LIST_ITEM);
		}
		builder.append(CLOSE_LIST);
		fixtures.remove(Integer.valueOf(item.getID()));
		return builder.toString();
	}
}
