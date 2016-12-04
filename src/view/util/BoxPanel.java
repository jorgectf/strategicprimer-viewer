package view.util;

import java.awt.*;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import javax.swing.*;

/**
 * A JPanel laid out by a BoxLayout, with helper methods.
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
public class BoxPanel extends JPanel {
	/**
	 * If true, the panel is laid out on the line axis; if false, on the page axis.
	 */
	private final boolean horizontal;

	/**
	 * Constructor.
	 *
	 * @param lineAxis If true, the panel is laid out on the line axis (horizontally); if
	 *                 false, on the page axis.
	 */
	@SuppressWarnings("UnnecessarySuperQualifier")
	public BoxPanel(final boolean lineAxis) {
		horizontal = lineAxis;
		if (horizontal) {
			super.setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		} else {
			super.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		}
	}

	/**
	 * Add "glue" between components.
	 */
	@SuppressWarnings("NonBooleanMethodNameMayNotStartWithQuestion")
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
	@SuppressWarnings("NonBooleanMethodNameMayNotStartWithQuestion")
	public final void addRigidArea(final int dim) {
		if (horizontal) {
			add(Box.createRigidArea(new Dimension(dim, 0)));
		} else {
			add(Box.createRigidArea(new Dimension(0, dim)));
		}
	}

	/**
	 * Prevent serialization.
	 *
	 * @param out ignored
	 * @throws IOException always
	 */
	@SuppressWarnings({"unused", "static-method"})
	private void writeObject(final ObjectOutputStream out) throws IOException {
		throw new NotSerializableException("Serialization is not allowed");
	}

	/**
	 * Prevent serialization
	 *
	 * @param in ignored
	 * @throws IOException            always
	 * @throws ClassNotFoundException never
	 */
	@SuppressWarnings({"unused", "static-method"})
	private void readObject(final ObjectInputStream in)
			throws IOException, ClassNotFoundException {
		throw new NotSerializableException("Serialization is not allowed");
	}

	/**
	 * @return a String representation of the object
	 */
	@Override
	public String toString() {
		if (horizontal) {
			return "Horizontal BoxPanel";
		} else {
			return "Vertical BoxPanel";
		}
	}
}
