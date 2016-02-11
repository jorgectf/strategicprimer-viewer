package controller.map.report;

import controller.map.misc.IDFactory;
import controller.map.misc.IDFactoryFiller;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import model.map.DistanceComparator;
import model.map.FixtureIterable;
import model.map.HasOwner;
import model.map.IFixture;
import model.map.IMapNG;
import model.map.Player;
import model.map.Point;
import model.map.PointFactory;
import model.map.TileFixture;
import model.map.fixtures.mobile.Unit;
import model.map.fixtures.terrain.Hill;
import model.map.fixtures.terrain.Oasis;
import model.map.fixtures.terrain.Sandbar;
import model.map.fixtures.towns.Fortress;
import model.report.IReportNode;
import model.report.RootReportNode;
import org.eclipse.jdt.annotation.NonNull;
import util.DelayedRemovalMap;
import util.IntMap;
import util.NullCleaner;
import util.Pair;
import util.PairComparator;

/**
 * A class to produce a report based on a map for a player.
 *
 * TODO: Use an IR for lists, producing "" if empty, to simplify these methods!
 *
 * This is part of the Strategic Primer assistive programs suite developed by Jonathan
 * Lovelace.
 *
 * Copyright (C) 2013-2015 Jonathan Lovelace
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of version 3 of the GNU General Public License as published by the Free Software
 * Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this
 * program. If not, see
 * <a href="http://www.gnu.org/licenses/">http://www.gnu.org/licenses/</a>.
 *
 * @author Jonathan Lovelace
 */
@SuppressWarnings("UtilityClassCanBeEnum")
public final class ReportGenerator {
	/**
	 * No non-static members anymore.
	 */
	private ReportGenerator() {
		// So don't instantiate.
	}

	/**
	 * A simple comparator for fixtures.
	 */
	private static final Comparator<@NonNull IFixture> SIMPLE_COMPARATOR =
			(firstFixture, secondFixture) -> {
				if (firstFixture.equals(secondFixture)) {
					return 0;
				} else {
					if (firstFixture.hashCode() > secondFixture.hashCode()) {
						return 1;
					} else if (firstFixture.hashCode() == secondFixture.hashCode()) {
						return 0;
					} else {
						return -1;
					}
				}
			};

	/**
	 * @param map    a map
	 * @param player a player
	 * @return the location of that player's HQ, or another of that player's
	 * fortresses if
	 * not found, (-1, -1) if none found
	 */
	@SuppressWarnings("IfStatementWithIdenticalBranches")
	private static Point findHQ(final IMapNG map, final Player player) {
		Point retval = PointFactory.point(-1, -1);
		for (final Point location : map.locations()) {
			for (final TileFixture fixture : map.getOtherFixtures(
					NullCleaner.assertNotNull(location))) {
				if ((fixture instanceof Fortress) &&
							((Fortress) fixture).getOwner().equals(player)) {
					if ("HQ".equals(((Fortress) fixture).getName())) {
						return location;
					} else if ((location.row >= 0) && (retval.row == -1)) {
						retval = location;
					}
				}
			}
		}
		return retval;
	}

	/**
	 * @param map the map to base the report on
	 * @return the report, in HTML, as a String
	 */
	public static String createReport(final IMapNG map) {
		// The full report for the world map, as of turn 11, is 8 megs. So we
		// make a 10 meg buffer.
		final StringBuilder builder = new StringBuilder(10485760)
											  .append("<html>\n");
		builder.append("<head><title>Strategic Primer map ").append(
				"summary report</title></head>\n");
		builder.append("<body>");
		final DelayedRemovalMap<Integer, Pair<Point, IFixture>> fixtures =
				getFixtures(map);
		final Player player = map.getCurrentPlayer();
		final Comparator<@NonNull Pair<@NonNull Point, @NonNull IFixture>> comparator =
				new PairComparator<>(new DistanceComparator(findHQ(map, player)),
											SIMPLE_COMPARATOR);
		builder.append(new FortressReportGenerator(comparator).produce(fixtures, map,
				player));
		fixtures.coalesce();
		builder.append(new UnitReportGenerator(comparator).produce(fixtures, map,
				player));
		fixtures.coalesce();
		builder.append(new TextReportGenerator(comparator).produce(fixtures, map,
				player));
		fixtures.coalesce();
		builder.append(new TownReportGenerator(comparator).produce(fixtures, map,
				player));
		fixtures.coalesce();
		builder.append(new FortressMemberReportGenerator(comparator)
				               .produce(fixtures, map, player));
		fixtures.coalesce();
		builder.append(new ExplorableReportGenerator(comparator).produce(fixtures, map,
				player));
		fixtures.coalesce();
		builder.append(new HarvestableReportGenerator(comparator).produce(fixtures,
				map, player));
		fixtures.coalesce();
		builder.append(new AnimalReportGenerator(comparator).produce(fixtures, map,
				player));
		fixtures.coalesce();
		builder.append(new VillageReportGenerator(comparator).produce(fixtures, map,
				player));
		fixtures.coalesce();
		builder.append(new ImmortalsReportGenerator(comparator).produce(fixtures, map,
				player));
		fixtures.coalesce();
		builder.append("</body>\n</html>\n");
		for (final Pair<Point, IFixture> pair : fixtures.values()) {
			final IFixture fix = pair.second();
			if ((fix instanceof Hill) || (fix instanceof Sandbar)
						|| (fix instanceof Oasis)) {
				fixtures.remove(Integer.valueOf(fix.getID()));
				continue;
			}
			System.out.print("Unhandled fixture:\t");
			System.out.println(fix);
		}
		return NullCleaner.assertNotNull(builder.toString());
	}

