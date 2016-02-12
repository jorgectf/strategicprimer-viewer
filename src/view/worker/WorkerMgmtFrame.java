package view.worker;

import com.bric.window.WindowList;
import controller.map.misc.IDFactoryFiller;
import controller.map.misc.IOHandler;
import controller.map.report.ReportGenerator;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.FileWriter;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.WindowConstants;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import model.listeners.MapChangeListener;
import model.listeners.PlayerChangeListener;
import model.map.DistanceComparator;
import model.map.HasName;
import model.map.Player;
import model.map.Point;
import model.map.PointFactory;
import model.map.TileFixture;
import model.map.fixtures.UnitMember;
import model.map.fixtures.mobile.IUnit;
import model.map.fixtures.mobile.IWorker;
import model.map.fixtures.mobile.Worker;
import model.map.fixtures.mobile.worker.IJob;
import model.map.fixtures.towns.Fortress;
import model.misc.IDriverModel;
import model.report.IReportNode;
import model.report.SimpleReportNode;
import model.viewer.IViewerModel;
import model.viewer.ViewerModel;
import model.workermgmt.IWorkerModel;
import model.workermgmt.IWorkerTreeModel;
import model.workermgmt.WorkerTreeModelAlt;
import model.workermgmt.WorkerTreeModelAlt.PlayerNode;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import util.ActionWrapper;
import util.NullCleaner;
import util.TypesafeLogger;
import view.map.main.ViewerFrame;
import view.util.BorderedPanel;
import view.util.ListenedButton;
import view.util.SplitWithWeights;
import view.util.SystemOut;

import static java.util.logging.Level.SEVERE;
import static javax.swing.JFileChooser.APPROVE_OPTION;

