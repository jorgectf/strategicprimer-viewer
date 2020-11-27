import javax.swing {
    JPanel,
    JLabel,
    JSplitPane,
    SwingList=JList,
    JScrollPane,
    JComponent
}
import ceylon.interop.java {
    CeylonList
}
import java.io {
    IOException
}
import lovelace.util.jvm {
    BoxAxis,
    boxPanel,
    BorderedPanel,
    BoxPanel,
    horizontalSplit,
    InterpolatedLabel
}
import strategicprimer.model.common.map {
    HasPortrait,
    HasOwner,
    TileFixture,
    TileType,
    Point
}
import javax.swing.event {
    ListSelectionListener,
    ListSelectionEvent
}
import java.awt {
    Image,
    Graphics,
    GridLayout,
    Dimension,
    Color
}
import strategicprimer.drivers.common {
    VersionChangeListener,
    IDriverModel,
    SelectionChangeListener
}
import strategicprimer.model.common.idreg {
    createIDFactory
}
import lovelace.util.common {
    todo
}
import strategicprimer.model.common.map.fixtures.mobile {
    IUnit
}

"A panel to show the details of a tile, using a list rather than sub-panels with chits
 for its fixtures."
todo("Separate controller functionality from presentation",
     "Try to convert back to a class")
JComponent&VersionChangeListener&SelectionChangeListener detailPanel(
        variable Integer version, IDriverModel model,
        Comparison(TileFixture, TileFixture) sortOrder) {
    JComponent keyElement(Integer version, TileType? type) {
        JPanel&BoxPanel retval = boxPanel(BoxAxis.lineAxis);
        retval.addGlue();
        retval.addRigidArea(7);
        JPanel&BoxPanel panel = boxPanel(BoxAxis.pageAxis);
        panel.addRigidArea(4);
        Integer tileSize = scaleZoom(ViewerModel.defaultZoomLevel, version);
        Color color = colorHelper.get(version, type) else Color.white;
        panel.add(KeyElementComponent(color, Dimension(4, 4),
            Dimension(8, 8), Dimension(tileSize, tileSize)));
        panel.addRigidArea(4);
        JLabel label = JLabel(colorHelper.getDescription(type));
        panel.add(label);
        panel.addRigidArea(4);
        retval.add(panel);
        retval.addRigidArea(7);
        retval.addGlue();
        retval.minimumSize = Dimension(largest(4, label.minimumSize.width.integer) + 14,
            16 + label.minimumSize.height.integer);
        return retval;
    }

    object keyPanel extends JPanel(GridLayout(0, 4)) satisfies VersionChangeListener {
        minimumSize = Dimension(
            (keyElement(version, null).minimumSize.width * 4).integer,
            minimumSize.height.integer);
        preferredSize = minimumSize;
        shared actual void changeVersion(Integer old, Integer newVersion) {
            removeAll();
            for (type in TileType.valuesForVersion(newVersion)) {
                add(keyElement(version, type));
            }
        }
    }
    keyPanel.changeVersion(-1, version);
    String headerString(Point point) =>
            "<html><body><p>Contents of the tile at ``point``:</p></body></html>";
    InterpolatedLabel<[Point]> header =
            InterpolatedLabel<[Point]>(headerString, [Point.invalidPoint]);

    object retval extends JSplitPane(JSplitPane.horizontalSplit, true)
            satisfies VersionChangeListener&SelectionChangeListener {
        shared late SelectionChangeListener delegate;
        shared actual void changeVersion(Integer old, Integer newVersion) =>
                keyPanel.changeVersion(old, newVersion);
        shared actual void selectedPointChanged(Point? old, Point newPoint) {
            delegate.selectedPointChanged(old, newPoint);
            header.arguments = [newPoint];
        }
        shared actual void selectedUnitChanged(IUnit? old, IUnit? newUnit) =>
            delegate.selectedUnitChanged(old, newUnit);
        shared actual void interactionPointChanged() {}
        shared actual void cursorPointChanged(Point? old, Point newCursor) {}
    }

    void markModified() => model.mapModified = true;

    SwingList<TileFixture>&SelectionChangeListener fixtureListObject =
            fixtureList(retval,
                FixtureListModel(model.map.fixtures.get, model.map.baseTerrain.get,
                    model.map.rivers.get, model.map.mountainous.get, (point) => null,
                    null, null, null, null, null, null, sortOrder), // TODO: implementations instead of null?
                createIDFactory(model.map), markModified,
                    model.map.players);

    retval.delegate = fixtureListObject;

    object portrait extends JComponent() satisfies ListSelectionListener {
        variable Image? portrait = null;
        shared actual void paintComponent(Graphics pen) {
            super.paintComponent(pen);
            if (exists local = portrait) {
                pen.drawImage(local, 0, 0, width, height, this);
            }
        }

        shared actual void valueChanged(ListSelectionEvent event) {
            List<TileFixture> selections =
                    CeylonList(fixtureListObject.selectedValuesList);
            portrait = null;
            if (!selections.empty, selections.size == 1) {
                if (is HasPortrait selectedValue = selections.first) {
                    String portraitName = selectedValue.portrait;
                    if (!portraitName.empty) {
                        try {
                            portrait = imageLoader.loadImage(portraitName);
                            repaint();
                            return;
                        } catch (IOException except) {
                            log.warn("I/O error loading portrait", except);
                        }
                    }
                    if (is HasOwner selectedValue) {
                        String playerPortraitName = selectedValue.owner.portrait;
                        if (!playerPortraitName.empty) {
                            try {
                                portrait = imageLoader.loadImage(playerPortraitName);
                            } catch (IOException except) {
                                log.warn("I/O error loading player portrait", except);
                            }
                        }
                    }
                }
                repaint();
            }
        }
    }
    fixtureListObject.addListSelectionListener(portrait);

    JPanel listPanel = BorderedPanel.verticalPanel(header, JScrollPane(fixtureListObject),
        null);
    retval.leftComponent = horizontalSplit(listPanel, portrait);
    retval.rightComponent = keyPanel;
    retval.resizeWeight = 0.9;
    retval.setDividerLocation(0.9);
    return retval;
}
