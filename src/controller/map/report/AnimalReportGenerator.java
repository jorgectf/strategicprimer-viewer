package controller.map.report;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import model.map.IFixture;
import model.map.IMapNG;
import model.map.Player;
import model.map.Point;
import model.map.fixtures.mobile.Animal;
import model.report.AbstractReportNode;
import model.report.EmptyReportNode;
import model.report.ListReportNode;
import model.report.SectionListReportNode;
import model.report.SimpleReportNode;
import util.DelayedRemovalMap;
import util.NullCleaner;
import util.Pair;

/**
 * A report generator for sightings of animals.
 *
 * This is part of the Strategic Primer assistive programs suite developed by
 * Jonathan Lovelace.
 *
 * Copyright (C) 2013-2015 Jonathan Lovelace
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of version 3 of the GNU General Public License as published by the
 * Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <a href="http://www.gnu.org/licenses/">http://www.gnu.org/licenses/</a>.
 *
 * @author Jonathan Lovelace
 *
 */
public class AnimalReportGenerator extends AbstractReportGenerator<Animal> {
	/**
	 * @param comparator a comparator for pairs of Points and fixtures.
	 */
	public AnimalReportGenerator(final Comparator<Pair<Point, IFixture>> comparator) {
		super(comparator);
	}
	/**
	 * Produce the sub-report on sightings of animals.
	 *
	 * @param fixtures the set of fixtures
	 * @param map ignored
	 * @param currentPlayer the player for whom the report is being produced
	 * @return the report
	 */
	@Override
	public String produce(
			final DelayedRemovalMap<Integer, Pair<Point, IFixture>> fixtures,
			final IMapNG map, final Player currentPlayer) {
		final Map<String, List<Point>> items = new HashMap<>();
		List<Pair<Point, IFixture>> values = new ArrayList<>(fixtures.values());
		Collections.sort(values, pairComparator);
		for (final Pair<Point, IFixture> pair : values) {
			if (pair.second() instanceof Animal) {
				final Animal animal = (Animal) pair.second();
				// ESCA-JAVA0177:
				final String string; // NOPMD
				if (animal.isTraces()) {
					string = "tracks or traces of " + animal.getKind();
				} else if (animal.isTalking()) {
					string = "talking " + animal.getKind();
				} else {
					string = animal.getKind();
				}
				// ESCA-JAVA0177:
				final List<Point> points; // NOPMD
				if (items.containsKey(string)) {
					points = items.get(string);
				} else {
					points = new ArrayList<>(); // NOPMD
					items.put(string, points);
				}
				points.add(pair.first());
				fixtures.remove(Integer.valueOf(animal.getID()));
			}
		}
		if (items.isEmpty()) {
			return ""; // NOPMD
		} else {
			// We doubt this list will ever be over 16K.
			final StringBuilder builder = new StringBuilder(16384).append(
					"<h4>Animal sightings or encounters</h4>\n").append(
							OPEN_LIST);
			for (final Entry<String, List<Point>> entry : items.entrySet()) {
				builder.append(OPEN_LIST_ITEM).append(entry.getKey())
				.append(": at ").append(pointCSL(entry.getValue()))
				.append(CLOSE_LIST_ITEM);
			}
			return NullCleaner.assertNotNull(builder.append(CLOSE_LIST)
					.toString()); // NOPMD
		}
	}

	/**
	 * Produce the sub-report on sightings of animals.
	 *
	 * @param fixtures the set of fixtures
	 * @param map ignored
	 * @param currentPlayer the player for whom the report is being produced
	 * @return the report
	 */
	@Override
	public AbstractReportNode produceRIR(
			final DelayedRemovalMap<Integer, Pair<Point, IFixture>> fixtures,
			final IMapNG map, final Player currentPlayer) {
		final Map<String, AbstractReportNode> items = new HashMap<>();
		List<Pair<Point, IFixture>> values = new ArrayList<>(fixtures.values());
		Collections.sort(values, pairComparator);
		for (final Pair<Point, IFixture> pair : values) {
			if (pair.second() instanceof Animal) {
				final Animal animal = (Animal) pair.second();
				// ESCA-JAVA0177:
				final String string = animal.getKind();
				final AbstractReportNode collection;
				if (items.containsKey(string)) {
					collection = items.get(string);
				} else {
					collection = new ListReportNode(string); // NOPMD
					items.put(string, collection);
				}
				collection.add(produceRIR(fixtures, map, currentPlayer, animal,
						pair.first()));
				fixtures.remove(Integer.valueOf(animal.getID()));
			}
		}
		if (items.isEmpty()) {
			return EmptyReportNode.NULL_NODE; // NOPMD
		} else {
			final AbstractReportNode retval = new SectionListReportNode(4,
					"Animal sightings or encounters");
			for (final Entry<String, AbstractReportNode> entry : items.entrySet()) {
				retval.add(entry.getValue());
			}
			return retval; // NOPMD
		}
	}

	/**
	 * @param fixtures the set of fixtures
	 * @param currentPlayer the player for whom the report is being produced
	 * @param map ignored
	 * @param item an animal
	 * @param loc its location
	 * @return a sub-report on the animal
	 */
	@Override
	public String produce(
			final DelayedRemovalMap<Integer, Pair<Point, IFixture>> fixtures,
			final IMapNG map, final Player currentPlayer,
			final Animal item, final Point loc) {
		final String tracesOrTalking; // NOPMD
		if (item.isTraces()) {
			tracesOrTalking = "tracks or traces of ";
		} else if (item.isTalking()) {
			tracesOrTalking = "talking ";
		} else {
			tracesOrTalking = "";
		}
		return concat(atPoint(loc), tracesOrTalking, item.getKind(), " ",
				distCalculator.distanceString(loc));
	}

	/**
	 * @param fixtures the set of fixtures
	 * @param currentPlayer the player for whom the report is being produced
	 * @param map ignored
	 * @param item an animal
	 * @param loc its location
	 * @return a sub-report on the animal
	 */
	@Override
	public SimpleReportNode produceRIR(
			final DelayedRemovalMap<Integer, Pair<Point, IFixture>> fixtures,
			final IMapNG map, final Player currentPlayer,
			final Animal item, final Point loc) {
		final String tracesOrTalking; // NOPMD
		if (item.isTraces()) {
			tracesOrTalking = "tracks or traces of ";
		} else if (item.isTalking()) {
			tracesOrTalking = "talking ";
		} else {
			tracesOrTalking = "";
		}
		return new SimpleReportNode(loc, atPoint(loc), tracesOrTalking,
				item.getKind(), " ", distCalculator.distanceString(loc));
	}

	/**
	 * @return a String representation of the object
	 */
	@Override
	public String toString() {
		return "AnimalReportGenerator";
	}
}