/**
 * A window to let the player manage units.
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
public final class WorkerMgmtFrame extends JFrame {
	/**
	 * The logger.
	 */
	private static final Logger LOGGER = TypesafeLogger
												 .getLogger(WorkerMgmtFrame.class);
	/**
	 * The header to put above the report.
	 */
	private static final String RPT_HDR =
			"The contents of the world you know about, for reference:";
	/**
	 * A constant for when a split panel should be divided evenly in half.
	 */
	private static final double HALF_WAY = 0.5;
	/**
	 * A constant for when a split panel should be divided not quite evenly.
	 */
	private static final double TWO_THIRDS = 2.0 / 3.0;

	/**
	 * At this point (proof-of-concept) we default to the first player of the choices.
	 *
	 * @param model     the driver model.
	 * @param ioHandler the I/O handler, so we can handle 'open' and 'save' menu items.
	 */
	public WorkerMgmtFrame(final IWorkerModel model, final IOHandler ioHandler) {
		super("Worker Management");
		if (model.getMapFile().exists()) {
			setTitle(model.getMapFile().getName() + " | Worker Management");
			getRootPane().putClientProperty("Window.documentFile",
					model.getMapFile());
		}
		setMinimumSize(new Dimension(640, 480));
		final NewUnitDialog newUnitFrame =
				new NewUnitDialog(model.getMap().getCurrentPlayer(),
										 IDFactoryFiller.createFactory(model.getMap()));
		final IWorkerTreeModel wtmodel =
				new WorkerTreeModelAlt(model.getMap().getCurrentPlayer(), model);
		final WorkerTree tree =
				new WorkerTree(wtmodel, model.getMap().players(), true);
		ioHandler.addPlayerChangeListener(wtmodel);
		newUnitFrame.addNewUnitListener(wtmodel);
		final boolean onMac = System.getProperty("os.name").toLowerCase()
									  .startsWith("mac os x");
		final int keyMask;
		final String keyDesc;
		if (onMac) {
			keyMask = InputEvent.META_DOWN_MASK;
			keyDesc = ": (\u2318U)";
		} else {
			keyMask = InputEvent.CTRL_DOWN_MASK;
			keyDesc = ": (Ctrl+U)";
		}
		final InputMap inputMap = tree.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		final ActionMap actionMap = tree.getActionMap();
		assert (inputMap != null) && (actionMap != null);
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_U, keyMask), "openUnits");
		actionMap.put("openUnits", new FocusRequester(tree));
		final PlayerLabel plabl = new PlayerLabel("Units belonging to ",
														  model.getMap()
																  .getCurrentPlayer(),
														  keyDesc);
		ioHandler.addPlayerChangeListener(plabl);
		ioHandler.addPlayerChangeListener(newUnitFrame);
		final OrdersPanel ordersPanel = new OrdersPanel(model);
		ioHandler.addPlayerChangeListener(ordersPanel);
		ordersPanel.playerChanged(null, model.getMap().getCurrentPlayer());
		tree.addTreeSelectionListener(ordersPanel);
		final Component outer = this;
		final DefaultTreeModel reportModel =
				new DefaultTreeModel(new SimpleReportNode("Please wait, loading report " +
																  "..."));
		new Thread(new ReportGeneratorThread(reportModel, model,
													model.getMap().getCurrentPlayer()))
				.start();
		final JTree report = new JTree(reportModel);
		report.setRootVisible(false);
		report.expandPath(new TreePath(((DefaultMutableTreeNode) reportModel
																		 .getRoot())
											   .getPath()));
		final ReportUpdater reportUpdater = new ReportUpdater(model,
																	 reportModel);
		@NonNull
		Point hqLoc = PointFactory.point(-1, -1);
		boolean found = false;
		for (final Point location : model.getMap().locations()) {
			if (found) {
				break;
			} else {
				for (final TileFixture fix : model.getMap().getOtherFixtures(location)) {
					if ((fix instanceof Fortress) && ((Fortress) fix).getOwner()
															 .equals(model.getMap()
																			 .getCurrentPlayer())) {
						if ("HQ".equals(((Fortress) fix).getName())) {
							hqLoc = location;
							found = true;
							break;
						} else if ((hqLoc.row < 0) && (location.row >= 0)) {
							hqLoc = location;
							break;
						}
					}
				}
			}
		}
		final DistanceComparator distCalculator = new DistanceComparator(hqLoc);
		report.setCellRenderer(
				(renderedTree, value, selected, expanded, leaf, row, hasFocus) -> {
					final TreeCellRenderer defRender = new DefaultTreeCellRenderer();
					final Component retval = defRender.getTreeCellRendererComponent(
							renderedTree, value, selected, expanded, leaf, row,
							hasFocus);
					if (value instanceof IReportNode) {
						final Point point = ((IReportNode) value).getPoint();
						// (-inf, -inf) replaces null
						if (point.getRow() > Integer.MIN_VALUE) {
							((JComponent) retval)
									.setToolTipText(distCalculator.distanceString
																		   (point));
						} else {
							((JComponent) retval).setToolTipText(null);
						}
					}
					assert retval != null;
					return retval;
				});
		ToolTipManager.sharedInstance().registerComponent(report);
		report.addMouseListener(new reportMouseHandler(report, model, ioHandler));
		ioHandler.addPlayerChangeListener(reportUpdater);
		model.addMapChangeListener(reportUpdater);
		final MemberDetailPanel mdp = new MemberDetailPanel();
		tree.addUnitMemberListener(mdp);
		final StrategyExporter strategyExporter = new StrategyExporter(model, wtmodel);
		// TODO: Make a JFileChooser subclass or wrapper that takes what to do with the
		// chosen file as a parameter
		setContentPane(SplitWithWeights.horizontal(HALF_WAY, HALF_WAY,
				SplitWithWeights.vertical(TWO_THIRDS, TWO_THIRDS,
						new BorderedPanel(new JScrollPane(tree), plabl, null, null, null),
						new BorderedPanel(ordersPanel, new ListenedButton("Add New Unit",
								                                                 evt ->
										                                                 newUnitFrame
										                                                        .setVisible(
												                                                        true)),

								                 new ListenedButton("Export a " +
										                                    "proto-strategy from units' orders",
										                                   evt -> {
											                                   final
											                                   JFileChooser
													                                   chooser =
													                                   new JFileChooser(".");
											                                   if (chooser.showSaveDialog(
													                                   outer) ==
													                                       APPROVE_OPTION) {
												                                   try (final FileWriter writer = new FileWriter(chooser.getSelectedFile())) {
													                                   writer.append(
															                                   strategyExporter
																	                                   .createStrategy());
												                                   } catch (final IOException except) {
													                                   LOGGER.log(
															                                   SEVERE,
															                                   "I/O error exporting strategy",
															                                   except);
												                                   }
											                                   }
										                                   }), null,
								                 null)),
				new BorderedPanel(new JScrollPane(report), new JLabel(RPT_HDR), mdp, null,
						                 null)));
		ioHandler.addTreeExpansionListener(new TreeExpansionHandler(tree));
		setJMenuBar(new WorkerMenu(ioHandler, this, model));
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		for (int i = 0; i < tree.getRowCount(); i++) {
			tree.expandRow(i);
		}
		pack();
	}

	/**
	 * @param model the driver-model of the worker-management GUI
	 * @param ioh   the I/O handler
	 * @return the viewer model of a viewer window the same map as that in the given
	 * driver-model
	 */
	protected static IViewerModel getViewerModelFor(final IDriverModel model,
													final IOHandler ioh) {
		for (final Frame frame : WindowList.getFrames(false, true, true)) {
			if ((frame instanceof ViewerFrame) && ((ViewerFrame) frame).getModel()
														  .getMapFile()
														  .equals(model.getMapFile())) {
				frame.toFront();
				if (frame.getExtendedState() == Frame.ICONIFIED) {
					frame.setExtendedState(Frame.NORMAL);
				}
				return ((ViewerFrame) frame).getModel();
			}
		}
		final ViewerFrame frame = new ViewerFrame(
														 new ViewerModel(model.getMap(),
																				model
																						.getMapFile()),
														 ioh);
		frame.setVisible(true);
		return frame.getModel();
	}

	/**
	 * A class to update the report when a new map is loaded.
	 *
	 * @author Jonathan Lovelace
	 */
	private static final class ReportUpdater implements PlayerChangeListener,
																MapChangeListener {
		/**
		 * The driver model, to get the map from.
		 */
		private final IWorkerModel model;
		/**
		 * The pane that we update.
		 */
		private final DefaultTreeModel reportModel;

		/**
		 * Constructor.
		 *
		 * @param wmodel The driver model to get the map from
		 * @param tmodel the tree model we update
		 */
		protected ReportUpdater(final IWorkerModel wmodel,
								final DefaultTreeModel tmodel) {
			model = wmodel;
			reportModel = tmodel;
		}

		/**
		 * Handle notification that a new map was loaded.
		 */
		@Override
		public void mapChanged() {
			new Thread(new ReportGeneratorThread(reportModel, model, model.getMap()
																			 .getCurrentPlayer()))
					.start();
		}

		/**
		 * Handle change in current player.
		 *
		 * @param old       the previous current player
		 * @param newPlayer the new current player
		 */
		@Override
		public void playerChanged(@Nullable final Player old,
								  final Player newPlayer) {
			new Thread(new ReportGeneratorThread(reportModel, model, newPlayer)).start();
		}

		/**
		 * @return a String representation of the object
		 */
		@SuppressWarnings("MethodReturnAlwaysConstant")
		@Override
		public String toString() {
			return "ReportUpdater";
		}
	}

	/**
	 * A class to export a "proto-strategy" to file.
	 *
	 * @author Jonathan Lovelace
	 */
	public static final class StrategyExporter {
		/**
		 * The worker model.
		 */
		private final IWorkerModel model;
		/**
		 * Unit members that have been dismissed.
		 */
		private final IWorkerTreeModel tmodel;

		/**
		 * Constructor.
		 *
		 * @param wmodel    the driver model to draw from
		 * @param treeModel the tree model to get dismissed unit members from
		 */
		public StrategyExporter(final IWorkerModel wmodel,
								final IWorkerTreeModel treeModel) {
			model = wmodel;
			tmodel = treeModel;
		}

		/**
		 * @return the proto-strategy as a String
		 */
		@SuppressWarnings("TypeMayBeWeakened")
		public String createStrategy() {
			final Player currentPlayer;
			final Object treeRoot = tmodel.getRoot();
			if (treeRoot instanceof PlayerNode) {
				currentPlayer = ((PlayerNode) treeRoot).getUserObject();
			} else if (treeRoot instanceof Player) {
				currentPlayer = (Player) treeRoot;
			} else {
				currentPlayer = model.getMap().getCurrentPlayer();
			}
			final String playerName = currentPlayer.getName();
			final String turn = Integer.toString(model.getMap().getCurrentTurn());
			final List<IUnit> units = model.getUnits(currentPlayer);

			final Map<String, List<IUnit>> unitsByKind = new HashMap<>();
			for (final IUnit unit : units) {
				if (!unit.iterator().hasNext()) {
					// FIXME: This should be exposed as a user option. Sometimes
					// users *want* empty units printed.
					continue;
				}
				final List<IUnit> list; // NOPMD
				if (unitsByKind.containsKey(unit.getKind())) {
					list = unitsByKind.get(unit.getKind());
				} else {
					list = new ArrayList<>(); // NOPMD
					unitsByKind.put(unit.getKind(), list);
				}
				list.add(unit);
			}

			int size = 58 + playerName.length() + turn.length();
			for (final Entry<String, List<IUnit>> entry : unitsByKind.entrySet()) {
				size += 4;
				size += entry.getKey().length();
				for (final IUnit unit : entry.getValue()) {
					size += 10;
					size += unit.getName().length();
					size += unitMemberSize(unit);
					size += unit.getOrders().length();
				}
			}
			final Iterable<UnitMember> dismissed = tmodel.dismissed();
			for (final UnitMember member : dismissed) {
				size += 2;
				if (member instanceof HasName) {
					size += ((HasName) member).getName().length();
				} else {
					size += member.toString().length();
				}
			}
			final StringBuilder builder = new StringBuilder(size);
			builder.append('[');
			builder.append(playerName);
			builder.append("\nTurn ");
			builder.append(turn);
			builder.append("]\n\nInventions: TODO: any?\n\n");
			if (dismissed.iterator().hasNext()) {
				builder.append("Dismissed workers etc.: ");
				String separator = "";
				for (final UnitMember member : dismissed) {
					builder.append(separator);
					separator = ", ";
					if (member instanceof HasName) {
						builder.append(((HasName) member).getName());
					} else {
						builder.append(member.toString());
					}
				}
				builder.append("\n\n");
			}
			builder.append("Workers:\n");
			for (final Entry<String, List<IUnit>> entry : unitsByKind.entrySet()) {
				builder.append("* ");
				builder.append(entry.getKey());
				builder.append(":\n");
				for (final IUnit unit : entry.getValue()) {
					builder.append("  - ");
					builder.append(unit.getName());
					builder.append(unitMembers(unit));
					builder.append(":\n\n");
					final String orders = unit.getOrders().trim();
					if (orders.isEmpty()) {
						builder.append("TODO");
					} else {
						builder.append(orders);
					}
					builder.append("\n\n");
				}
			}
			return NullCleaner.assertNotNull(builder.toString());
		}

		/**
		 * @param unit a unit
		 * @return the size of string needed to represent its members
		 */
		private static int unitMemberSize(final Iterable<UnitMember> unit) {
			if (unit.iterator().hasNext()) {
				int size = 3;
				for (final UnitMember member : unit) {
					size += 2;
					size += memberStringSize(NullCleaner.assertNotNull(member));
				}
				return size;
			} else {
				return 0;
			}
		}

		/**
		 * @param unit a unit
		 * @return a String representing its members
		 */
		private static String unitMembers(final Iterable<UnitMember> unit) {
			if (unit.iterator().hasNext()) {
				// Assume at least two K.
				final StringBuilder builder = new StringBuilder(2048)
													  .append(" [");
				boolean first = true;
				for (final UnitMember member : unit) {
					if (first) {
						first = false;
					} else {
						builder.append(", ");
					}
					builder.append(memberString(member));
				}
				builder.append(']');
				return NullCleaner.assertNotNull(builder.toString()); // NOPMD
			} else {
				return "";
			}
		}

		/**
		 * @param member a unit member
		 * @return the size of a string for it
		 */
		private static int memberStringSize(final UnitMember member) {
			if (member instanceof Worker) {
				int size = ((Worker) member).getName().length();
				size += 2;
				for (final IJob job : (IWorker) member) {
					size += 3;
					size += job.getName().length();
					size += Integer.toString(job.getLevel()).length();
				}
				return size;
			} else {
				return member.toString().length();
			}
		}

		/**
		 * @param member a unit member
		 * @return a suitable string for it
		 */
		private static String memberString(final UnitMember member) {
			if (member instanceof IWorker) {
				final IWorker worker = (IWorker) member;
				// To save calculations, assume a half-K every time.
				final StringBuilder builder = new StringBuilder(512)
													  .append(worker.getName());
				if (worker.iterator().hasNext()) {
					builder.append(" (");
					boolean first = true;
					for (final IJob job : worker) {
						if (first) {
							first = false;
						} else {
							builder.append(", ");
						}
						builder.append(job.getName());
						builder.append(' ');
						builder.append(job.getLevel());
					}
					builder.append(')');
				}
				return NullCleaner.assertNotNull(builder.toString()); // NOPMD
			} else {
				return NullCleaner.assertNotNull(member.toString());
			}
		}

		/**
		 * @return a String representation of the object
		 */
		@SuppressWarnings("MethodReturnAlwaysConstant")
		@Override
		public String toString() {
			return "StrategyExporter";
		}
	}

	/**
	 * A thread to generate the report tree in the background.
	 */
	protected static final class ReportGeneratorThread implements Runnable {
		/**
		 * A logger for the thread.
		 */
		private static final Logger RGT_LOGGER =
				TypesafeLogger.getLogger(ReportGeneratorThread.class);
		/**
		 * The tree-model to put the report into.
		 */
		protected final DefaultTreeModel tmodel;
		/**
		 * The worker model to generate the report from.
		 */
		private final IWorkerModel wmodel;
		/**
		 * The player to generate the report for.
		 */
		private final Player player;

		/**
		 * Constructor.
		 *
		 * @param treeModel     The tree-model to put the report into.
		 * @param workerModel   the driver model to generate the report from
		 * @param currentPlayer the player to generate the report for
		 */
		protected ReportGeneratorThread(final DefaultTreeModel treeModel,
										final IWorkerModel workerModel,
										final Player currentPlayer) {
			tmodel = treeModel;
			wmodel = workerModel;
			player = currentPlayer;
		}

		/**
		 * Run the thread.
		 */
		@Override
		public void run() {
			RGT_LOGGER.info("About to generate report");
			final IReportNode report =
					ReportGenerator.createAbbreviatedReportIR(wmodel.getMap(), player);
			RGT_LOGGER.info("Finished generating report");
			SwingUtilities.invokeLater(() -> tmodel.setRoot(report));
		}
		/**
		 * @return a String representation of the thread
		 */
		@Override
		public String toString() {
			return "Generating report for " + player;
		}
	}

	/**
	 * An action to request focus in a component.
	 */
	private static class FocusRequester extends ActionWrapper {
		/**
		 * The type of component we're handling.
		 */
		private final String type;
		/**
		 * Constructor.
		 * @param comp The component to request focus in.
		 */
		protected FocusRequester(final WorkerTree comp) {
			super(evt -> comp.requestFocusInWindow());
			type = comp.getClass().getSimpleName();
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
		 * @return a String representation of the action
		 */
		@Override
		public String toString() {
			return "Requesting focus in a " + type;
		}
	}

	/**
	 * Handler for mouse clicks in the report tree.
	 */
	private static class reportMouseHandler extends MouseAdapter {
		/**
		 * The report tree.
		 */
		private final JTree report;
		/**
		 * The driver model.
		 */
		private final IWorkerModel model;
		/**
		 * The menu-item handler.
		 */
		private final IOHandler ioh;

		/**
		 * Constructor.
		 * @param reportTree The report tree.
		 * @param workerModel the driver model
		 * @param ioHandler the menu-item handler
		 */
		protected reportMouseHandler(final JTree reportTree,
									 final IWorkerModel workerModel,
									 final IOHandler ioHandler) {
			report = reportTree;
			model = workerModel;
			ioh = ioHandler;
		}

		/**
		 * Handle a mouse press.
		 * @param evt the event to handle
		 */
		@Override
		public void mousePressed(final @Nullable MouseEvent evt) {
			if (evt == null) {
				SystemOut.SYS_OUT.println("MouseEvent was null");
				return;
			}
			final TreePath selPath = report.getPathForLocation(evt.getX(), evt.getY());
			if (selPath == null) {
				return;
			}
			final Object node = selPath.getLastPathComponent();
			if ((evt.isControlDown() || evt.isMetaDown()) &&
						(node instanceof IReportNode)) {
				final Point point = ((IReportNode) node).getPoint();
				// (-inf, -inf) replaces null
				if (point.getRow() > Integer.MIN_VALUE) {
					final IViewerModel vModel =
							getViewerModelFor(model, ioh);
					SwingUtilities.invokeLater(() -> vModel.setSelection(point));
				}
			}
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
