package view.worker;

import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import model.listeners.PlayerChangeListener;
import model.map.Player;
import model.map.fixtures.mobile.IUnit;
import model.map.fixtures.mobile.ProxyUnit;
import model.workermgmt.IWorkerModel;
import org.eclipse.jdt.annotation.Nullable;
import util.ActionWrapper;
import util.NullCleaner;
import util.OnMac;
import view.util.Applyable;
import view.util.BorderedPanel;
import view.util.ListenedButton;
import view.util.Revertible;

/**
 * A panel for the user to enter a unit's orders.
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
 */
public final class OrdersPanel extends BorderedPanel implements Applyable, Revertible,
																		TreeSelectionListener,
																		PlayerChangeListener {
	/**
	 * The worker model to get units from if the user selected a kind.
	 */
	private final IWorkerModel model;
	/**
	 * The current player.
	 */
	private Player player;
	/**
	 * The current selection.
	 */
	@Nullable
	private Object sel = null;

	/**
	 * The text area in which the user writes the orders.
	 */
	private final JTextArea area = new JTextArea();

	/**
	 * Constructor.
	 *
	 * @param workerModel the worker model
	 */
	@SuppressWarnings("StringConcatenationMissingWhitespace")
	public OrdersPanel(final IWorkerModel workerModel) {
		// Can't use the multi-arg constructor, because of the references to
		// 'this' below.
		final boolean onMac = OnMac.SYSTEM_IS_MAC;
		final String prefix;
		final int keyMask;
		if (onMac) {
			prefix = "\u2318";
			keyMask = InputEvent.META_DOWN_MASK;
		} else {
			prefix = "Ctrl+";
			keyMask = InputEvent.CTRL_DOWN_MASK;
		}
		setPageStart(
				new JLabel("Orders for current selection, if a unit: (" + prefix + "D)"))
				.setCenter(new JScrollPane(area)).setPageEnd(new BorderedPanel()
																	.setLineStart(
																			new
																					ListenedButton("Apply", evt -> apply()))
																	.setLineEnd(
																			new ListenedButton("Revert",
																									evt -> revert())));
		area.addKeyListener(new KeyAdapter() {
			/**
			 * @param evt a key-event
			 * @return whether it records the system modifier key being pressed.
			 */
			private boolean isModifierPressed(final KeyEvent evt) {
				if (onMac) {
					return evt.isMetaDown();
				} else {
					return evt.isControlDown();
				}
			}

			@SuppressWarnings("ParameterNameDiffersFromOverriddenParameter")
			@Override
			public void keyPressed(@Nullable final KeyEvent evt) {
				if ((evt != null) && (evt.getKeyCode() == KeyEvent.VK_ENTER)
							&& isModifierPressed(evt)) {
					apply();

				}
			}
		});
		area.setLineWrap(true);
		area.setWrapStyleWord(true);
		model = workerModel;
		final InputMap inputMap = getInputMap(WHEN_IN_FOCUSED_WINDOW);
		final ActionMap actionMap = getActionMap();
		assert (inputMap != null) && (actionMap != null);
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, keyMask), "openOrders");
		// Prevent synthetic access warning
		final JTextArea localArea = area;
		actionMap.put("openOrders", new ActionWrapper(evt ->  {
				final boolean newlyGainingFocus = !localArea.isFocusOwner();
				localArea.requestFocusInWindow();
				if (newlyGainingFocus) {
					localArea.selectAll();
				}
			}));
		player = model.getMap().getCurrentPlayer();
	}

	/**
	 * If a unit is selected, change its orders to what the user wrote.
	 */
	@Override
	public void apply() {
		if (sel instanceof IUnit) {
			final IUnit selection = (IUnit) sel;
			selection.setOrders(model.getMap().getCurrentTurn(), NullCleaner
										.assertNotNull(area.getText().trim()));
			getParent().getParent().repaint();
		}
	}

	/**
	 * Change the text in the area to either the current orders, if a unit is
	 * selected, or
	 * the empty string, if one is not.
	 */
	@Override
	public void revert() {
		if (sel instanceof IUnit) {
			area.setText(((IUnit) sel).getLatestOrders(model.getMap().getCurrentTurn())
								 .trim());
		} else {
			area.setText("");
		}
	}

	/**
	 * @param evt the event to handle
	 */
	@SuppressWarnings("ParameterNameDiffersFromOverriddenParameter")
	@Override
	public void valueChanged(@Nullable final TreeSelectionEvent evt) {
		if (evt != null) {
			final TreePath selPath = evt.getNewLeadSelectionPath();
			if (selPath == null) {
				return;
			}
			sel = selPath.getLastPathComponent();
			if (sel instanceof DefaultMutableTreeNode) {
				sel = ((DefaultMutableTreeNode) sel).getUserObject();
			}
			if (sel instanceof String) {
				final String kind = (String) sel;
				final ProxyUnit proxyUnit = new ProxyUnit(kind);
				model.getUnits(player, kind).forEach(proxyUnit::addProxied);
				sel = proxyUnit;
			}
			revert();
		}
	}

	/**
	 * @param old       the previously selected player
	 * @param newPlayer the newly selected player
	 */
	@Override
	public void playerChanged(@Nullable final Player old, final Player newPlayer) {
		player = newPlayer;
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
	 * @return a quasi-diagnostic String
	 */
	@Override
	public String toString() {
		return "OrdersPanel for player " + player;
	}
}
