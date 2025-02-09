package report;

import lovelace.util.LovelaceLogger;
import org.jetbrains.annotations.Nullable;
import org.javatuples.Pair;

import lovelace.util.DelayedRemovalMap;

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

/**
 * Produces reports based on maps.
 */
public final class ReportGenerator {
	private ReportGenerator() {
	}

	/**
	 * Produces sub-reports, appending them to the buffer and calling
	 * {@link DelayedRemovalMap#coalesce} on the fixtures collection after each.
	 */
	private static void createSubReports(final StringBuilder builder,
	                                     final DelayedRemovalMap<Integer, Pair<Point, IFixture>> fixtures, final IMapNG map,
	                                     final Player player, final IReportGenerator<?>... generators) throws IOException {
		for (final IReportGenerator<?> generator : generators) {
			generator.produce(fixtures, map, builder::append);
			fixtures.coalesce();
		}
	}

	private static <Type> int compareToEqual(final Type one, final Type two) {
		return 0;
	}

	public static String createReport(final IMapNG map, final ICLIHelper cli) throws IOException {
		return createReport(map, cli, map.getCurrentPlayer());
	}

	/**
	 * Create the report for the given player based on the given map.
	 *
	 * TODO: Consider generating Markdown instead of HTML. OTOH, we'd have
	 * to keep a list nesting level parameter or something.
	 */
	public static String createReport(final IMapNG map, final ICLIHelper cli, final Player player)
			throws IOException {
		final MapDimensions dimensions = map.getDimensions();
		final StringBuilder builder = new StringBuilder();
		builder.append("""
				<!DOCTYPE html>
				<html>
				<head><title>Strategic Primer map summary report</title></head>
				<body>
				""");
		final DelayedRemovalMap<Integer, Pair<Point, IFixture>> fixtures =
			ReportGeneratorHelper.getFixtures(map);
		final @Nullable Point hq = ReportGeneratorHelper.findHQ(map, player);
		final int currentTurn = map.getCurrentTurn();
		createSubReports(builder, fixtures, map, player,
			new FortressReportGenerator(player, dimensions, currentTurn, hq),
			new UnitReportGenerator(player, dimensions, currentTurn, hq),
			new TextReportGenerator(dimensions, hq),
			new TownReportGenerator(player, dimensions, currentTurn, hq),
			new FortressMemberReportGenerator(player, dimensions, currentTurn, hq),
			new AdventureReportGenerator(player, dimensions, hq),
			new ExplorableReportGenerator(dimensions, hq),
			new HarvestableReportGenerator(dimensions, hq),
			new AnimalReportGenerator(dimensions, currentTurn, hq),
			new VillageReportGenerator(player, dimensions, hq),
			new ImmortalsReportGenerator(dimensions, hq));
		builder.append("""
				</body>
				</html>
				""");
		for (final Pair<Point, IFixture> pair : fixtures.values()) {
			final Point loc = pair.getValue0();
			final IFixture fixture = pair.getValue1();
			if (fixture.getId() < 0) {
				continue;
			} else if (fixture instanceof Ground || fixture instanceof TerrainFixture) {
				fixtures.remove(fixture.getId());
					continue;
			}
			LovelaceLogger.warning("Unhandled fixture:\t%s (ID #%d)",
				fixture, fixture.getId());
		}
		return builder.toString();
	}
}
