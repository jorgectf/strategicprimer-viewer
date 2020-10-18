import javax.swing.event {
    ChangeEvent,
    ChangeListener
}
import java.awt {
    BorderLayout,
    Adjustable
}
import strategicprimer.model.common.map {
    MapDimensions,
    Point
}
import lovelace.util.common {
    defer
}
import lovelace.util.jvm {
    BorderedPanel
}
import strategicprimer.drivers.common {
    MapChangeListener,
    SelectionChangeListener
}
import javax.swing {
    JScrollBar,
    JComponent,
    InputVerifier
}
import strategicprimer.model.common.map.fixtures.mobile {
    IUnit
}

"A class, formerly static within [[ScrollListener]], to verify that scroll
 inputs are within the valid range."
class ScrollInputVerifier extends InputVerifier {
    Integer() mapDimension;
    Integer() visibleDimension;
    shared new horizontal(MapDimensions() mapDimsSource,
            VisibleDimensions() visibleDimsSource) extends InputVerifier() {
        mapDimension = compose(MapDimensions.columns, mapDimsSource);
        visibleDimension = compose(VisibleDimensions.height, visibleDimsSource);
    }

    shared new vertical(MapDimensions() mapDimsSource,
            VisibleDimensions() visibleDimsSource) extends InputVerifier() {
        mapDimension = compose(MapDimensions.rows, mapDimsSource);
        visibleDimension = compose(VisibleDimensions.height, visibleDimsSource);
    }

    "A scrollbar is valid if its value is between 0 and the size of the map minus the
     visible size of the map (that subtraction is to prevent scrolling so far that
     empty tiles show to the right of or below the map)."
    shared actual Boolean verify(JComponent input) {
        if (is JScrollBar input) {
            return (0:(mapDimension() - visibleDimension())).contains(input.\ivalue);
        } else {
            return false;
        }
    }
}

class ScrollAdjustmentListener(IViewerModel model, JScrollBar horizontalBar, JScrollBar verticalBar)
        satisfies ChangeListener&GraphicalParamsListener {
    shared variable VisibleDimensions visibleDimensions = model.visibleDimensions;
    shared actual void stateChanged(ChangeEvent event) {
        VisibleDimensions oldDimensions = model.visibleDimensions;
        Integer newColumn = horizontalBar.model.\ivalue;
        Integer newRow = verticalBar.model.\ivalue;
        Integer newMinColumn;
        Integer newMaxColumn;
        if (oldDimensions.minimumColumn > newColumn) {
            newMinColumn = newColumn;
            newMaxColumn = newColumn + visibleDimensions.width - 1;
        } else if (oldDimensions.maximumColumn < newColumn) {
            newMaxColumn = newColumn;
            newMinColumn = newColumn - visibleDimensions.width + 1;
        } else {
            newMaxColumn = oldDimensions.maximumColumn;
            newMinColumn = oldDimensions.minimumColumn;
        }
        Integer newMinRow;
        Integer newMaxRow;
        if (oldDimensions.minimumRow > newRow) {
            newMinRow = newRow;
            newMaxRow = newRow + visibleDimensions.height - 1;
        } else if (oldDimensions.maximumColumn < newColumn) {
            newMaxRow = newColumn;
            newMinRow = newRow - visibleDimensions.height + 1;
        } else {
            newMaxRow = oldDimensions.maximumRow;
            newMinRow = oldDimensions.minimumRow;
        }
        VisibleDimensions newDimensions = VisibleDimensions(newMinRow,
            newMaxRow, newMinColumn, newMaxColumn);
        if (oldDimensions != newDimensions) {
            model.visibleDimensions = newDimensions;
        }
    }
    "Handle a change in visible dimensions."
    shared actual void dimensionsChanged(VisibleDimensions oldDimensions,
            VisibleDimensions newDimensions) => visibleDimensions = newDimensions;

    "Ignored; other listeners will adjust the dimensions, causing [[dimensionsChanged]] to
     be called."
    shared actual void tileSizeChanged(Integer oldSize, Integer newSize) { }
}

