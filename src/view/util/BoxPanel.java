package view.util;

import java.awt.*;
import javax.swing.*;

/**
 * A JPanel laid out by a BoxLayout, with helper methods.
 *
 * This is part of the Strategic Primer assistive programs suite developed by Jonathan
 * Lovelace.
 *
 * Copyright (C) 2013-2014 Jonathan Lovelace
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
public class BoxPanel extends JPanel {
	/**
	 * If true, the panel is laid out on the line axis; if false, on the page axis.
	 */
	private final boolean horizontal;

	/**
	 * Constructor.
	 *
	 * @param horiz If true, the panel is laid out on the line axis; if false, on the
	 *                 page
	 *              axis.
	 */
	@SuppressWarnings("UnnecessarySuperQualifier")
	public BoxPanel(final boolean horiz) {
		horizontal = horiz;
		if (horizontal) {
			super.setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		} else {
			super.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		}
	}

	/**
	 * Add "glue" between components.
	 */
	public final void addGlue() {
		if (horizontal) {
			add(Box.createHorizontalGlue());
		} else {
			add(Box.createVerticalGlue());
		}
	}

	/**
	 * Add a rigid area between components.
	 *
	 * @param dim how big to make it in the dimension that counts.
	 */
	public final void addRigidArea(final int dim) {
		if (horizontal) {
			add(Box.createRigidArea(new Dimension(dim, 0)));
		} else {
			add(Box.createRigidArea(new Dimension(0, dim)));
		}
	}
}
