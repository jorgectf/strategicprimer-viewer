package report.generators.tabular;

import java.util.List;
import org.javatuples.Pair;
import org.jetbrains.annotations.Nullable;

import lovelace.util.DelayedRemovalMap;

import common.DistanceComparator;
import common.map.IFixture;
import common.map.TileFixture;
import common.map.MapDimensions;
import common.map.Point;
import common.map.HasKind;
import common.map.fixtures.resources.Grove;
import common.map.fixtures.resources.Meadow;
import common.map.fixtures.resources.Shrub;
import common.map.fixtures.terrain.Forest;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Collections;
import java.util.Map;
import java.text.NumberFormat;

/**
 * A tabular report generator for crops---forests, groves, orchards, fields,
 * meadows, and shrubs.
 */
public class CropTabularReportGenerator implements ITableGenerator</*Forest|Shrub|Meadow|Grove*/TileFixture> {
	@Override
	public boolean canHandle(final IFixture fixture) {
		return fixture instanceof Forest || fixture instanceof Shrub || fixture instanceof Meadow ||
				       fixture instanceof Grove;
	}

	private static final NumberFormat NUM_FORMAT = NumberFormat.getInstance();

	static {
		NUM_FORMAT.setMinimumFractionDigits(0);
		NUM_FORMAT.setMaximumFractionDigits(2);
	}

	/**
	 * Produce a {@link String} representation of a {@link Number}, limiting it to two decimal places.
	 */
	private static String truncatedNumberString(final Number number) {
		return NUM_FORMAT.format(number);
	}

	private final @Nullable Point hq;
	private final MapDimensions dimensions;
	private final Comparator<Point> distanceComparator;

	public CropTabularReportGenerator(final @Nullable Point hq, final MapDimensions dimensions) {
		this.hq = hq;
		this.dimensions = dimensions;
		if (hq == null) {
			distanceComparator = (one, two) -> 0;
		} else {
			distanceComparator = new DistanceComparator(hq, dimensions);
		}
	}

	/**
	 * The header row for the table.
	 */
	@Override
	public List<String> getHeaderRow() {
		return Arrays.asList("Distance", "Location", "Kind", "Size", "Size Unit", "Cultivation",
			"Status", "Crop");
	}

	/**
	 * The file-name to (by default) write this table to.
	 */
	@Override
	public String getTableName() {
		return "crops";
	}

	/**
	 * Create a table row representing the crop.
	 */
	@Override
	public List<List<String>> produce(
			final DelayedRemovalMap<Integer, Pair<Point, IFixture>> fixtures,
			/*Forest|Shrub|Meadow|Grove*/ final TileFixture item, final int key, final Point loc,
			final Map<Integer, Integer> parentMap) {
		final String kind;
		final String cultivation;
		final String status;
		final String size;
		final String sizeUnit;
		final String crop = ((HasKind) item).getKind();
		if (item instanceof Forest f) {
			kind = (f.isRows()) ? "rows" : "forest";
			cultivation = "---";
			status = "---";
			if (f.getAcres().doubleValue() > 0.0) {
				size = truncatedNumberString(f.getAcres());
				sizeUnit = "acres";
			} else {
				size = "unknown";
				sizeUnit = "---";
			}
		} else if (item instanceof Shrub s) {
			kind = "shrub";
			cultivation = "---";
			status = "---";
			if (s.getPopulation() > 0) {
				size = Integer.toString(s.getPopulation());
				sizeUnit = "plants";
			} else {
				size = "unknown";
				sizeUnit = "---";
			}
		} else if (item instanceof Meadow m) {
			kind = m.isField() ? "field" : "meadow";
			cultivation = m.isCultivated() ? "cultivated" : "wild";
			status = m.getStatus().toString();
			if (m.getAcres().doubleValue() > 0.0) {
				size = truncatedNumberString(m.getAcres());
				sizeUnit = "acres";
			} else {
				size = "unknown";
				sizeUnit = "---";
			}
		} else if (item instanceof Grove g) {
			kind = g.isOrchard() ? "orchard" : "grove";
			cultivation = g.isCultivated() ? "cultivated" : "wild";
			status = "---";
			if (g.getPopulation() > 0) {
				size = Integer.toString(g.getPopulation());
				sizeUnit = "trees";
			} else {
				size = "unknown";
				sizeUnit = "---";
			}
		} else {
			return Collections.emptyList();
		}
		fixtures.remove(key);
		return Collections.singletonList(Arrays.asList(distanceString(loc, hq, dimensions),
			locationString(loc), kind, size, sizeUnit, cultivation, status, crop));
	}

	/**
	 * Compare two Point-fixture pairs.
	 * @return
	 */
	@Override
	public Comparator<Pair<Point, TileFixture>> comparePairs() {
		return Comparator.<Pair<Point, TileFixture>, String>comparing(p -> ((HasKind) p.getValue1()).getKind())
			.thenComparing(Pair::getValue0, distanceComparator)
			.thenComparing(p -> p.getValue1().getClass().hashCode())
			.thenComparing(p -> p.getValue1().hashCode());
	}
}
