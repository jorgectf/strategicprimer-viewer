package view.resources;

import controller.map.misc.IDFactory;
import controller.map.misc.IDFactoryFiller;
import controller.map.misc.IOHandler;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.StreamSupport;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import model.map.Player;
import model.map.fixtures.Implement;
import model.map.fixtures.ResourcePile;
import model.resources.ResourceManagementDriver;
import org.eclipse.jdt.annotation.Nullable;
import util.NullCleaner;
import view.util.BoxPanel;
import view.util.ISPWindow;
import view.util.ImprovedComboBox;
import view.util.SplitWithWeights;
import view.util.StreamingLabel;
import view.worker.WorkerMenu;

/**
 * A window to let the user enter resources etc. Note that this is not a dialog to enter
 * one resource and close.
 *
 * This is part of the Strategic Primer assistive programs suite developed by Jonathan
 * Lovelace.
 *
 * Copyright (C) 2015 Jonathan Lovelace
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
public class ResourceAddingFrame extends JFrame implements ISPWindow {
	/**
	 * The driver model.
	 */
	private final ResourceManagementDriver model;
	/**
	 * The current player.
	 */
	@SuppressWarnings({"FieldMayBeFinal", "CanBeFinal"})
	private Player current;
	/**
	 * The "resource" label.
	 */
	private final JLabel resourceLabel;
	/**
	 * The "implement" label.
	 */
	private final JLabel implementLabel;
	/**
	 * The combo box for resource kinds.
	 */
	private final UpdatedComboBox resKindBox = new UpdatedComboBox();
	/**
	 * The model for the field giving the turn resources were created. See end of
	 * constructor for why the low maximum.
	 */
	private final SpinnerNumberModel resCreatedModel = new SpinnerNumberModel(-1, -1, 2000, 1);
	/**
	 * The parser for integers.
	 */
	private final NumberFormat nf =
			NullCleaner.assertNotNull(NumberFormat.getIntegerInstance());
	/**
	 * The combo box for resource types.
	 */
	private final UpdatedComboBox resourceBox = new UpdatedComboBox();
	/**
	 * The model for the field for resource quantities. See end of constructor for why
	 * the low maximum.
	 */
	private final SpinnerNumberModel resQtyModel = new SpinnerNumberModel(0, 0, 2000, 1);

	/**
	 * The combo box for resource units.
	 */
	private final UpdatedComboBox resUnitsBox = new UpdatedComboBox();
	/**
	 * The model for the spinner to add more than one identical implement. See end of
	 * constructor for why the low maximum.
	 */
	private final SpinnerNumberModel implQtyModel = new SpinnerNumberModel(1, 1, 2000, 1);
	/**
	 * The field to let the user say how many identical implements to add.
	 */
	private final JSpinner implQtyField = new JSpinner(implQtyModel);
	/**
	 * The combo box for implement kinds.
	 */
	private final UpdatedComboBox implKindBox = new UpdatedComboBox();
	/**
	 * The label that we use to display diagnostics.
	 */
	private final StreamingLabel logLabel = new StreamingLabel();
	/**
	 * Whether we have yet to ask the user to choose a player.
	 */
	private boolean playerIsDefault = true;
	/**
	 * Ask the user to choose a player, if the current player is unlikely to be what he
	 * or she wants and we haven't already done so.
	 * @param ioh the menu handler to use to show the dialog
	 */
	private void confirmPlayer(final IOHandler ioh) {
		if (playerIsDefault && current.getName().trim().isEmpty()) {
			ioh.actionPerformed(new ActionEvent(this, 1, "change current player"));
		}
		playerIsDefault = false;
	}
	/**
	 * Constructor.
	 * @param dmodel the driver model
	 * @param ioh the I/O handler for menu items
	 */
	public ResourceAddingFrame(final ResourceManagementDriver dmodel, final IOHandler ioh) {
		super("Resource Entry");
		model = dmodel;
		final IDFactory idf = IDFactoryFiller.createFactory(model);
		current = StreamSupport.stream(dmodel.getPlayers().spliterator(), false)
						  .filter(player -> player.isCurrent())
						  .findAny().orElse(new Player(-1, ""));
		resourceLabel =
				new JLabel(String.format("Add resource for %s:", current.getName()));
		implementLabel =
				new JLabel(String.format("Add equipment for %s:", current.getName()));
		ioh.addPlayerChangeListener((final Player old, @Nullable final Player newPlayer) -> {
			if (newPlayer == null) {
				current = new Player(-1, "");
			} else {
				current = newPlayer;
			}
			resourceLabel
					.setText(String.format("Add resource for %s:", current.getName()));
			implementLabel
					.setText(String.format("Add equipment for %s:", current.getName()));
		});
		final JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
		mainPanel.add(resourceLabel);
		final JPanel panel = new BoxPanel(true);
		addPair(panel, new JLabel("General Category"), resKindBox);
		addPair(panel, new JLabel("Turn created"), new JSpinner(resCreatedModel));
		addPair(panel, new JLabel("Specific Resource"), resourceBox);
		addPair(panel, new JLabel("Quantity"), new JSpinner(resQtyModel));
		addPair(panel, new JLabel("Units"), resUnitsBox);
		final JButton resourceButton = new JButton("Add Resource");
		addPair(panel, new JLabel(""), resourceButton);
		final Component outer = this;
		Function<JComboBox, String> selectedItem = box -> {
			final Object sel = box.getSelectedItem();
			if (sel == null) {
				return "";
			} else {
				return NullCleaner.assertNotNull(sel.toString().trim());
			}
		};
		final ActionListener resListener = evt -> {
			confirmPlayer(ioh);
			final String kind = selectedItem.apply(resKindBox);
			final String resource = selectedItem.apply(resourceBox);
			final String units = selectedItem.apply(resUnitsBox);
			if (kind.isEmpty()) {
				resKindBox.requestFocusInWindow();
				return;
			} else if (resource.isEmpty()) {
				resourceBox.requestFocusInWindow();
				return;
			} else if (units.isEmpty()) {
				resUnitsBox.requestFocusInWindow();
				return;
			}
			final ResourcePile pile = new ResourcePile(idf.createID(), kind,
															  resource,
															  resQtyModel.getNumber()
																	  .intValue(),
															  units);
			pile.setCreated(resCreatedModel.getNumber().intValue());
			model.addResource(pile, current);
			logAddition(pile.toString());
			resKindBox.checkAndClear();
			resCreatedModel.setValue(-1);
			resourceBox.checkAndClear();
			resQtyModel.setValue(0);
			resUnitsBox.checkAndClear();
			resKindBox.requestFocusInWindow();
		};
		resourceButton.addActionListener(resListener);
		BiConsumer<JComboBox, ActionListener> addListener = (box, list) -> {
			final Component inner = box.getEditor().getEditorComponent();
			if (inner instanceof JTextField) {
				((JTextField) inner).addActionListener(list);
			} else {
				System.out.println("Editor wasn't a text field, but a " + inner.getClass().getCanonicalName());
			}
		};
		// Unfortunately, this would fire every time the "selected item" changed!
//		resUnitsBox.addActionListener(resListener);
		addListener.accept(resUnitsBox, resListener);
		mainPanel.add(panel);
		mainPanel.add(Box.createVerticalGlue());
		mainPanel.add(implementLabel);
		final JPanel secondPanel = new BoxPanel(true);
		secondPanel.add(implQtyField);
		secondPanel.add(implKindBox);
		final JButton implButton = new JButton("Add Equipment");
		final ActionListener implListener = evt -> {
			confirmPlayer(ioh);
			final String kind = selectedItem.apply(implKindBox);
			if (kind.isEmpty()) {
				return;
			}
			final int qty = implQtyModel.getNumber().intValue();
			for (int i = 0; i < qty; i++) {
				model.addResource(new Implement(idf.createID(), kind), current);
			}
			logAddition(Integer.toString(qty) + " x " + kind);
			implQtyModel.setValue(1);
			implKindBox.checkAndClear();
			implQtyField.requestFocusInWindow();
		};
		implButton.addActionListener(implListener);
		// Unfortunately, this would fire every time the "selected item" changed!
//		implKindBox.addActionListener(implListener);
		addListener.accept(implKindBox, implListener);
		secondPanel.add(implButton);
		mainPanel.add(secondPanel);
		mainPanel.add(Box.createVerticalGlue());
		logLabel.setMinimumSize(new Dimension(getWidth() - 20, 50));
		logLabel.setPreferredSize(new Dimension(getWidth(), 100));
		final JScrollPane scrolledLog = new JScrollPane(logLabel);
		scrolledLog.setMinimumSize(logLabel.getMinimumSize());
		add(SplitWithWeights.verticalSplit(.2, 0.1, mainPanel, scrolledLog));
		setJMenuBar(new WorkerMenu(ioh, this, model));
		pack();
		// If we set these at model creation, the fields would (try to) be unnecessarily
		// large. Not that this helps.
		resCreatedModel.setMaximum(Integer.MAX_VALUE);
		resQtyModel.setMaximum(Integer.MAX_VALUE);
		implQtyModel.setMaximum(Integer.MAX_VALUE);
	}
	/**
	 * Log the addition of something.
	 * @param addend what was added
	 */
	private void logAddition(final String addend) {
		try (final PrintWriter writer = logLabel.getWriter()) {
			writer.printf("<p style=\"color: white; margin-bottom: 0.5em; margin-top: 0.5em;\">Added %s for %s</p>%n", addend,
					current.getName());
		}
	}
	/**
	 * Add two components in a panel joining them vertically.
	 * @param container the container to add the panel containing the two components to
	 * @param firstComponent the first component
	 * @param secondComponent the second component
	 */
	@SuppressWarnings("NonBooleanMethodNameMayNotStartWithQuestion")
	private static void addPair(final Container container, final Component firstComponent,
	                            final Component secondComponent) {
		final JPanel panel = new BoxPanel(false);
		panel.add(Box.createVerticalGlue());
		panel.add(firstComponent);
		panel.add(Box.createVerticalGlue());
		panel.add(secondComponent);
		panel.add(Box.createVerticalGlue());
		container.add(panel);
	}

	/**
	 * Extends ImprovedComboBox to keep a running collection of values.
	 */
	private static class UpdatedComboBox extends ImprovedComboBox<String> {
		/**
		 * Constructor. We need it to be neither private nor public for this to
		 * work with as few warnings as possible as a private inner class, and
		 * it needs to do something to not be an empty method, so we moved the
		 * initialization of the collection here.
		 */
		protected UpdatedComboBox() {
			values = new HashSet<>();
		}

		/**
		 * The values we've had in the past.
		 */
		private final Collection<String> values;

		/**
		 * Clear the combo box, but if its value was one we haven't had previously, add
		 * it to the drop-down list.
		 */
		@SuppressWarnings("NonBooleanMethodNameMayNotStartWithQuestion")
		public void checkAndClear() {
			final Object raw = getSelectedItem();
			if (raw == null) {
				return;
			}
			final String item = NullCleaner.assertNotNull(getSelectedItem().toString().trim());
			if (!values.contains(item)) {
				values.add(item);
				addItem(item);
			}
			setSelectedItem(null);
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
			return "UpdatedComboBox with " + values.size() + " items";
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
	/**
	 * @return a quasi-diagnostic String
	 */
	@Override
	public String toString() {
		return "ResourceAddingFrame with current player " + current.toString();
	}

	/**
	 * @return the title of this app
	 */
	@Override
	public String getWindowName() {
		return "Resource Entry";
	}
}
