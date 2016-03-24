package view.map.main;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import model.map.MapDimensions;
import model.map.Point;
import model.viewer.IViewerModel;
import model.viewer.VisibleDimensions;
import org.eclipse.jdt.annotation.Nullable;
import util.TypesafeLogger;

/**
 * A class to listen for zoom menu events.
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
public final class ZoomListener implements ActionListener {
	/**
	 * The map model.
	 */
	private final IViewerModel model;

	/**
	 * Constructor.
	 *
	 * @param vmodel the viewer model, which now handles the zoom level
	 */
	public ZoomListener(final IViewerModel vmodel) {
		model = vmodel;
	}

	/**
	 * @param evt the event to handle
	 */
	@SuppressWarnings("ParameterNameDiffersFromOverriddenParameter")
	@Override
	public void actionPerformed(@Nullable final ActionEvent evt) {
		if (evt == null) {
			return;
		} else {
			switch (evt.getActionCommand().toLowerCase()) {
			case "zoom in":
				model.zoomIn();
				break;
			case "zoom out":
				model.zoomOut();
				break;
			case "center":
				final Point selection = model.getSelectedPoint();
				final MapDimensions dims = model.getMapDimensions();
				final VisibleDimensions vDims = model.getDimensions();
				final int topRow;
				if ((selection.row - (vDims.getHeight() / 2)) <= 0) {
					topRow = 0;
				} else if ((selection.row + (vDims.getHeight() / 2)) >= dims.getRows()) {
					topRow = dims.getRows() - vDims.getHeight();
				} else {
					topRow = selection.row - (vDims.getHeight() / 2);
				}
				final int leftColumn;
				if ((selection.col - (vDims.getWidth() / 2)) <= 0) {
					leftColumn = 0;
				} else if ((selection.col + (vDims.getWidth() / 2)) >=
						           dims.getColumns()) {
					leftColumn = dims.getColumns() - vDims.getWidth();
				} else {
					leftColumn = selection.col - (vDims.getWidth() / 2);
				}
				model.setDimensions(
						new VisibleDimensions(topRow, topRow + dims.getRows(), leftColumn,

								                     leftColumn + dims.getColumns()));
				break;
			default:
				TypesafeLogger.getLogger(ZoomListener.class)
						.info("Unhandled action in ZoomListener");
			}
		}
	}

	/**
	 * @return a String representation of the object
	 */
	@SuppressWarnings("MethodReturnAlwaysConstant")
	@Override
	public String toString() {
		return "ZoomListener";
	}
}
