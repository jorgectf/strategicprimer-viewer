package model.viewer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import model.listeners.GraphicalParamsListener;
import model.listeners.SelectionChangeListener;
import model.listeners.SelectionChangeSupport;
import model.map.IMutableMapNG;
import model.map.Point;
import model.map.PointFactory;
import model.misc.IDriverModel;
import model.misc.SimpleDriverModel;
import util.Pair;

/**
 * A class to encapsulate the various model-type things views need to do with maps.
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
 *
 *         TODO: tests
 */
public final class ViewerModel extends SimpleDriverModel implements
		IViewerModel {
	/**
	 * The starting zoom level.
	 */
	public static final int DEF_ZOOM_LEVEL = 8;
	/**
	 * The maximum zoom level, to make sure that the tile size never overflows.
	 */
	private static final int MAX_ZOOM_LEVEL = Integer.MAX_VALUE / 4;
	/**
	 * The list of graphical-parameter listeners.
	 */
	private final Collection<GraphicalParamsListener> gpListeners = new ArrayList<>();
	/**
	 * The object to handle notifying selection-change listeners.
	 */
	private final SelectionChangeSupport scs = new SelectionChangeSupport();
	/**
	 * The current zoom level.
	 */
	private int zoomLevel = DEF_ZOOM_LEVEL;
	/**
	 * The currently selected point in the main map.
	 */
	private Point selPoint;
	/**
	 * The visible dimensions of the map.
	 */
	private VisibleDimensions dimensions;

	/**
	 * Constructor.
	 *
	 * @param firstMap the initial map
	 * @param file     the name the map was loaded from or should be saved to
	 */
	public ViewerModel(final IMutableMapNG firstMap, final Optional<Path> file) {
		dimensions = new VisibleDimensions(0, firstMap.dimensions().rows - 1, 0,
												  firstMap.dimensions().cols - 1);
		selPoint = PointFactory.INVALID_POINT;
		setMap(firstMap, file);
	}

	/**
	 * Constructor.
	 *
	 * @param pair a Pair of the initial map and the name it was loaded from or should be
	 *             saved to
	 */
	public ViewerModel(final Pair<IMutableMapNG, Optional<Path>> pair) {
		this(pair.first(), pair.second());
	}

	/**
	 * Copy constructor.
	 *
	 * @param model a driver model
	 */
	public ViewerModel(final IDriverModel model) {
		if (model instanceof IViewerModel) {
			dimensions = ((IViewerModel) model).getDimensions();
			selPoint = ((IViewerModel) model).getSelectedPoint();
		} else {
			dimensions = new VisibleDimensions(0, model.getMapDimensions().rows - 1, 0,
													  model.getMapDimensions().cols - 1);
			selPoint = PointFactory.INVALID_POINT;
		}
		setMap(model.getMap(), model.getMapFile());
	}

	/**
	 * Set the (main) map and its filename, and also clear the selection and  reset the
	 * visible dimensions and the zoom level.
	 * @param newMap the new map
	 * @param origin the file the map was loaded from or should be saved to
	 */
	@Override
	public void setMap(final IMutableMapNG newMap, final Optional<Path> origin) {
		super.setMap(newMap, origin);
		clearSelection();
		setDimensions(new VisibleDimensions(0, newMap.dimensions().rows - 1, 0,
												   newMap.dimensions().cols - 1));
		resetZoom();
	}

	/**
	 * Set the new selected tiles, given coordinates.
	 *
	 * @param point the location of the new tile.
	 */
	@Override
	public void setSelection(final Point point) {
		final Point oldSel = selPoint;
		selPoint = point;
		scs.fireChanges(oldSel, selPoint);
	}

	/**
	 * Clear the selection.
	 */
	public void clearSelection() {
		final Point oldSel = selPoint;
		selPoint = PointFactory.INVALID_POINT;
		scs.fireChanges(oldSel, selPoint);
	}

	/**
	 * @return the visible dimensions of the map
	 */
	@Override
	public VisibleDimensions getDimensions() {
		return dimensions;
	}

	/**
	 * Set the visible dimensions.
	 * @param dim the new visible dimensions of the map
	 */
	@Override
	public void setDimensions(final VisibleDimensions dim) {
		for (final GraphicalParamsListener list : gpListeners) {
			list.dimensionsChanged(dimensions, dim);
		}
		dimensions = dim;
	}

	/**
	 * A simple toString().
	 * @return a String representation of the class
	 */
	@Override
	public String toString() {
		final Optional<Path> mapFile = getMapFile();
		return mapFile.map(path -> "ViewerModel for " + path)
					   .orElse("ViewerModel for an unsaved map");
	}

	/**
	 * The current zoom level.
	 * @return the current zoom level.
	 */
	@Override
	public int getZoomLevel() {
		return zoomLevel;
	}

	/**
	 * Zoom in, increasing the zoom level.
	 */
	@Override
	public void zoomIn() {
		if (zoomLevel < MAX_ZOOM_LEVEL) {
			zoomLevel++;
			for (final GraphicalParamsListener list : gpListeners) {
				list.tileSizeChanged(zoomLevel - 1, zoomLevel);
			}
		}
	}

	/**
	 * Zoom out, decreasing the zoom level.
	 */
	@Override
	public void zoomOut() {
		if (zoomLevel > 1) {
			zoomLevel--;
			for (final GraphicalParamsListener list : gpListeners) {
				list.tileSizeChanged(zoomLevel + 1, zoomLevel);
			}
		}
	}

	/**
	 * Reset the zoom level to the default.
	 */
	@Override
	public void resetZoom() {
		final int old = zoomLevel;
		zoomLevel = DEF_ZOOM_LEVEL;
		for (final GraphicalParamsListener list : gpListeners) {
			list.tileSizeChanged(old, zoomLevel);
		}
	}

	/**
	 * The currently selected point.
	 * @return the location of the currently selected tile
	 */
	@Override
	public Point getSelectedPoint() {
		return selPoint;
	}

	/**
	 * Add a selection-change listener.
	 * @param list a selection-change listener to add
	 */
	@Override
	public void addSelectionChangeListener(final SelectionChangeListener list) {
		scs.addSelectionChangeListener(list);
	}

	/**
	 * Remove a selection-change listener.
	 * @param list a selection-change listener to remove
	 */
	@Override
	public void removeSelectionChangeListener(final SelectionChangeListener list) {
		scs.removeSelectionChangeListener(list);
	}

	/**
	 * Add a graphical-parameters listener.
	 * @param list a listener to add
	 */
	@Override
	public void addGraphicalParamsListener(final GraphicalParamsListener list) {
		gpListeners.add(list);
	}

	/**
	 * Remove a graphical-parameters listener.
	 * @param list a listener to remove
	 */
	@Override
	public void removeGraphicalParamsListener(final GraphicalParamsListener list) {
		gpListeners.remove(list);
	}
}
