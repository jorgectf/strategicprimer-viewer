package view.exploration;

import java.awt.Graphics;
import java.awt.Polygon;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import javax.swing.JButton;
import model.map.IMapNG;
import model.map.Point;
import model.map.PointFactory;
import model.viewer.FixtureMatcher;
import model.viewer.ZOrderFilter;
import org.eclipse.jdt.annotation.Nullable;
import view.map.main.TileDrawHelper;
import view.map.main.TileDrawHelperFactory;

/**
 * A button that represents a tile in two maps.
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
public final class DualTileButton extends JButton {
	/**
	 * How much margin to give.
	 */
	private static final int MARGIN = 2;
	/**
	 * The main map map.
	 */
	private final IMapNG mapOne;
	/**
	 * The subordinate map.
	 */
	private final IMapNG mapTwo;

	/**
	 * @param master      the first map
	 * @param subordinate the second map
	 */
	public DualTileButton(final IMapNG master, final IMapNG subordinate) {
		mapOne = master;
		mapTwo = subordinate;
	}

	/**
	 * The currently selected point.
	 */
	@SuppressWarnings("FieldHasSetterButNoGetter")
	private Point point = PointFactory.point(-1, -1);
	/**
	 * The ZOrderFilter instance to pass to the factory rather than null.
	 */
	private static final ZOrderFilter NULL_ZOF = fix -> true;

	/**
	 * Paint the component.
	 *
	 * @param pen the Graphics object to draw with.
	 */
	@SuppressWarnings("ParameterNameDiffersFromOverriddenParameter")
	@Override
	protected void paintComponent(@Nullable final Graphics pen) {
		if (pen == null) {
			throw new IllegalArgumentException("Graphics cannot be null");
		}
		super.paintComponent(pen);
		// TODO: When the model has a suitable list of matchers, use it instead of this stub
		final TileDrawHelper helper = TileDrawHelperFactory.INSTANCE.factory(2,
				this, NULL_ZOF,
				Collections.singleton(new FixtureMatcher(fix -> true, "stub")));
		pen.setClip(new Polygon(
									new int[]{getWidth() - MARGIN, MARGIN, MARGIN},
									new int[]{
											MARGIN, getHeight() - MARGIN, MARGIN},
									3));
		helper.drawTileTranslated(pen, mapOne, point, getWidth(), getHeight());
		pen.setClip(new Polygon(new int[]{getWidth() - MARGIN,
				getWidth() - MARGIN, MARGIN}, new int[]{MARGIN,
				getHeight() - MARGIN, getHeight() - MARGIN}, 3));
		helper.drawTileTranslated(pen, mapTwo, point, getWidth(), getHeight());
	}

	/**
	 * Set the currently selected point.
	 *
	 * @param newPoint the newly selected point
	 */
	public void setPoint(final Point newPoint) {
		point = newPoint;
		repaint();
	}
	/**
	 * Prevent serialization.
	 * @param out ignored
	 * @throws IOException always
	 */
	@SuppressWarnings({ "unused", "static-method" })
	private void writeObject(final ObjectOutputStream out) throws IOException {
		throw new NotSerializableException("Serialization is not allowed");
	}
	/**
	 * Prevent serialization
	 * @param in ignored
	 * @throws IOException always
	 * @throws ClassNotFoundException never
	 */
	@SuppressWarnings({ "unused", "static-method" })
	private void readObject(final ObjectInputStream in)
			throws IOException, ClassNotFoundException {
		throw new NotSerializableException("Serialization is not allowed");
	}
	/**
	 * @return a diagnostic String
	 */
	@Override
	public String toString() {
		return "DualTileButton for " + point;
	}
}
