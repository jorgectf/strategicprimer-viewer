import java.awt {
    Component,
    Frame
}
import java.awt.event {
    ActionEvent
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

import lovelace.util.jvm {
    platform,
    listenedButton,
    BoxPanel,
    horizontalSplit,
    BoxAxis,
    BorderedPanel,
    boxPanel
}

import strategicprimer.model.common.map {
    IFixture,
    HasName,
    HasKind,
    Player,
    HasOwner,
    TileFixture,
    PointIterator
}
import strategicprimer.drivers.gui.common {
    SPDialog
}
import lovelace.util.common {
    as,
    todo,
    silentListener
}

"""A dialog to let the user find fixtures by ID, name, or "kind"."""
todo("""Add a "nearby" search (using
        [[strategicprimer.drivers.exploration.common::surroundingPointIterable]]?)""")
class FindDialog(Frame parent, IViewerModel model) extends SPDialog(parent, "Find") {
    JTextField searchField = JTextField("", 20);
    JCheckBox backwards = JCheckBox("Search backwards");
    JCheckBox vertically = JCheckBox("Search vertically then horizontally");
    JCheckBox caseSensitive = JCheckBox("Case-sensitive search");
    Component&ZOrderFilter filterList = FixtureFilterList();

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
            } else if (["none", "independent"].contains(pattern.lowercased),
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
            return true;
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
    Boolean matches(String pattern, Integer? idNum,
            Boolean caseSensitivity)(IFixture fixture) {
        if (matchesSimple(pattern, idNum, fixture, caseSensitivity)) {
            return true;
        } else if (is {IFixture*} fixture) {
            return fixture.any(matches(pattern, idNum, caseSensitivity));
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
        Integer? idNum = as<Integer>(Integer.parse(pattern));
        if (exists result = PointIterator(model.mapDimensions, !backwards.selected,
                !vertically.selected, model.selection).find(
//                    (point) => model.map.fixtures[point].any( // TODO: syntax sugar once compiler bug fixed
                    (point) => model.map.fixtures.get(point).any( // TODO: extract named method (which'd have to be curried here, but better that than an explicit lambda)
                        matches(pattern, idNum, caseSensitivity)))) {
            log.debug("Found in point ``result``");
            model.selection = result;
        }
    }

    void okListener(ActionEvent event) { // TODO: Make a void function and use silentListener()?
        search();
        setVisible(false);
        parent.requestFocus();
        dispose();
    }

    searchField.addActionListener(okListener);
    searchField.setActionCommand("OK");

    JPanel searchBoxPane = JPanel(); // TODO: Use a different layout than BoxLayout for contentPanel so we won't have to wrap searchField in a JPanel
    searchBoxPane.add(searchField);

    JPanel contentPanel = boxPanel(BoxAxis.pageAxis);
    contentPanel.add(searchBoxPane);
    contentPanel.add(backwards);
    contentPanel.add(vertically);
    contentPanel.add(caseSensitive);

    JPanel&BoxPanel buttonPanel = boxPanel(BoxAxis.lineAxis); // TODO: Use a better layout
    buttonPanel.addGlue();

    JButton okButton = listenedButton("OK", okListener);

    void cancelListener(ActionEvent event) { // TODO: Make a void function and use silentListener()?
        setVisible(false);
        parent.requestFocus();
        dispose();
    }
    JButton cancelButton = listenedButton("Cancel", cancelListener);

    platform.makeButtonsSegmented(okButton, cancelButton);
    buttonPanel.add(okButton);

    void clearSearchField() => searchField.text = "";

    if (platform.systemIsMac) {
        searchField.putClientProperty("JTextField.variant", "search");
        searchField.putClientProperty("JTextField.Search.FindAction", okListener);
        searchField.putClientProperty("JTextField.Search.CancelAction",
            silentListener(clearSearchField));
    } else {
        buttonPanel.addGlue();
    }

    buttonPanel.add(cancelButton);
    buttonPanel.addGlue();
    contentPanel.add(buttonPanel);

    void populate(Anything fixture) {
        if (is TileFixture fixture) {
            filterList.shouldDisplay(fixture);
        } else if (is {Anything*} fixture) {
            fixture.each(populate);
        }
    }

    void populateAll() => populate(model.map.fixtures.map(Entry.item));
    SwingUtilities.invokeLater(populateAll);

    JScrollPane scrollPane;
    if (platform.systemIsMac) {
        scrollPane = JScrollPane(filterList,
            ScrollPaneConstants.verticalScrollbarAlways,
            ScrollPaneConstants.horizontalScrollbarAlways);
    } else {
        scrollPane = JScrollPane(filterList,
            ScrollPaneConstants.verticalScrollbarAsNeeded,
            ScrollPaneConstants.horizontalScrollbarAsNeeded);
    }

    contentPane = horizontalSplit(contentPanel,
        BorderedPanel.verticalPanel(JLabel("Find only ..."),
            scrollPane, null), 0.6);
    pack();
}
