package drivers.map_viewer;

import common.map.Point;
import common.map.IMapNG;
import java.awt.Graphics;

/**
 * An interface for helpers that do the drawing of tiles in various components.
 */
@FunctionalInterface
public interface TileDrawHelper {
	/**
	 * Draw a tile, at the given coordinates.
	 * 
	 * @param pen The graphics context
	 * @param map The map to draw a tile from
	 * @param location The (location of the) tile to draw
	 * @param coordinates The coordinates of the tile's upper-left corner.
	 * @param dimensions The width ('x') and height ('y') to draw the tile within.
	 */
	void drawTile(Graphics pen, IMapNG map, Point location, Coordinate coordinates,
		Coordinate dimensions);
}
