package model.viewer;

import java.util.Comparator;
import model.map.TileFixture;
import org.eclipse.jdt.annotation.NonNull;

/**
 * A Comparator for TileFixtures. In the new map version, only the upper-most of a tile's
 * fixtures is visible.
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
 *
 * TODO: tests
 */
public final class FixtureComparator implements Comparator<@NonNull TileFixture> {
	/**
	 * Compare two fixtures.
	 *
	 * @param firstFixture  The first fixture
	 * @param secondFixture The second fixture
	 * @return the result of the comparison.
	 */
	@SuppressWarnings("ParameterNameDiffersFromOverriddenParameter")
	@Override
	public int compare(final TileFixture firstFixture, final TileFixture secondFixture) {
		final int oneValue = firstFixture.getZValue();
		final int twoValue = secondFixture.getZValue();
		if (twoValue > oneValue) {
			return 1;
		} else if (twoValue == oneValue) {
			return 0;
		} else {
			return -1;
		}
	}

	/**
	 * @return a String representation of the object
	 */
	@SuppressWarnings("MethodReturnAlwaysConstant")
	@Override
	public String toString() {
		return "FixtureComparator";
	}
}
