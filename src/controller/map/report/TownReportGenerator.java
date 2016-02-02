package controller.map.report;

import controller.map.misc.TownComparator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import model.map.IFixture;
import model.map.IMapNG;
import model.map.Player;
import model.map.Point;
import model.map.fixtures.towns.AbstractTown;
import model.map.fixtures.towns.Fortress;
import model.map.fixtures.towns.ITownFixture;
import model.map.fixtures.towns.Village;
import model.report.EmptyReportNode;
import model.report.IReportNode;
import model.report.SectionListReportNode;
import model.report.SimpleReportNode;
import org.eclipse.jdt.annotation.NonNull;
import util.DelayedRemovalMap;
import util.NullCleaner;
import util.Pair;

/**
 * A report generator for towns.
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
public final class TownReportGenerator extends AbstractReportGenerator<ITownFixture> {
	/**
	 * @param comparator a comparator for pairs of Points and fixtures.
	 */
	public TownReportGenerator(final Comparator<@NonNull Pair<@NonNull Point, @NonNull
			                                                                          IFixture>> comparator) {
		super(comparator);
	}

	/**
	 * Produce the part of the report dealing with towns. Note that while this class
	 * specifies {@link ITownFixture}, this method ignores {@link Fortress}es and {@link
	 * Village}s. All fixtures referred to in this report are removed from the
	 * collection.
	 *
	 * TODO: Figure out some way to report what was found at any of the towns.
	 *
	 * @param fixtures      the set of fixtures
	 * @param map           ignored
	 * @param currentPlayer the player for whom the report is being produced
	 * @return the part of the report dealing with towns, sorted in a way I hope is
	 * helpful.
	 */
	@Override
	public String produce(
			                     final DelayedRemovalMap<Integer, Pair<Point, IFixture>>
					                     fixtures,
			                     final IMapNG map, final Player currentPlayer) {
		final Map<AbstractTown, Point> townLocs = new HashMap<>();
		fixtures.values().stream().filter(pair -> pair.second() instanceof AbstractTown)
				.forEach(
						pair -> townLocs.put((AbstractTown) pair.second(), pair.first
								                                                        ()));
		final List<AbstractTown> sorted = new ArrayList<>(townLocs.keySet());
		Collections.sort(sorted, new TownComparator());
		// FIXME: Within any given status, sort by distance from HQ
		final int len = 80 + (sorted.size() * 512);
		final StringBuilder builder = new StringBuilder(len);
		builder.append("<h4>Cities, towns, and/or fortifications you know about:</h4>\n");
		builder.append(OPEN_LIST);
		for (final AbstractTown town : sorted) {
			builder.append(OPEN_LIST_ITEM).append(
					produce(fixtures, map, currentPlayer, town, townLocs.get(town)))
					.append(CLOSE_LIST_ITEM);
		}
		builder.append(CLOSE_LIST);
		if (sorted.isEmpty()) {
			return "";
		} else {
			return NullCleaner.assertNotNull(builder.toString()); // NOPMD
		}
	}

	/**
	 * Produce the part of the report dealing with towns. Note that while this class
	 * specifies {@link ITownFixture}, this method ignores {@link Fortress}es and {@link
	 * Village}s. All fixtures referred to in this report are removed from the
	 * collection.
	 *
	 * TODO: Figure out some way to report what was found at any of the towns.
	 *
	 * @param fixtures      the set of fixtures
	 * @param map           ignored
	 * @param currentPlayer the player for whom the report is being produced
	 * @return the part of the report dealing with towns, sorted in a way I hope is
	 * helpful.
	 */
	@Override
	public IReportNode produceRIR(
			                                    final DelayedRemovalMap<Integer,
					                                                           Pair<Point, IFixture>> fixtures,
			                                    final IMapNG map,
			                                    final Player currentPlayer) {
		final Map<AbstractTown, Point> townLocs = fixtures.values().stream()
				                                          .filter(pair -> pair.second()
						                                                          instanceof AbstractTown)

				                                          .collect(Collectors
						                                                   .toMap(pair -> (AbstractTown) pair.second(),
								                                                   Pair::first));
		final List<AbstractTown> sorted = new ArrayList<>(townLocs.keySet());
		Collections.sort(sorted, new TownComparator());
		// FIXME: Within any given status, sort by distance from HQ
		final IReportNode retval = new SectionListReportNode(4,
				                                                           "Cities, towns, and/or fortifications you know about:");
		for (final AbstractTown town : sorted) {
			retval.add(produceRIR(fixtures, map, currentPlayer, town,
					townLocs.get(town)));
		}
		if (sorted.isEmpty()) {
			return EmptyReportNode.NULL_NODE; // NOPMD
		} else {
			return retval;
		}
	}

	/**
	 * Produce a report for a town. Handling of fortresses and villages is delegated to
	 * their dedicated report-generating classes. We remove the town from the set of
	 * fixtures.
	 *
	 * @param fixtures      the set of fixtures
	 * @param map           ignored
	 * @param item          the town to report on
	 * @param loc           its location
	 * @param currentPlayer the player for whom the report is being produced
	 * @return the sub-report dealing with the town.
	 */
	@Override
	public String produce(
			                     final DelayedRemovalMap<Integer, Pair<Point, IFixture>>
					                     fixtures,
			                     final IMapNG map, final Player currentPlayer,
			                     final ITownFixture item, final Point loc) {
		if (item instanceof Village) {
			return new VillageReportGenerator(pairComparator)
					       .produce(fixtures, map, // NOPMD
							       currentPlayer, (Village) item, loc);
		} else if (item instanceof Fortress) {
			return new FortressReportGenerator(pairComparator)
					       .produce(fixtures, map, // NOPMD
							       currentPlayer, (Fortress) item, loc);
		} else if (item instanceof AbstractTown) {
			fixtures.remove(Integer.valueOf(item.getID()));
			if (item.getOwner().isIndependent()) {
				return concat(atPoint(loc), item.getName(), //NOPMD
						", an independent ", item.size().toString(), " ", item
								                                                  .status()
								                                                  .toString(),
						" ",
						((AbstractTown) item).kind(), " ",
						distCalculator.distanceString(loc));
			} else {
				return concat(atPoint(loc), item.getName(), ", a ", // NOPMD
						item.size().toString(), " ", item.status().toString(),
						" ", ((AbstractTown) item).kind(), " allied with ",
						playerNameOrYou(item.getOwner()), " ",
						distCalculator.distanceString(loc));
			}
		} else {
			throw new IllegalStateException("Unhandled ITownFixture subclass");
		}
	}

	/**
	 * Produce a report for a town. Handling of fortresses and villages is delegated to
	 * their dedicated report-generating classes. We remove the town from the set of
	 * fixtures.
	 *
	 * @param fixtures      the set of fixtures
	 * @param map           ignored
	 * @param item          the town to report on
	 * @param loc           its location
	 * @param currentPlayer the player for whom the report is being produced
	 * @return the sub-report dealing with the town.
	 */
	@Override
	public IReportNode produceRIR(
			                                    final DelayedRemovalMap<Integer,
					                                                           Pair<Point, IFixture>> fixtures,
			                                    final IMapNG map,
			                                    final Player currentPlayer,
			                                    final ITownFixture item,
			                                    final Point loc) {
		if (item instanceof Village) {
			return new VillageReportGenerator(pairComparator)
					       .produceRIR(fixtures, // NOPMD
							       map, currentPlayer, (Village) item, loc);
		} else if (item instanceof Fortress) {
			return new FortressReportGenerator(pairComparator)
					       .produceRIR(fixtures, // NOPMD
							       map, currentPlayer, (Fortress) item, loc);
		} else if (item instanceof AbstractTown) {
			fixtures.remove(Integer.valueOf(item.getID()));
			if (item.getOwner().isIndependent()) {
				return new SimpleReportNode(loc, atPoint(loc), item.getName(),
						                           ", an independent ",
						                           item.size().toString(), " ", item
								                                                        .status()
								                                                        .toString(),
						                           " ",
						                           ((AbstractTown) item).kind(), " ",
						                           distCalculator.distanceString(loc));
			} else {
				return new SimpleReportNode(loc, atPoint(loc), item.getName(),
						                           ", a ", item.size().toString(), " ",
						                           item.status()
								                           .toString(), " ",
						                           ((AbstractTown) item).kind(),
						                           " allied with " + playerNameOrYou(
								                           item.getOwner()), " ",
						                           distCalculator.distanceString(loc));
			}
		} else {
			throw new IllegalStateException("Unhandled ITownFixture subclass");
		}
	}

	/**
	 * @return a String representation of the object
	 */
	@SuppressWarnings("MethodReturnAlwaysConstant")
	@Override
	public String toString() {
		return "TownReportGenerator";
	}
}
