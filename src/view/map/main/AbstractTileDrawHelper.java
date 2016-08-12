package view.map.main;

import java.awt.*;
import model.map.IEvent;
import model.map.IMapNG;
import model.map.Point;
import model.map.TileType;
import model.map.fixtures.mobile.IUnit;
import model.map.fixtures.towns.Fortress;
import util.NullCleaner;

/**
 * An abstract superclass containing helper methods for TileDrawHelpers.
 *
 * This is part of the Strategic Primer assistive programs suite developed by Jonathan
 * Lovelace.
 *
 * Copyright (C) 2011-2015 Jonathan Lovelace
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
public abstract class AbstractTileDrawHelper implements TileDrawHelper {
	/**
	 * Brown, the color of a fortress.
	 */
	protected static final Color FORT_COLOR = new Color(160, 82, 45);
	/**
	 * Purple, the color of a unit.
	 */
	protected static final Color UNIT_COLOR = new Color(148, 0, 211);
	/**
	 * Mapping from tile types to colors.
	 */
	private static final TileUIHelper COLORS = new TileUIHelper();
	/**
	 * The number of sides on the symbol for a miscellaneous event.
	 */
	protected static final int MISC_EVENT_SIDES = 3;

	/**
	 * The color of the icon used to show that a tile has an event or associated text.
	 */
	protected static final Color EVENT_COLOR = NullCleaner.assertNotNull(Color.pink);

	/**
	 * @param ver  the map version
	 * @param type a tile type
	 * @return the color associated with that tile-type.
	 */
	protected static Color getTileColor(final int ver, final TileType type) {
		return COLORS.get(ver, type);
	}

	/**
	 * @param map      a map
	 * @param location a location
	 * @return whether there are any fortresses at that location
	 */
	protected static boolean hasAnyForts(final IMapNG map, final Point location) {
		return map.streamOtherFixtures(location).anyMatch(Fortress.class::isInstance);
	}

	/**
	 * @param map      a map
	 * @param location a location
	 * @return whether there are any units at that location
	 */
	protected static boolean hasAnyUnits(final IMapNG map, final Point location) {
		return map.streamOtherFixtures(location).anyMatch(IUnit.class::isInstance);
	}

	/**
	 * @param map      a map
	 * @param location a location
	 * @return whether there are any 'events' at that location
	 */
	protected static boolean hasEvent(final IMapNG map, final Point location) {
		return map.streamOtherFixtures(location).anyMatch(IEvent.class::isInstance);
	}

	/**
	 * @return the UI helper.
	 */
	protected static TileUIHelper getHelper() {
		return COLORS;
	}
}
