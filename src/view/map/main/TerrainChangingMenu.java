package view.map.main;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.eclipse.jdt.annotation.Nullable;

import controller.map.misc.IDFactoryFiller;
import model.listeners.SelectionChangeListener;
import model.listeners.SelectionChangeSource;
import model.listeners.SelectionChangeSupport;
import model.listeners.VersionChangeListener;
import model.map.Point;
import model.map.PointFactory;
import model.map.TileType;
import model.misc.IDriverModel;
import model.viewer.IViewerModel;
import view.worker.NewUnitDialog;

/**
 * A popup menu to let the user change a tile's terrain type, or add a unit.
 *
 * This is part of the Strategic Primer assistive programs suite developed by Jonathan
 * Lovelace.
 *
 * Copyright (C) 2011-2015 Jonathan Lovelace
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
public final class TerrainChangingMenu extends JPopupMenu
		implements VersionChangeListener, SelectionChangeSource, SelectionChangeListener {
	/**
	 * The driver model.
	 */
	private final IDriverModel model;
	/**
	 * The point at which we might change terrain.
	 */
	private Point point = PointFactory.point(-1, -1);

	/**
	 * The helper to handle selection-change listeners for us.
	 */
	private final SelectionChangeSupport scs = new SelectionChangeSupport();
	/**
	 * The menu item to allow the user to create a new unit.
	 */
	private final JMenuItem newUnitItem = new JMenuItem("Add New Unit");
	/**
	 * The window to allow the user to create a new unit.
	 */
	private final NewUnitDialog nuDialog;

	/**
	 * Constructor.
	 *
	 * @param version the map version
	 * @param dmodel  the driver model
	 */
	public TerrainChangingMenu(final int version, final IViewerModel dmodel) {
		model = dmodel;
		nuDialog = new NewUnitDialog(dmodel.getMap().getCurrentPlayer(),
				                            IDFactoryFiller
						                            .createFactory(dmodel.getMap()));
		nuDialog.addNewUnitListener(unit -> {
			dmodel.getMap().addFixture(point, unit);
			dmodel.setSelection(point);
			scs.fireChanges(null, point);
		});
		newUnitItem.addActionListener(e -> nuDialog.setVisible(true));
		updateForVersion(version);
	}

	/**
	 * Update the menu for a new version.
	 *
	 * @param version the version
	 */
	private void updateForVersion(final int version) {
		removeAll();
		for (final TileType type : TileType.valuesForVersion(version)) {
			final JMenuItem item = new JMenuItem(type.toString()); // NOPMD
			add(item);
			item.addActionListener(evt -> {
				model.getMap().setBaseTerrain(point, type);
				scs.fireChanges(null, point);
			});
		}
		addSeparator();
		add(newUnitItem);
	}

	/**
	 * @param old        the previously selected version
	 * @param newVersion the newly selected version
	 */
	@Override
	public void changeVersion(final int old, final int newVersion) {
		updateForVersion(newVersion);
	}

	/**
	 * @param list a listener to add
	 */
	@Override
	public void addSelectionChangeListener(
			                                      final SelectionChangeListener list) {
		scs.addSelectionChangeListener(list);
	}

	/**
	 * @param list a listener to remove
	 */
	@Override
	public void removeSelectionChangeListener(
			                                         final SelectionChangeListener
					                                         list) {
		scs.removeSelectionChangeListener(list);
	}

	/**
	 * @param old      ignored
	 * @param newPoint ignored
	 */
	@Override
	public void selectedPointChanged(@Nullable final Point old,
	                                 final Point newPoint) {
		point = newPoint;
		if (TileType.NotVisible == model.getMap().getBaseTerrain(newPoint)) {
			newUnitItem.setEnabled(false);
		} else {
			newUnitItem.setEnabled(true);
		}
	}
	/**
	 * Prevent serialization.
	 * @param out ignored
	 * @throws IOException always
	 */
	@SuppressWarnings({ "unused", "static-method" })
	private void writeObject(final ObjectOutputStream out) throws IOException {
		throw new NotSerializableException("Serialization is not allowed");
	}
	/**
	 * Prevent serialization
	 * @param in ignored
	 * @throws IOException always
	 * @throws ClassNotFoundException never
	 */
	@SuppressWarnings({ "unused", "static-method" })
	private void readObject(final ObjectInputStream in)
			throws IOException, ClassNotFoundException {
		throw new NotSerializableException("Serialization is not allowed");
	}
}