	/**
	 * Creates a slightly abbreviated report, omitting the player's fortresses and units.
	 *
	 * @param map    the map to base the report on
	 * @param player the player to report on
	 * @return the report, in HTML, as a string.
	 */
	public static String createAbbreviatedReport(final IMapNG map,
												 final Player player) {
		// The full report for the world map, as of turn 11, is 8 megs. So we
		// make a 10 meg buffer.
		final StringBuilder builder = new StringBuilder(10485760)
											  .append("<html>\n<head>");
		builder.append("<title>Strategic Primer map summary ").append(
				"abridged report</title></head>\n");
		builder.append("<body>");
		final DelayedRemovalMap<Integer, Pair<Point, IFixture>> fixtures =
				getFixtures(map);
		final Comparator<@NonNull Pair<@NonNull Point, @NonNull IFixture>> comparator =
				new PairComparator<>(new DistanceComparator(findHQ(map, player)),
											SIMPLE_COMPARATOR);

		fixtures.values().stream().filter(pair -> ((pair.second() instanceof Unit) ||
														   (pair.second() instanceof
																	Fortress))
														  && player.equals(
				((HasOwner) pair.second()).getOwner()))
				.forEach(pair -> fixtures.remove(Integer.valueOf(pair.second().getID()
				)));
		fixtures.coalesce();
		builder.append(new FortressReportGenerator(comparator).produce(fixtures, map,
				player));
		fixtures.coalesce();
		builder.append(new UnitReportGenerator(comparator).produce(fixtures, map,
				player));
		fixtures.coalesce();
		builder.append(new TextReportGenerator(comparator).produce(fixtures, map,
				player));
		fixtures.coalesce();
		builder.append(new TownReportGenerator(comparator).produce(fixtures, map,
				player));
		fixtures.coalesce();
		builder.append(new ExplorableReportGenerator(comparator).produce(fixtures, map,
				player));
		fixtures.coalesce();
		builder.append(new HarvestableReportGenerator(comparator).produce(fixtures,
				map, player));
		fixtures.coalesce();
		builder.append(new FortressMemberReportGenerator(comparator)
				               .produce(fixtures, map, player));
		fixtures.coalesce();
		builder.append(new AnimalReportGenerator(comparator).produce(fixtures, map,
				player));
		fixtures.coalesce();
		builder.append(new VillageReportGenerator(comparator).produce(fixtures, map,
				player));
		fixtures.coalesce();
		builder.append(new ImmortalsReportGenerator(comparator).produce(fixtures, map,
				player));
		fixtures.coalesce();
		builder.append("</body>\n</html>\n");
		return NullCleaner.assertNotNull(builder.toString());
	}

	/**
	 * @param map the map to base the report on
	 * @return the report, in ReportIntermediateRepresentation
	 */
	public static IReportNode createReportIR(final IMapNG map) {
		final IReportNode retval = new RootReportNode(
																	"Strategic Primer " +
																			"map summary" +
																			" report");
		final DelayedRemovalMap<Integer, Pair<Point, IFixture>> fixtures =
				getFixtures(map);
		final Player player = map.getCurrentPlayer();
		final Comparator<@NonNull Pair<@NonNull Point, @NonNull IFixture>> comparator =
				new PairComparator<>(new DistanceComparator(findHQ(map, player)),
											SIMPLE_COMPARATOR);
		retval.add(new FortressReportGenerator(comparator).produceRIR(fixtures, map,
				player));
		fixtures.coalesce();
		retval.add(new UnitReportGenerator(comparator)
						   .produceRIR(fixtures, map, player));
		fixtures.coalesce();
		retval.add(new TextReportGenerator(comparator)
						   .produceRIR(fixtures, map, player));
		fixtures.coalesce();
		retval.add(new TownReportGenerator(comparator)
						   .produceRIR(fixtures, map, player));
		fixtures.coalesce();
		retval.add(new ExplorableReportGenerator(comparator).produceRIR(fixtures, map,
				player));
		fixtures.coalesce();
		retval.add(new HarvestableReportGenerator(comparator).produceRIR(fixtures, map,
				player));
		fixtures.coalesce();
		retval.add(new FortressMemberReportGenerator(comparator)
				           .produceRIR(fixtures, map, player));
		fixtures.coalesce();
		retval.add(new AnimalReportGenerator(comparator).produceRIR(fixtures, map,
				player));
		fixtures.coalesce();
		retval.add(new VillageReportGenerator(comparator).produceRIR(fixtures, map,
				player));
		fixtures.coalesce();
		retval.add(new ImmortalsReportGenerator(comparator).produceRIR(fixtures, map,
				player));
		fixtures.coalesce();
		return retval;
	}

