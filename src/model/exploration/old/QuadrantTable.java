package model.exploration.old;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import model.map.Point;
import model.map.PointFactory;
import model.map.TileFixture;
import model.map.TileType;
import util.NullCleaner;

/**
 * A class for things where results are by quadrant rather than randomly.
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
public final class QuadrantTable implements EncounterTable {
	/**
	 * The default size of the map in rows.
	 */
	private static final int MAP_SIZE_ROWS = 69;
	/**
	 * The default size of the map in columns.
	 */
	private static final int MAP_SIZE_COLS = 88;
	/**
	 * The collection of results.
	 */
	private final Map<Point, String> quadrants;
	/**
	 * Constructor.
	 * @param mapRows the size of the map in rows
	 * @param mapCols the size of the map in columns
	 * @param rows the number of rows of quadrants
	 * @param items the items to allocate by quadrant
	 */
	public QuadrantTable(final int mapRows, final int mapCols, final int rows,
						final List<String> items) {
		if ((items.size() % rows) != 0) {
			throw new IllegalArgumentException(Integer.toString(items.size()) +
													" items won't divide evenly into " +
													Integer.toString(rows));
		}
		final int cols = items.size() / rows;
		final int rowStep = mapRows / rows;
		final int colStep = mapCols / cols;
		final int rowRemain = mapRows % rows;
		final int colRemain = mapCols % cols;
		quadrants = new HashMap<>();
		int i = 0;
		for (int row = 0; row < (mapRows - rowRemain); row += rowStep) {
			for (int col = 0; col < (mapCols - colRemain); col += colStep) {
				quadrants.put(PointFactory.point(row, col), items.get(i));
				i++;
			}
		}
	}
	/**
	 * Constructor.
	 *
	 * @param rows  the number of rows of quadrants
	 * @param items the items to allocate by quadrant
	 */
	public QuadrantTable(final int rows, final List<String> items) {
		this(MAP_SIZE_ROWS, MAP_SIZE_COLS, rows, items);
	}

	/**
	 * @param row the row of a tile
	 * @param col the column of a tile
	 * @return the result from the quadrant containing that tile.
	 */
	public String getQuadrantValue(final int row, final int col) {
		Point bestKey = PointFactory.point(-1, -1);
		for (final Point iter : quadrants.keySet()) {
			if ((iter.getRow() <= row) && (iter.getRow() > bestKey.getRow()) && (iter.getCol() <= col) &&
						(iter.getCol() > bestKey.getCol())) {
				bestKey = iter;
			}
		}
		return NullCleaner.valueOrDefault(quadrants.get(bestKey), "");
	}

	/**
	 * @param terrain  ignored
	 * @param fixtures ignored
	 * @param point    the location of the tile
	 * @return what the table has for that tile
	 */
	@Override
	public String generateEvent(final Point point, final TileType terrain,
								final Iterable<TileFixture> fixtures) {
		return getQuadrantValue(point.getRow(), point.getCol());
	}
	/**
	 * @param terrain  ignored
	 * @param point    ignored
	 * @param fixtures any fixtures on the tile
	 * @return the event on that tile
	 */
	@Override
	public String generateEvent(final Point point, final TileType terrain,
								final Stream<TileFixture> fixtures) {
		return getQuadrantValue(point.getRow(), point.getCol());
	}

	/**
	 * @return all events that this table can produce.
	 */
	@Override
	public Set<String> allEvents() {
		return new HashSet<>(quadrants.values());
	}

	/**
	 * @return a String representation of the object
	 */
	@Override
	public String toString() {
		return "QuadrantTable in " + quadrants.size() + " quadrants";
	}
}
