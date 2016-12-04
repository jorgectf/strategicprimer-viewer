package view.util;

/**
 * A class to hold numeric constants useful for drawing.
 *
 * This is part of the Strategic Primer assistive programs suite developed by Jonathan
 * Lovelace.
 *
 * Copyright (C) 2011-2016 Jonathan Lovelace
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of version 3 of the GNU General Public License as published by the Free Software
 * Foundation; see COPYING or
 * <a href="http://www.gnu.org/licenses/">http://www.gnu.org/licenses/</a>.
 *
 * @author Jonathan Lovelace
 */
public enum DrawingNumericConstants {
	/**
	 * The part of a tile's width or height a river's short dimension should occupy.
	 */
	RiverShortDimension(1.0 / 8.0),
	/**
	 * Where the short side of a river starts, along the edge of the tile.
	 */
	RiverShortStart(7.0 / 16.0),
	/**
	 * The part of a tile's width or height its long dimension should occupy.
	 */
	RiverLongDimension(1.0 / 2.0),
	/**
	 * The coordinates in an 'event' other than EventStart, 0, and 100%.
	 */
	EventOther(1.0 / 2.0),
	/**
	 * How far along a tile's dimension a lake should start.
	 */
	LakeStart(1.0 / 4.0),
	/**
	 * How big a unit should be. Also its starting position (?).
	 */
	UnitSize(1.0 / 4.0),
	/**
	 * How wide and tall a fort should be.
	 */
	FortSize(1.0 / 3.0),
	/**
	 * Where a fort should start.
	 */
	FortStart(2.0 / 3.0),
	/**
	 * Where an 'event' should start.
	 */
	EventStart(3.0 / 4.0);
	/**
	 * The constant this instance encapsulates.
	 */
	public final double constant;

	/**
	 * @param numConst the constant this instance encapsulates.
	 */
	DrawingNumericConstants(final double numConst) {
		constant = numConst;
	}
}
