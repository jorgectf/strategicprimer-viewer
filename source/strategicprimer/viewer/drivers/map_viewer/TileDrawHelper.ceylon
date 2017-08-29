import strategicprimer.model.map.fixtures.towns {
    Fortress
}
import strategicprimer.model.map {
    Point,
    IMapNG
}
import strategicprimer.model.map.fixtures {
    IEvent
}
import strategicprimer.model.map.fixtures.mobile {
    IUnit
}
import java.awt {
    Graphics
}
"An interface for helpers that do the drawing of tiles in various components."
shared interface TileDrawHelper {
    "Draw a tile. Assumes that the graphics context has been translated so that its origin
     coincides with the tile's upper-left-hand corner."
    shared formal void drawTileTranslated(
            "The graphics context"
            Graphics pen,
            "The map to draw a tile from"
            IMapNG map,
            "The (location of the) tile to draw"
            Point location,
            "The width of the drawing area (i.e. how wide to draw the tile)"
            Integer width,
            "The height of the drawing area (i.e. how tall to draw the tile)"
            Integer height);
    "Draw a tile, at the given coordinates."
    shared formal void drawTile(
            "The graphics context"
            Graphics pen,
            "The map to draw a tile from"
            IMapNG map,
            "The (location of the) tile to draw"
            Point location,
            "The coordinates of the tile's upper-left corner."
            Coordinate coordinates,
            "The width ('x') and height ('y') to draw the tile within."
            Coordinate dimensions);
}
"Whether the given map has any fortresses at the given location."
Boolean hasAnyForts(IMapNG map, Point location) =>
//        !map.fixtures[location].narrow<Fortress>().empty; // TODO: syntax sugar once compiler bug fixed
        !map.fixtures.get(location).narrow<Fortress>().empty;
"Whether the given map has any units at the given location."
Boolean hasAnyUnits(IMapNG map, Point location) =>
//        !map.fixtures[location].narrow<IUnit>().empty;
        !map.fixtures.get(location).narrow<IUnit>().empty;
"""Whether the given map has any "events" at the given location."""
Boolean hasEvent(IMapNG map, Point location) =>
//        !map.fixtures[location].narrow<IEvent>().empty;
        !map.fixtures.get(location).narrow<IEvent>().empty;
