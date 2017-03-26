import java.awt.event {
    ActionEvent
}
import java.awt {
    Component,
    Frame
}
import model.map.fixtures {
    RiverFixture
}
import strategicprimer.viewer.drivers {
    SPDialog
}
import javax.swing {
    JTextField,
    JPanel,
    JScrollPane,
    JCheckBox,
    JButton,
    JLabel,
    ScrollPaneConstants,
    SwingUtilities
}
import model.map {
    HasOwner,
    Player,
    TileFixture,
    IFixture,
    HasName,
    HasKind,
    Point
}
import strategicprimer.viewer.model.map {
    PointIterator,
    FixtureIterable
}
import lovelace.util.jvm {
    platform,
    listenedButton,
    BoxPanel,
    horizontalSplit,
    BoxAxis,
    BorderedPanel,
    boxPanel
}
"""A dialog to let the user find fixtures by ID, name, or "kind"."""
class FindDialog(Frame parent, IViewerModel model) extends SPDialog(parent, "Find") {
    JTextField searchField = JTextField("", 20);
    JCheckBox backwards = JCheckBox("Search backwards");
    JCheckBox vertically = JCheckBox("Search vertically then horizontally");
    JCheckBox caseSensitive = JCheckBox("Case-sensitive search");
    Component&ZOrderFilter filterList = fixtureFilterList();
    "Whether the fixture has a name matching the given pattern."
    Boolean matchesName(String pattern, IFixture fixture, Boolean caseSensitivity) {
        if (is HasName fixture) {
            String name = (caseSensitivity) then fixture.name else
            fixture.name.lowercased;
            return name.contains(pattern);
        } else {
            return false;
        }
    }
    "Whether the fixture has a kind matching the given pattern."
    Boolean matchesKind(String pattern, IFixture fixture, Boolean caseSensitivity) {
        if (is HasKind fixture) {
            String kind = (caseSensitivity) then fixture.kind else
            fixture.kind.lowercased;
            return kind.contains(pattern);
        } else {
            return false;
        }
    }
    "Whether the fixture has an owner matching the given pattern."
    Boolean matchesOwner(String pattern, Integer? idNum, IFixture fixture,
            Boolean caseSensitivity) {
        if (is HasOwner fixture) {
            Player owner = fixture.owner;
            String ownerName = (caseSensitivity) then owner.name else
            owner.name.lowercased;
            if (exists idNum, owner.playerId == idNum || ownerName.contains(pattern)) {
                return true;
            } else if ("me".equals(pattern.lowercased), owner.current) {
                return true;
            } else if ({"none", "independent"}.any(pattern.lowercased.equals),
                owner.independent) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
    "Whether the fixture matches the pattern in any of our simple ways."
    Boolean matchesSimple(String pattern, Integer? idNum, IFixture fixture,
            Boolean caseSensitivity) {
        if (pattern.empty) {
            return false;
        } else if (is TileFixture fixture, !filterList.shouldDisplay(fixture)) {
            return false;
        } else if (exists idNum, idNum == fixture.id) {
            return true;
        } else if (matchesName(pattern, fixture, caseSensitivity) ||
        matchesKind(pattern, fixture, caseSensitivity) ||
        matchesOwner(pattern, idNum, fixture, caseSensitivity)) {
            return true;
        } else {
            return false;
        }
    }
    "Whether the given fixture matches the given pattern in any way we recognize."
    Boolean matches(String pattern, Integer? idNum, IFixture fixture,
            Boolean caseSensitivity) {
        if (matchesSimple(pattern, idNum, fixture, caseSensitivity)) {
            return true;
        } else if (is FixtureIterable<out Object> fixture) {
            for (member in fixture) {
                if (is IFixture member, matches(pattern, idNum, member, caseSensitivity)) {
                    return true;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
    "Search for the current pattern. If the pattern is found (as the ID of a fixture,
     or the name of a [[HasName]], or the kind of a [[HasKind]]), select the tile
     containing the thing found. If the pattern is the empty string, don't search."
    shared void search() {
        String pattern;
        Boolean caseSensitivity = caseSensitive.selected;
        if (caseSensitivity) {
            pattern = searchField.text.trimmed;
        } else {
            pattern = searchField.text.trimmed.lowercased;
        }
        if (pattern.empty) {
            return;
        }
        Integer? idNum;
        if (is Integer temp = Integer.parse(pattern)) {
            idNum = temp;
        } else {
            idNum = null;
        }
        // TODO: inline declaration into if
        Point? result = PointIterator(model.mapDimensions, !backwards.selected,
            !vertically.selected, model.selection).find(
                    (point) => model.map.getAllFixtures(point).any(
                        (fixture) => matches(pattern, idNum, fixture, caseSensitivity)));
        if (exists result) {
            log.info("Found in point ``result``");
            model.selection = result;
        }
    }
    Anything(ActionEvent) okListener = (ActionEvent event) {
        search();
        setVisible(false);
        parent.requestFocus();
        dispose();
    };
    searchField.addActionListener(okListener);
    searchField.setActionCommand("OK");
    JPanel searchBoxPane = JPanel();
    searchBoxPane.add(searchField);
    JPanel contentPanel = boxPanel(BoxAxis.pageAxis);
    contentPanel.add(searchBoxPane);
    contentPanel.add(backwards);
    contentPanel.add(vertically);
    contentPanel.add(caseSensitive);
    JPanel&BoxPanel buttonPanel = boxPanel(BoxAxis.lineAxis);
    buttonPanel.addGlue();
    JButton okButton = listenedButton("OK", okListener);
    JButton cancelButton = listenedButton("Cancel", (ActionEvent event) {
        setVisible(false);
        parent.requestFocus();
        dispose();
    });
    platform.makeButtonsSegmented(okButton, cancelButton);
    buttonPanel.add(okButton);
    if (platform.systemIsMac) {
        searchField.putClientProperty("JTextField.variant", "search");
        searchField.putClientProperty("JTextField.Search.FindAction", okListener);
        searchField.putClientProperty("JTextField.Search.CancelAction",
                    (ActionEvent event) => searchField.text = "");
    } else {
        buttonPanel.addGlue();
    }
    buttonPanel.add(cancelButton);
    contentPanel.add(buttonPanel);
    SwingUtilities.invokeLater(() {
        void populate(Anything fixture) {
            if (is TileFixture fixture) {
                filterList.shouldDisplay(fixture);
            } else if (is Iterable<Anything> fixture) {
                for (item in fixture) {
                    populate(item);
                }
            }
        }
        for (point in model.map.locations) {
            if (!model.map.getRivers(point).empty) {
                populate(RiverFixture());
            }
            populate(model.map.getAllFixtures(point));
        }
    });
    contentPane = horizontalSplit(0.6, 0.6, contentPanel,
        BorderedPanel.verticalPanel(JLabel("Find only ..."),
            JScrollPane(filterList, ScrollPaneConstants.verticalScrollbarAsNeeded,
                ScrollPaneConstants.horizontalScrollbarAsNeeded), null));
    pack();
}
