package model.exploration.old;

import java.util.Set;
import java.util.stream.Stream;
import model.map.MapDimensions;
import model.map.Point;
import model.map.TileFixture;
import model.map.TileType;

/**
 * An interface for encounter tables, both quadrant and random-event tables. At present we
 * assume this is for the Judge's use; to produce output a player can see unmodified we
 * need to be able to know the explorer's Perception modifier and perhaps other data.
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
public interface EncounterTable {
	/**
	 * Generates an "encounter." For QuadrantTables this is always the same for each
	 * tile for random event tables the result will be randomly selected from that table.
	 *
	 * @param point         the location of the tile
	 * @param terrain       the terrain at the location
	 * @param fixtures      the fixtures on the tile, if any
	 * @param mapDimensions the dimensions of the map
	 * @return an appropriate event for that tile
	 */
	String generateEvent(Point point, TileType terrain, Iterable<TileFixture> fixtures,
						 final MapDimensions mapDimensions);

	/**
	 * Generates an "encounter." For QuadrantTables this is always the same for each
	 * tile for random event tables the result will be randomly selected from that table.
	 *
	 * @param point         the location of the tile
	 * @param terrain       the terrain at the location
	 * @param fixtures      the fixtures on the tile, if any
	 * @param mapDimensions the dimensions of the map
	 * @return an appropriate event for that tile
	 */
	String generateEvent(Point point, TileType terrain, Stream<TileFixture> fixtures,
						 final MapDimensions mapDimensions);

	/**
	 * For table-debugging purposes.
	 *
	 * @return all events the table can return.
	 */
	Set<String> allEvents();
}
