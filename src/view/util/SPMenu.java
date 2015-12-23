package view.util;

import controller.map.misc.IOHandler;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import javax.swing.InputMap;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import model.map.IMutableMapNG;
import model.map.Player;
import model.misc.IDriverModel;
import model.misc.IMultiMapModel;
import model.viewer.IViewerModel;
import model.viewer.ViewerModel;
import util.Pair;
import view.map.main.FindDialog;
import view.map.main.SelectTileDialog;
import view.map.main.ViewerFrame;
import view.map.main.ZoomListener;
import view.worker.PlayerChooserHandler;

import static javax.swing.SwingUtilities.invokeLater;
import static view.util.MenuItemCreator.createHotkey;
import static view.util.MenuItemCreator.createMenuItem;
import static view.util.MenuItemCreator.createShiftHotkey;

/**
 * A common superclass for application-specific menu bars.
 *
 * This is part of the Strategic Primer assistive programs suite developed by Jonathan
 * Lovelace.
 *
 * Copyright (C) 2015-2015 Jonathan Lovelace
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
public class SPMenu extends JMenuBar {
	/**
	 * Create the file menu.
	 *
	 * FIXME: Any menu items not handled by the IOHandler should be handled there.
	 *
	 * @param handler the class to handle I/O related menu items
	 * @param parent  the menu-bar's parent window, which should be the window closed
	 *                   when
	 *                the user selects "close"
	 * @param model   the current driver model; only its type is used, to determine which
	 *                menu items to disable.
	 * @return the file menu
	 */
	protected static JMenu createFileMenu(final IOHandler handler,
										  final JFrame parent, final IDriverModel
																	   model) {
		final JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic(KeyEvent.VK_F);
		final JMenuItem newItem = createMenuItem("New", KeyEvent.VK_N,
				createHotkey(KeyEvent.VK_N),
				"Create a new, empty map the same size as the current one",
				handler);
		fileMenu.add(newItem);
		if (!(model instanceof IViewerModel)) {
			newItem.setEnabled(false);
		}
		final String loadCaption;
		final String saveCaption;
		final String saveAsCaption;
		if (model instanceof IMultiMapModel) {
			loadCaption = "Load the main map from file";
			saveCaption = "Save the main map to the file it was loaded from";
			saveAsCaption = "Save the main map to file";
		} else {
			loadCaption = "Load a map from file";
			saveCaption = "Save the map to the file it was loaded from";
			saveAsCaption = "Save the map to file";
		}
		fileMenu.add(createMenuItem("Load", KeyEvent.VK_L,
				createHotkey(KeyEvent.VK_O), loadCaption, handler));
		final JMenuItem loadSecondaryItem = createMenuItem("Load secondary",
				KeyEvent.VK_E, createShiftHotkey(KeyEvent.VK_O),
				"Load an additional secondary map from file", handler);
		fileMenu.add(loadSecondaryItem);
		fileMenu.add(createMenuItem("Save", KeyEvent.VK_S,
				createHotkey(KeyEvent.VK_S),
				saveCaption, handler));
		fileMenu.add(createMenuItem("Save As", KeyEvent.VK_A,
				createShiftHotkey(KeyEvent.VK_S), saveAsCaption,
				handler));
		final JMenuItem saveAllItem = createMenuItem("Save All", KeyEvent.VK_V,
				createHotkey(KeyEvent.VK_L), "Save all maps to their files",
				handler);
		fileMenu.add(saveAllItem);
		if (!(model instanceof IMultiMapModel)) {
			loadSecondaryItem.setEnabled(false);
			saveAllItem.setEnabled(false);
		}
		fileMenu.addSeparator();
		final JMenuItem openViewerItem = createMenuItem("Open in map viewer",
				KeyEvent.VK_M, createHotkey(KeyEvent.VK_M),
				"Open the main map in the map viewer for a broader view",
				evt -> invokeLater(() -> new ViewerFrame(
																new ViewerModel(model
																						.getMap(),
																					   model.getMapFile()),

																handler)
												 .setVisible(true)));
		fileMenu.add(openViewerItem);
		if (model instanceof IViewerModel) {
			openViewerItem.setEnabled(false);
		}
		final JMenuItem openSecondaryViewerItem = createMenuItem(
				"Open secondary map in map viewer", KeyEvent.VK_E,
				createHotkey(KeyEvent.VK_E),
				"Open the first secondary map in the map viewer for a broader view",
				evt -> {
					if (model instanceof IMultiMapModel) {
						final Pair<IMutableMapNG, File> mapPair = ((IMultiMapModel)
																		   model)
																		  .getSubordinateMaps()
																		  .iterator()
																		  .next();
						invokeLater(() -> new ViewerFrame(
																 new ViewerModel(mapPair
																						 .first(),
																						mapPair.second()),
																 handler)
												  .setVisible(true));
					}
				});
		fileMenu.add(openSecondaryViewerItem);
		if ((model instanceof IViewerModel) || !(model instanceof IMultiMapModel)) {
			openSecondaryViewerItem.setEnabled(false);
		}
		fileMenu.addSeparator();
		fileMenu.add(createMenuItem("Close", KeyEvent.VK_W,
				createHotkey(KeyEvent.VK_W),
				"Close this window", evt -> {
					if ((evt != null)
								&& "Close".equals(evt.getActionCommand())) {
						parent.setVisible(false);
						parent.dispose();
					}
				}));
		fileMenu.addSeparator();
		fileMenu.add(createMenuItem("About", KeyEvent.VK_B,
				createHotkey(KeyEvent.VK_B), "Show development credits", handler));
		fileMenu.addSeparator();
		if (!System.getProperty("os.name").toLowerCase().startsWith("mac os x")) {
			fileMenu.add(createMenuItem("Quit", KeyEvent.VK_Q,
					createHotkey(KeyEvent.VK_Q), "Quit the application",
					event -> {
						if ((event != null) && "Quit".equals(event.getActionCommand())) {
							DriverQuit.quit(0);
						}
					}));
		}
		return fileMenu;
	}

	/**
	 * Create the "map" menu, including go-to-tile, find, and zooming functions. This now
	 * takes any IDriverModel, because it's expected that apps where none of that makes
	 * sense will show but disable the menu.
	 *
	 * @param parent the menu-bar's parent window
	 * @param model  the driver model
	 * @return the menu created
	 */
	protected static JMenu createMapMenu(final JFrame parent,
										 final IDriverModel model) {
		final JMenu retval = new JMenu("Map");
		retval.setMnemonic(KeyEvent.VK_M);
		final JMenuItem gotoTileItem;
		final int findKey = KeyEvent.VK_F;
		final KeyStroke findStroke = createHotkey(findKey);
		final JMenuItem findItem;
		final JMenuItem nextItem;
		final int nextKey = KeyEvent.VK_N;
		final KeyStroke nextStroke = createHotkey(KeyEvent.VK_G);
		final ActionListener zoomListener;
		if (model instanceof IViewerModel) {
			gotoTileItem = createMenuItem("Go to tile",
					KeyEvent.VK_T, createHotkey(KeyEvent.VK_T),
					"Go to a tile by coordinates",
					evt -> new SelectTileDialog(parent, (IViewerModel) model)
								   .setVisible(true));
			final FindDialog finder = new FindDialog(parent, (IViewerModel) model);
			findItem = createMenuItem("Find a fixture", findKey,
					findStroke, "Find a fixture by name, kind, or ID#",
					evt -> finder.setVisible(true));
			nextItem = createMenuItem("Find next", nextKey,
					nextStroke, "Find the next fixture matching the pattern",
					evt -> finder.search());
			zoomListener = new ZoomListener((IViewerModel) model);
		} else {
			final ActionListener nullAction = evt -> {
				// do nothing
			};
			gotoTileItem = createMenuItem("Go to tile",
					KeyEvent.VK_T, createHotkey(KeyEvent.VK_T),
					"Go to a tile by coordinates", nullAction);
			gotoTileItem.setEnabled(false);
			findItem = createMenuItem("Find a fixture", findKey,
					findStroke, "Find a fixture by name, kind, or ID#",
					nullAction);
			findItem.setEnabled(false);
			nextItem =
					createMenuItem("Find next", nextKey,
							nextStroke,
							"Find the next fixture matching the pattern",
							nullAction);
			zoomListener = nullAction;
		}
		retval.add(gotoTileItem);
		final InputMap findInput = findItem.getInputMap(WHEN_IN_FOCUSED_WINDOW);
		findInput.put(KeyStroke.getKeyStroke(KeyEvent.VK_SLASH, 0),
				findInput.get(findStroke));
		retval.add(findItem);
		final InputMap nextInput = nextItem.getInputMap(WHEN_IN_FOCUSED_WINDOW);
		nextInput.put(KeyStroke.getKeyStroke(nextKey, 0), nextInput.get(nextStroke));
		retval.add(nextItem);
		retval.addSeparator();
		// VK_PLUS only works on non-US keyboards, but we leave it as the
		// primary hotkey because it's the best to *show* in the menu.
		final KeyStroke plusKey =
				createHotkey(KeyEvent.VK_PLUS);
		final JMenuItem zoomInItem =
				createMenuItem("Zoom in", KeyEvent.VK_I,
						plusKey, "Increase the visible size of each tile",
						zoomListener);
		final InputMap inputMap =
				zoomInItem.getInputMap(WHEN_IN_FOCUSED_WINDOW);
		inputMap.put(createHotkey(KeyEvent.VK_EQUALS),
				inputMap.get(plusKey));
		inputMap.put(createShiftHotkey(KeyEvent.VK_EQUALS),
				inputMap.get(plusKey));
		inputMap.put(createHotkey(KeyEvent.VK_ADD),
				inputMap.get(plusKey));
		retval.add(zoomInItem);
		retval.add(createMenuItem("Zoom out", KeyEvent.VK_O,
				createHotkey(KeyEvent.VK_MINUS),
				"Decrease the visible size of each tile", zoomListener));
		retval.addSeparator();
		retval.add(createMenuItem("Center", KeyEvent.VK_C,
				createHotkey(KeyEvent.VK_C),
				"Center the view on the selected tile", zoomListener));
		retval.addSeparator();
		final PlayerChooserHandler pch = new PlayerChooserHandler(parent, model);
		retval.add(createMenuItem(
				PlayerChooserHandler.MENU_ITEM, KeyEvent.VK_P, null,
				"Mark a player as the current player in the map", pch));
		pch.addPlayerChangeListener((old, newPlayer) -> {
			for (final Player player : model.getMap().players()) {
				if (player.equals(newPlayer)) {
					player.setCurrent(true);
				} else {
					player.setCurrent(false);
				}
			}
		});
		return retval;
	}

	/**
	 * Create the "view" menu.
	 *
	 * @param pch the object to notify when the user selects a different player
	 * @param al  the object to expand and collapse rows of the tree when the user says
	 *            to
	 * @return the "edit" menu
	 */
	protected static JMenu createViewMenu(final PlayerChooserHandler pch,
										  final ActionListener al) {
		final JMenu viewtMenu = new JMenu("View");
		viewtMenu.setMnemonic(KeyEvent.VK_E);
		viewtMenu.add(createMenuItem(
				PlayerChooserHandler.MENU_ITEM, KeyEvent.VK_P,
				createHotkey(KeyEvent.VK_P),
				"Look at a different player's units and workers", pch));
		viewtMenu.add(createMenuItem("Reload tree",
				KeyEvent.VK_R, createHotkey(KeyEvent.VK_R),
				"Refresh the view of the workers", e -> pch.reload()));
		viewtMenu.add(createMenuItem("Expand All", KeyEvent.VK_X, null,
				"Expand all nodes in the unit tree", al));
		viewtMenu.add(
				createMenuItem("Expand Unit Kinds", KeyEvent.VK_K, null,
						"Expand all unit kinds to show the units", al));
		viewtMenu.add(createMenuItem("Collapse All", KeyEvent.VK_C, null,
				"Collapse all nodes in the unit tree", al));
		return viewtMenu;
	}

	/**
	 * Add a menu, but set it to disabled.
	 *
	 * @param menu a menu
	 * @return it
	 */
	protected final JMenu addDisabled(final JMenu menu) {
		add(menu);
		menu.setEnabled(false);
		return menu;
	}
}
