package view.map.details;

import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import model.map.HasKind;
import model.map.HasMutableKind;
import model.map.HasMutableName;
import model.map.HasMutableOwner;
import model.map.HasName;
import model.map.HasOwner;
import model.map.IFixture;
import model.map.Player;
import model.map.PlayerCollection;
import model.map.fixtures.UnitMember;
import model.workermgmt.IWorkerTreeModel;
import org.eclipse.jdt.annotation.NonNull;
import util.NullCleaner;

import static javax.swing.JOptionPane.PLAIN_MESSAGE;
import static javax.swing.JOptionPane.YES_NO_OPTION;
import static javax.swing.JOptionPane.YES_OPTION;
import static javax.swing.JOptionPane.showConfirmDialog;
import static javax.swing.JOptionPane.showInputDialog;

/**
 * A pop-up menu to let the user edit a fixture.
 *
 * This is part of the Strategic Primer assistive programs suite developed by Jonathan
 * Lovelace.
 *
 * Copyright (C) 2013-2015 Jonathan Lovelace
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
public final class FixtureEditMenu extends JPopupMenu {
	/**
	 * Listeners to notify about name and kind changes.
	 */
	private final Collection<IWorkerTreeModel> listeners = new ArrayList<>();

	/**
	 * Constructor.
	 *
	 * @param fixture         the fixture the user clicked on
	 * @param players         the players in the map
	 * @param changeListeners any tree-model objects that want to know when something's
	 *                        name or kind has changed.
	 */
	public FixtureEditMenu(final IFixture fixture, final Iterable<Player> players,
						final @NonNull IWorkerTreeModel @NonNull ... changeListeners) {
		Collections.addAll(listeners, changeListeners);
		boolean immutable = true;
		final FixtureEditMenu outer = this;
		if (fixture instanceof HasMutableName) {
			addMenuItem(new JMenuItem("Rename", KeyEvent.VK_N), event -> {
				final String result = (String) showInputDialog(outer,
						"Fixture's new name:", "Rename Fixture",
						PLAIN_MESSAGE, null, null,
						((HasMutableName) fixture).getName());
				if ((result != null) && !result.equals(((HasMutableName) fixture).getName())) {
					((HasMutableName) fixture).setName(result);
					for (final IWorkerTreeModel listener : listeners) {
						listener.renameItem((HasMutableName) fixture);
					}
				}
			});
			immutable = false;
		}
		if (fixture instanceof HasMutableKind) {
			addMenuItem(new JMenuItem("Change kind", KeyEvent.VK_K),
					event -> {
						final String old = ((HasKind) fixture).getKind();
						final String result = (String) showInputDialog(
								outer, "Fixture's new kind:",
								"Change Fixture Kind",
								PLAIN_MESSAGE, null, null,
								((HasKind) fixture).getKind());
						if ((result != null) && !old.equals(result)) {
							((HasMutableKind) fixture).setKind(result);
							for (final IWorkerTreeModel listener : listeners) {
								listener.moveItem((HasKind) fixture);
							}
						}
					});
			immutable = false;
		}
		if (fixture instanceof HasMutableOwner) {
			addMenuItem(new JMenuItem("Change owner", KeyEvent.VK_O),
					event -> {
						final Player result =
								(Player) showInputDialog(outer,
										"Fixture's new owner:",
										"Change Fixture Owner",
										PLAIN_MESSAGE, null,
										playersAsArray(players),
										((HasOwner) fixture).getOwner());
						if (result != null) {
							((HasMutableOwner) fixture).setOwner(result);
						}
					});
			immutable = false;
		}
		if (fixture instanceof UnitMember) {
			final String name;
			if (fixture instanceof HasName) {
				name = ((HasName) fixture).getName();
			} else {
				name = "this " + fixture;
			}
			addMenuItem(new JMenuItem("Dismiss", KeyEvent.VK_D),
					event -> {
						final int reply = showConfirmDialog(
								outer, String.format("Are you sure you want to dismiss %s?", name),
								"Confirm Dismissal", YES_NO_OPTION);
						if (YES_OPTION == reply) {
							for (final IWorkerTreeModel listener : listeners) {
								listener.dismissUnitMember((UnitMember) fixture);
							}
						}
					});
		}
		if (immutable) {
			add(new JLabel("Fixture is not mutable"));
		}
	}

	/**
	 * @param players a collection of players
	 * @return it as an array
	 */
	private static Player[] playersAsArray(final Iterable<Player> players) {
		if (players instanceof PlayerCollection) {
			return ((PlayerCollection) players).asArray();
		} else {
			final List<Player> list = StreamSupport.stream(players.spliterator(), false)
											.collect(Collectors.toList());
			return NullCleaner.assertNotNull(list.toArray(new Player[list.size()]));
		}
	}

	/**
	 * Add a menu item, and attach a suitable listener to it.
	 *
	 * @param item     the menu item
	 * @param listener the listener to listen to it
	 */
	@SuppressWarnings("NonBooleanMethodNameMayNotStartWithQuestion")
	private void addMenuItem(final JMenuItem item, final ActionListener listener) {
		add(item);
		item.addActionListener(listener);
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
	/**
	 * @return a diagnostic String
	 */
	@Override
	public String toString() {
		return "FixtureEditMenu with " + getComponentCount() + " menu items";
	}
}
