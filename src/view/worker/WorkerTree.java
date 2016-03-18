package view.worker;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import model.listeners.UnitMemberListener;
import model.listeners.UnitMemberSelectionSource;
import model.listeners.UnitSelectionListener;
import model.listeners.UnitSelectionSource;
import model.map.IFixture;
import model.map.Player;
import model.map.fixtures.UnitMember;
import model.map.fixtures.mobile.IUnit;
import model.map.fixtures.mobile.IWorker;
import model.map.fixtures.mobile.Worker;
import model.map.fixtures.mobile.worker.ProxyWorker;
import model.map.fixtures.mobile.worker.WorkerStats;
import model.workermgmt.IWorkerTreeModel;
import org.eclipse.jdt.annotation.Nullable;
import util.NullCleaner;
import view.map.details.FixtureEditMenu;

import static java.lang.String.format;
import static model.map.fixtures.mobile.worker.WorkerStats.getModifierString;

/**
 * A tree of a player's units.
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
public final class WorkerTree extends JTree implements UnitMemberSelectionSource,
															   UnitSelectionSource {
	/**
	 * The format string for creating the stats tooltip.
	 */
	private static final String STATS_FMT_STR =
			"<html><p>Str %s, Dex %s, Con %s, Int %s, Wis %s, Cha %s</p></html>";
	/**
	 * The listener to tell other listeners when a new worker has been selected.
	 */
	private final WorkerTreeSelectionListener tsl;

	/**
	 * @param wtModel    the tree model
	 * @param players    the players in the map
	 * @param orderCheck whether we should visually warn if orders contain "todo" or
	 *                   "fixme" or if a unit named "unassigned" is nonempty
	 */
	public WorkerTree(final IWorkerTreeModel wtModel,
					  final Iterable<Player> players, final boolean orderCheck) {
		setModel(wtModel);
		final JTree tree = this;
		wtModel.addTreeModelListener(new TreeModelListener() {
			@Override
			public void treeStructureChanged(@Nullable final TreeModelEvent e) {
				if (e == null) {
					return;
				}
				tree.expandPath(e.getTreePath().getParentPath());
				for (int i = 0; i < getRowCount(); i++) {
					expandRow(i);
				}
				updateUI();
			}

			@Override
			public void treeNodesRemoved(@Nullable final TreeModelEvent e) {
				updateUI();
			}

			@Override
			public void treeNodesInserted(@Nullable final TreeModelEvent e) {
				if (e == null) {
					return;
				}
				tree.expandPath(e.getTreePath());
				tree.expandPath(e.getTreePath().getParentPath());
				updateUI();
			}

			@Override
			public void treeNodesChanged(@Nullable final TreeModelEvent e) {
				if (e == null) {
					return;
				}
				tree.expandPath(e.getTreePath().getParentPath());
				updateUI();
			}
		});
		setRootVisible(false);
		setDragEnabled(true);
		setShowsRootHandles(true);
		setTransferHandler(new WorkerTreeTransferHandler(
																NullCleaner
																		.assertNotNull(
																		getSelectionModel()),

																wtModel));
		setCellRenderer(new UnitMemberCellRenderer(orderCheck));
		addMouseListener(new TreeMouseListener(players, wtModel, this));
		ToolTipManager.sharedInstance().registerComponent(this);
		tsl = new WorkerTreeSelectionListener(wtModel);
		addTreeSelectionListener(tsl);
		for (int i = 0; i < getRowCount(); i++) {
			expandRow(i);
		}
	}

	/**
	 * A listener to set up pop-up menus.
	 *
	 * @author Jonathan Lovelace
	 */
	private static final class TreeMouseListener extends MouseAdapter {
		/**
		 * The collection of players in the map.
		 */
		private final Iterable<Player> players;
		/**
		 * The tree model backing the tree.
		 */
		private final IWorkerTreeModel model;
		/**
		 * The tree we're watching.
		 */
		private final JTree tree;

		/**
		 * Constructor.
		 *
		 * @param playerColl the collection of players in the map
		 * @param tmodel     the tree model backing the tree
		 * @param jtree      the tree we're watching
		 */
		protected TreeMouseListener(final Iterable<Player> playerColl,
									final IWorkerTreeModel tmodel, final JTree jtree) {
			players = playerColl;
			model = tmodel;
			tree = jtree;
		}

		/**
		 * @param event the event to handle
		 */
		@SuppressWarnings("ParameterNameDiffersFromOverriddenParameter")
		@Override
		public void mouseClicked(@Nullable final MouseEvent event) {
			handleMouseEvent(event);
		}

		/**
		 * @param event the event to handle
		 */
		@SuppressWarnings("ParameterNameDiffersFromOverriddenParameter")
		@Override
		public void mousePressed(@Nullable final MouseEvent event) {
			handleMouseEvent(event);
		}

		/**
		 * @param event the event to handle
		 */
		@SuppressWarnings("ParameterNameDiffersFromOverriddenParameter")
		@Override
		public void mouseReleased(@Nullable final MouseEvent event) {
			handleMouseEvent(event);
		}

		/**
		 * @param event the event to handle. Marked @Nullable so we only have to handle
		 *              the null-event case once.
		 */
		private void handleMouseEvent(@Nullable final MouseEvent event) {
			if ((event != null) && event.isPopupTrigger()
						&& (event.getClickCount() == 1)) {
				final TreePath path = tree.getClosestPathForLocation(event.getX(),
						event.getY());
				if (path == null) {
					return;
				}
				final Object pathEnd = path.getLastPathComponent();
				if (pathEnd == null) {
					return;
				}
				final Object obj = model.getModelObject(pathEnd);
				if (obj instanceof IFixture) {
					new FixtureEditMenu((IFixture) obj, players, model).show(
							event.getComponent(), event.getX(), event.getY());
				}
			}
		}

		/**
		 * @return a String representation of the object
		 */
		@SuppressWarnings("MethodReturnAlwaysConstant")
		@Override
		public String toString() {
			return "TreeMouseListener";
		}
	}

	/**
	 * @param event an event indicating the mouse cursor
	 * @return a tooltip if over a worker, null otherwise
	 */
	@SuppressWarnings("ReturnOfNull")
	@Override
	@Nullable
	public String getToolTipText(@Nullable final MouseEvent event) {
		if ((event == null) || (getRowForLocation(event.getX(), event.getY()) == -1)) {
			return null; // NOPMD
		}
		final TreePath path = getPathForLocation(event.getX(), event.getY());
		if (path == null) {
			return null;
		}
		final Object pathLast = path.getLastPathComponent();
		if (pathLast == null) {
			return null; // NOPMD
		}
		return getStatsToolTip(pathLast);
	}

	/**
	 * @param node a node in the tree
	 * @return a tooltip if it's a worker or a worker node, null otherwise
	 */
	@Nullable
	private String getStatsToolTip(final Object node) {
		final Object localNode = ((IWorkerTreeModel) getModel())
										 .getModelObject(node);
		if (localNode instanceof Worker) {
			final WorkerStats stats = ((Worker) localNode).getStats();
			if (stats == null) {
				return null; // NOPMD
			} else {
				return format(STATS_FMT_STR, // NOPMD
						getModifierString(stats.getStrength()),
						getModifierString(stats.getDexterity()),
						getModifierString(stats.getConstitution()),
						getModifierString(stats.getIntelligence()),
						getModifierString(stats.getWisdom()),
						getModifierString(stats.getCharisma()));
			}
		} else if (localNode instanceof ProxyWorker) {
			for (final IWorker proxied : ((ProxyWorker) localNode).getProxied()) {
				if (proxied instanceof Worker) {
					final Worker worker = (Worker) proxied;
					final WorkerStats stats = worker.getStats();
					if (stats == null) {
						continue;
					} else {
						//noinspection TailRecursion
						return getStatsToolTip(worker);
					}
				} else {
					final String ttip = getStatsToolTip(proxied);
					if (ttip != null) {
						return ttip;
					}
				}
			}
			return null;
		} else {
			return null;
		}
	}

	/**
	 * A selection listener.
	 *
	 * @author Jonathan Lovelace
	 */
	private static final class WorkerTreeSelectionListener implements
			TreeSelectionListener, UnitMemberSelectionSource,
					UnitSelectionSource {
		/**
		 * The list of unit-selection listeners listening to us.
		 */
		private final Collection<UnitSelectionListener> selectionListeners =
				new ArrayList<>();
		/**
		 * The list of listeners to notify of newly selected unit member.
		 */
		private final Collection<UnitMemberListener> umListeners = new ArrayList<>();

		/**
		 * The tree model to refer to.
		 */
		protected final IWorkerTreeModel model;

		/**
		 * Constructor.
		 *
		 * @param tmodel the tree model to refer to
		 */
		protected WorkerTreeSelectionListener(final IWorkerTreeModel tmodel) {
			model = tmodel;
		}

		/**
		 * @param evt the event to handle
		 */
		@SuppressWarnings("ParameterNameDiffersFromOverriddenParameter")
		@Override
		public void valueChanged(@Nullable final TreeSelectionEvent evt) {
			if (evt != null) {
				final TreePath path = evt.getNewLeadSelectionPath();
				if (path == null) {
					return;
				}
				final Object pathLast = path
												.getLastPathComponent();
				if (pathLast != null) {
					handleSelection(model.getModelObject(pathLast));
				}
			}
		}

		/**
		 * Handle a selection.
		 *
		 * @param sel the new selection. Might be null.
		 */
		private void handleSelection(@Nullable final Object sel) {
			if ((sel instanceof UnitMember) || (sel == null)) {
				for (final UnitMemberListener list : umListeners) {
					list.memberSelected(null, (UnitMember) sel);
				}
			}
			if (sel instanceof IUnit) {
				for (final UnitSelectionListener list : selectionListeners) {
					list.selectUnit((IUnit) sel);
				}
				for (final UnitMemberListener list : umListeners) {
					//noinspection ObjectAllocationInLoop
					list.memberSelected(null, new ProxyWorker((IUnit) sel));
				}
			} else if (sel == null) {
				for (final UnitSelectionListener list : selectionListeners) {
					list.selectUnit(null);
				}
			}
		}

		/**
		 * @param list a listener to add
		 */
		@Override
		public void addUnitSelectionListener(final UnitSelectionListener list) {
			selectionListeners.add(list);
		}

		/**
		 * @param list a listener to remove
		 */
		@Override
		public void removeUnitSelectionListener(final UnitSelectionListener list) {
			selectionListeners.remove(list);
		}

		/**
		 * @param list a listener to add
		 */
		@Override
		public void addUnitMemberListener(final UnitMemberListener list) {
			umListeners.add(list);
		}

		/**
		 * @param list a listener to remove
		 */
		@Override
		public void removeUnitMemberListener(final UnitMemberListener list) {
			umListeners.remove(list);
		}

		/**
		 * @return a String representation of the object
		 */
		@SuppressWarnings("MethodReturnAlwaysConstant")
		@Override
		public String toString() {
			return "WorkerTreeSelectionListener";
		}
	}

	/**
	 * @param list the listener to add
	 */
	@Override
	public void addUnitSelectionListener(final UnitSelectionListener list) {
		tsl.addUnitSelectionListener(list);
	}

	/**
	 * @param list the listener to remove
	 */
	@Override
	public void removeUnitSelectionListener(final UnitSelectionListener list) {
		tsl.removeUnitSelectionListener(list);
	}

	/**
	 * @param list the listener to add
	 */
	@Override
	public void addUnitMemberListener(final UnitMemberListener list) {
		tsl.addUnitMemberListener(list);
	}

	/**
	 * @param list the listener to remove
	 */
	@Override
	public void removeUnitMemberListener(final UnitMemberListener list) {
		tsl.removeUnitMemberListener(list);
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
	 * @return a quasi-diagnostic String.
	 */
	@Override
	public String toString() {
		return "WorkerTree showing " + getRowCount() + " rows";
	}
}