"A class to change the visible area of the map based on the user's use of the scrollbars."
class ScrollListener satisfies MapChangeListener&SelectionChangeListener&
        GraphicalParamsListener {
    static Integer constrainToRange(Integer val, Integer min, Integer max) {
        if (val < min) {
            return min;
        } else if (val > max) {
            return max;
        } else {
            return val;
        }
    }

    IViewerModel model;
    JScrollBar horizontalBar;
    JScrollBar verticalBar;
    variable MapDimensions mapDimensions;
    variable VisibleDimensions visibleDimensions;
    shared new (IViewerModel mapModel, JScrollBar horizontal, JScrollBar vertical) {
        model = mapModel;
        visibleDimensions = mapModel.visibleDimensions;
        mapDimensions = mapModel.mapDimensions;
        Point selectedPoint = mapModel.selection;
        horizontalBar = horizontal;
        horizontal.model.setRangeProperties(constrainToRange(selectedPoint.column,
                0, mapDimensions.columns - 1),
            smallest(mapDimensions.columns, visibleDimensions.width), 0,
            mapDimensions.columns, false);
        horizontal.inputVerifier = ScrollInputVerifier.horizontal(
            defer(IViewerModel.mapDimensions, [mapModel]),
                    defer(IViewerModel.visibleDimensions, [mapModel]));
        verticalBar = vertical;
        vertical.model.setRangeProperties(constrainToRange(selectedPoint.row, 0,
                mapDimensions.rows - 1),
            smallest(mapDimensions.rows, visibleDimensions.height), 0,
            mapDimensions.rows, false);
        vertical.inputVerifier = ScrollInputVerifier.vertical(
            defer(IViewerModel.mapDimensions, [mapModel]),
                    defer(IViewerModel.visibleDimensions, [mapModel]));

        value adjustmentListener = ScrollAdjustmentListener(model, horizontalBar, verticalBar);
        mapModel.addGraphicalParamsListener(adjustmentListener);

        horizontalBar.model.addChangeListener(adjustmentListener);
        verticalBar.model.addChangeListener(adjustmentListener);
    }

    "Alternate constructor that adds new scroll-bars to an existing component. This only
     works if that component is laid out using a [[BorderLayout]] and doesn't already have
      members at page-end and line-end."
    shared new createScrollBars(IViewerModel mapModel, BorderedPanel component)
            extends ScrollListener(mapModel, JScrollBar(Adjustable.horizontal),
                JScrollBar(Adjustable.vertical)) {
        "We don't want to replace existing components with scrollbars"
        assert (!component.pageEnd exists, !component.lineEnd exists);
        component.pageEnd = horizontalBar;
        component.lineEnd = verticalBar;
    }

    variable Boolean mutex = true;
    "Handle a change in visible dimensions."
    shared actual void dimensionsChanged(VisibleDimensions oldDimensions,
            VisibleDimensions newDimensions) {
        if (mutex) {
            mutex = false;
            visibleDimensions = newDimensions;
            horizontalBar.model.setRangeProperties(largest(model.selection.column, 0),
                smallest(newDimensions.width, mapDimensions.columns),
                0, mapDimensions.columns, false);
            verticalBar.model.setRangeProperties(largest(model.selection.row, 0),
                smallest(newDimensions.height, mapDimensions.rows), 0,
                mapDimensions.rows, false);
            mutex = true;
        }
    }

    "Ignored; other listeners will adjust the dimensions, causing [[dimensionsChanged]] to
     be called."
    shared actual void tileSizeChanged(Integer oldSize, Integer newSize) { }

    "Handle a change to the selected location in the map. The property-change based
     version this replaced went to the model for the selected point rather than looking
     at the reported new value; since it's typesafe here, and probably faster, this
     switched to using the new value it was passed."
    shared actual void selectedPointChanged(Point? old, Point newPoint) {
        VisibleDimensions temp = model.visibleDimensions;
        if (!temp.columns.contains(newPoint.column)) {
            horizontalBar.model.\ivalue = largest(newPoint.column, 0);
        }
        if (!temp.rows.contains(newPoint.row)) {
            verticalBar.model.\ivalue = largest(newPoint.row, 0);
        }
    }

    "Handle notification that a new map was loaded."
    shared actual void mapChanged() {
        mapDimensions = model.mapDimensions;
        visibleDimensions = model.visibleDimensions;
        horizontalBar.model.setRangeProperties(0,
            smallest(visibleDimensions.width, mapDimensions.columns), 0,
            mapDimensions.columns, false);
        verticalBar.model.setRangeProperties(0,
            smallest(visibleDimensions.height, mapDimensions.rows), 0,
            mapDimensions.rows, false);
    }

    shared actual void mapMetadataChanged() {}
    shared actual void selectedUnitChanged(IUnit? oldSelection, IUnit? newSelection) {}
    shared actual void interactionPointChanged() {}
}
