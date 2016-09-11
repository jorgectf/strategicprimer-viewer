package model.exploration.old;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import model.map.MapDimensions;
import model.map.Point;
import model.map.TileFixture;
import model.map.TileType;
import util.ComparablePair;
import util.Pair;
import util.SingletonRandom;

/**
 * A table where the event is selected at random.
 *
 * This is part of the Strategic Primer assistive programs suite developed by Jonathan
 * Lovelace.
 *
 * Copyright (C) 2013-2016 Jonathan Lovelace
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of version 3 of the GNU General Public License as published by the Free Software
 * Foundation; see COPYING or
 * <a href="http://www.gnu.org/licenses/">http://www.gnu.org/licenses/</a>.
 *
 * @author Jonathan Lovelace
 */
public final class RandomTable implements EncounterTable {
	/**
	 * A list of items.
	 */
	private final List<ComparablePair<Integer, String>> table;

	/**
	 * @param point    ignored
	 * @param terrain  ignored
	 * @param fixtures ignored
	 * @param mapDimensions ignored
	 * @return a random item from the table, or the last item in the table if the normal
	 * procedure fails.
	 */
	@Override
	public String generateEvent(final Point point, final TileType terrain,
								final Iterable<TileFixture> fixtures,
								final MapDimensions mapDimensions) {
		final int roll = SingletonRandom.RANDOM.nextInt(100);
		return getLowestMatch(roll);
	}
	/**
	 * @param point    ignored
	 * @param terrain  ignored
	 * @param fixtures any fixtures on the tile
	 * @param mapDimensions ignored
	 * @return the event on that tile
	 */
	@Override
	public String generateEvent(final Point point, final TileType terrain,
								final Stream<TileFixture> fixtures,
								final MapDimensions mapDimensions) {
		return getLowestMatch(SingletonRandom.RANDOM.nextInt(100));
	}

	/**
	 * @param value a number to check the table against
	 * @return the result of the check
	 */
	private String getLowestMatch(final int value) {
		for (final Pair<Integer, String> item : table) {
			if (value >= item.first().intValue()) {
				return item.second();
			}
		}
		return table.get(table.size() - 1).second();
	}

	/**
	 * Constructor.
	 *
	 * @param items the items in the table.
	 */
	public RandomTable(final List<ComparablePair<Integer, String>> items) {
		table = new ArrayList<>(items);
		Collections.sort(table, Collections.reverseOrder());
	}

	/**
	 * @return all events that this table can produce.
	 */
	@Override
	public Set<String> allEvents() {
		return table.stream().map(Pair::second).collect(Collectors.toSet());
	}

	/**
	 * @return a String representation of the class
	 */
	@Override
	public String toString() {
		return "RandomTable of " + table.size() + " items";
	}
}
