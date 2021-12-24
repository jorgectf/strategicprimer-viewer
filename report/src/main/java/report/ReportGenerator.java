package report;

import org.jetbrains.annotations.Nullable;
import org.javatuples.Pair;
import java.util.logging.Logger;

import lovelace.util.DelayedRemovalMap;

import common.DistanceComparator;

import common.map.IFixture;
import common.map.Player;
import common.map.Point;
import common.map.MapDimensions;
import common.map.IMapNG;

import common.map.fixtures.TerrainFixture;
import common.map.fixtures.Ground;

import report.generators.AnimalReportGenerator;
import report.generators.VillageReportGenerator;
import report.generators.FortressReportGenerator;
import report.generators.HarvestableReportGenerator;
import report.generators.UnitReportGenerator;
import report.generators.IReportGenerator;
import report.generators.FortressMemberReportGenerator;
import report.generators.TownReportGenerator;
import report.generators.ExplorableReportGenerator;
import report.generators.ImmortalsReportGenerator;
import report.generators.TextReportGenerator;
import report.generators.AdventureReportGenerator;
import drivers.common.cli.ICLIHelper;
import java.io.IOException;
import java.util.Comparator;

/**
 * Produces reports based on maps.
 */
public final class ReportGenerator {
	/**
	 * A logger.
	 */
	private static final Logger LOGGER = Logger.getLogger(ReportGenerator.class.getName());

	/**
	 * Produces sub-reports, appending them to the buffer and calling
	 * {@link DelayedRemovalMap#coalesce} on the fixtures collection after each.
	 */
	private static void createSubReports(StringBuilder builder,
			DelayedRemovalMap<Integer, Pair<Point, IFixture>> fixtures, IMapNG map,
			Player player, IReportGenerator<?>... generators) throws IOException {
		for (IReportGenerator<?> generator : generators) {
			// TODO: change "IOConsumer" to "Consumer" in interface (and drop "throws IOException")
			generator.produce(fixtures, map, builder::append);
			fixtures.coalesce();
		}
	}

	private static <Type> int compareToEqual(Type one, Type two) {
		return 0;
	}

	public static String createReport(IMapNG map, ICLIHelper cli) throws IOException {
		return createReport(map, cli, map.getCurrentPlayer());
	}

	/**
	 * Create the report for the given player based on the given map.
	 *
	 * TODO: Consider generating Markdown instead of HTML. OTOH, we'd have
	 * to keep a list nesting level parameter or something.
	 */
	public static String createReport(IMapNG map, ICLIHelper cli, Player player) 
			throws IOException {
		MapDimensions dimensions = map.getDimensions();
		StringBuilder builder = new StringBuilder();
		builder.append("<!DOCTYPE html>").append(System.lineSeparator())
			.append("<html>").append(System.lineSeparator())
			.append("<head><title>Strategic Primer map summary report</title></head>")
			.append(System.lineSeparator()).append("<body>").append(System.lineSeparator());
		DelayedRemovalMap<Integer, Pair<Point, IFixture>> fixtures =
			ReportGeneratorHelper.getFixtures(map);
		@Nullable Point hq = ReportGeneratorHelper.findHQ(map, player);
		Comparator<Pair<Point, IFixture>> comparator;
		if (hq == null) {
			comparator = new PairComparator<>(ReportGenerator::compareToEqual,
				Comparator.comparing(IFixture::hashCode));
		} else {
			comparator = new PairComparator(new DistanceComparator(hq, dimensions),
				Comparator.comparing(IFixture::hashCode));
		}
		int currentTurn = map.getCurrentTurn();
		createSubReports(builder, fixtures, map, player,
			new FortressReportGenerator(comparator, player, dimensions, currentTurn, hq),
			new UnitReportGenerator(comparator, player, dimensions, currentTurn, hq),
			new TextReportGenerator(comparator, dimensions, hq),
			new TownReportGenerator(comparator, player, dimensions, currentTurn, hq),
			new FortressMemberReportGenerator(comparator, player, dimensions, currentTurn, hq),
			new AdventureReportGenerator(comparator, player, dimensions, hq),
			new ExplorableReportGenerator(comparator, player, dimensions, hq),
			new HarvestableReportGenerator(comparator, dimensions, hq),
			new AnimalReportGenerator(comparator, dimensions, currentTurn, hq),
			new VillageReportGenerator(comparator, player, dimensions, hq),
			new ImmortalsReportGenerator(comparator, dimensions, hq));
		builder.append("</body>").append(System.lineSeparator())
			.append("</html>").append(System.lineSeparator());
		for (Pair<Point, IFixture> pair : fixtures.values()) {
			Point loc = pair.getValue0();
			IFixture fixture = pair.getValue1();
			if (fixture.getId() < 0) {
				continue;
			} else if (fixture instanceof Ground || fixture instanceof TerrainFixture) {
				fixtures.remove(fixture.getId());
					continue;
			}
			LOGGER.warning(String.format("Unhandled fixture:\t%s (ID #%d)",
				fixture.toString(), fixture.getId()));
		}
		return builder.toString();
	}
}