	/**
	 * Creates a slightly abbreviated report, omitting the player's fortresses and units.
	 *
	 * @param map    the map to base the report on
	 * @param player the player to report on
	 * @return the report, in HTML, as a string.
	 */
	public static IReportNode createAbbreviatedReportIR(final IMapNG map,
															   final Player player) {
		final DelayedRemovalMap<Integer, Pair<Point, IFixture>> fixtures =
				getFixtures(map);
		final Comparator<@NonNull Pair<@NonNull Point, @NonNull IFixture>> comparator =
				new PairComparator<>(new DistanceComparator(findHQ(map, player)),
											SIMPLE_COMPARATOR);

		fixtures.values().stream().filter(pair -> ((pair.second() instanceof Unit) ||
														   (pair.second() instanceof
																	Fortress))
														  && player.equals(
				((HasOwner) pair.second()).getOwner()))
				.forEach(pair -> fixtures.remove(Integer.valueOf(pair.second().getID()
				)));
		fixtures.coalesce();
		final IReportNode retval =
				new RootReportNode("Strategic Primer map summary abbreviated report");
		retval.add(new FortressReportGenerator(comparator).produceRIR(fixtures, map,
				player));
		fixtures.coalesce();
		retval.add(new UnitReportGenerator(comparator)
						   .produceRIR(fixtures, map, player));
		fixtures.coalesce();
		retval.add(new TextReportGenerator(comparator)
						   .produceRIR(fixtures, map, player));
		fixtures.coalesce();
		retval.add(new TownReportGenerator(comparator)
						   .produceRIR(fixtures, map, player));
		fixtures.coalesce();
		retval.add(new ExplorableReportGenerator(comparator).produceRIR(fixtures, map,
				player));
		fixtures.coalesce();
		retval.add(new HarvestableReportGenerator(comparator).produceRIR(fixtures, map,
				player));
		fixtures.coalesce();
		retval.add(new FortressMemberReportGenerator(comparator)
				           .produceRIR(fixtures, map, player));
		fixtures.coalesce();
		retval.add(new AnimalReportGenerator(comparator).produceRIR(fixtures, map,
				player));
		fixtures.coalesce();
		retval.add(new VillageReportGenerator(comparator).produceRIR(fixtures, map,
				player));
		fixtures.coalesce();
		retval.add(new ImmortalsReportGenerator(comparator).produceRIR(fixtures, map,
				player));
		fixtures.coalesce();
		return retval;
	}

	/**
	 * @param map a map
	 * @return the fixtures in it, a mapping from their ID to a Pair of the fixture's
	 * location and the fixture itself.
	 */
	private static DelayedRemovalMap<Integer, Pair<Point, IFixture>> getFixtures(
																						final IMapNG map) {
		final DelayedRemovalMap<Integer, Pair<Point, IFixture>> retval =
				new IntMap<>();
		final IDFactory idf = IDFactoryFiller.createFactory(map);
		for (final Point point : map.locations()) {
			// Because neither Forests, Mountains, nor Ground have positive IDs,
			// we can ignore everything but the "other" fixtures.
			retval.putAll(getFixtures(map.streamOtherFixtures(point)).filter(fix -> fix instanceof TileFixture || fix.getID() > 0).collect(Collectors.toMap(fix -> {
				if (fix instanceof TileFixture) {
					return Integer.valueOf(idf.createID());
				} else {
					return Integer.valueOf(fix.getID());
				}
			}, fix -> Pair.of(point, fix))));
		}
		return retval;
	}

	/**
	 * @param stream a source of tile-fixtures
	 * @return all the tile-fixtures in it, recursively.
	 */
	private static Stream<IFixture> getFixtures(
														   final Stream<? extends IFixture> stream) {
		return stream.flatMap(fix -> {
			if (fix instanceof FixtureIterable) {
				return Stream.concat(Stream.of(fix), getFixtures(StreamSupport
						                                     .stream(((FixtureIterable<@NonNull ?>) fix)
								                                             .spliterator(),
								                                     false)));
			} else {
				return Stream.of(fix);
			}
		});
	}
}
