package model.viewer;

import model.listeners.GraphicalParamsSource;
import model.listeners.SelectionChangeSource;
import model.map.Point;
import model.misc.IDriverModel;

/**
 * An interface for a model behind the map viewer, handling the selected tile and visible
 * dimensions, and allowing the caller to get the tile at a specific point.
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
public interface IViewerModel
		extends IDriverModel, SelectionChangeSource, GraphicalParamsSource {
	/**
	 * @return the location of the currently selected tile.
	 */
	Point getSelectedPoint();

	/**
	 * Set the new selected tiles, given coordinates.
	 *
	 * @param point the location of the new tile.
	 */
	void setSelection(Point point);

	/**
	 * @param dim the new visible dimensions of the map
	 */
	void setDimensions(VisibleDimensions dim);

	/**
	 * @return the visible dimensions of the map
	 */
	VisibleDimensions getDimensions();

	/**
	 * @return the current zoom level
	 */
	int getZoomLevel();

	/**
	 * Zoom in.
	 */
	void zoomIn();

	/**
	 * Zoom out.
	 */
	void zoomOut();

	/**
	 * Reset the zoom level to the default.
	 */
	void resetZoom();
}
