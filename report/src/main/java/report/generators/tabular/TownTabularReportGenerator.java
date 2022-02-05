package report.generators.tabular;

import org.javatuples.Pair;
import org.jetbrains.annotations.Nullable;

import lovelace.util.DelayedRemovalMap;

import common.DistanceComparator;
import common.map.Player;
import common.map.IFixture;
import common.map.MapDimensions;
import common.map.Point;
import common.map.fixtures.towns.AbstractTown;

import java.util.Comparator;
import java.util.Map;
import java.util.Arrays;
import java.util.Collections;

/**
 * A tabular report generator for towns.
 */
public class TownTabularReportGenerator implements ITableGenerator<AbstractTown> {
	@Override
	public Class<AbstractTown> narrowedClass() {
		return AbstractTown.class;
	}

	public TownTabularReportGenerator(final Player player, @Nullable final Point hq, final MapDimensions dimensions) {
		this.player = player;
		this.hq = hq;
		this.dimensions = dimensions;
		if (hq == null) {
			distanceComparator = (one, two) -> 0;
		} else {
			distanceComparator = new DistanceComparator(hq, dimensions);
		}
	}

	private final Player player;
	@Nullable
	private final Point hq;
	private final MapDimensions dimensions;

	/**
	 * The file-name to (by default) write this table to
	 */
	@Override
	public String getTableName() {
		return "towns";
	}

	/**
	 * The header row for this table.
	 */
	@Override
	public Iterable<String> getHeaderRow() {
		return Arrays.asList("Distance", "Location", "Owner", "Kind", "Size", "Status", "Name");
	}

	private final Comparator<Point> distanceComparator;

	/**
	 * Create a table row representing a town.
	 */
	@Override
	public Iterable<Iterable<String>> produce(
			final DelayedRemovalMap<Integer, Pair<Point, IFixture>> fixtures,
			final AbstractTown item, final int key, final Point loc, final Map<Integer, Integer> parentMap) {
		fixtures.remove(key);
		return Collections.singleton(Arrays.asList(distanceString(loc, hq, dimensions),
			locationString(loc), ownerString(player, item.getOwner()), item.getKind(),
			item.getTownSize().toString(), item.getStatus().toString(), item.getName()));
	}

	/**
	 * Compare two location-town pairs. We partially reimplement {@link
	 * TownComparators#compareTowns} because there we want to have all
	 * active communities together, and so on, while here we want all
	 * fortifications together, and so on.
	 */
	@Override
	public int comparePairs(final Pair<Point, AbstractTown> one, final Pair<Point, AbstractTown> two) {
		return Comparator.<Pair<Point, AbstractTown>, AbstractTown>comparing(Pair::getValue1, TownComparators::compareTownKind)
			.thenComparing(Pair::getValue0, distanceComparator)
			.thenComparing(Pair::getValue1, Comparator.comparing(AbstractTown::getTownSize)
					.thenComparing(AbstractTown::getStatus)
					.thenComparing(AbstractTown::getName))
			.compare(one, two);
	}
}
