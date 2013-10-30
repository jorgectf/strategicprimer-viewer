package view.worker;

import static model.map.fixtures.mobile.worker.WorkerStats.getModifierString;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeSelectionModel;

import model.listeners.NewUnitListener;
import model.listeners.PlayerChangeListener;
import model.listeners.UnitMemberListener;
import model.listeners.UnitMemberSelectionSource;
import model.listeners.UnitSelectionListener;
import model.listeners.UnitSelectionSource;
import model.map.IFixture;
import model.map.Player;
import model.map.PlayerCollection;
import model.map.fixtures.UnitMember;
import model.map.fixtures.mobile.Unit;
import model.map.fixtures.mobile.Worker;
import model.map.fixtures.mobile.worker.WorkerStats;
import model.workermgmt.IWorkerModel;
import model.workermgmt.IWorkerTreeModel;
import model.workermgmt.WorkerTreeModelAlt;

import org.eclipse.jdt.annotation.Nullable;

import view.map.details.FixtureEditMenu;

/**
 * A tree of a player's units.
 *
 * @author Jonathan Lovelace
 *
 */
public class WorkerTree extends JTree implements UnitMemberSelectionSource,
		UnitSelectionSource, NewUnitListener, PlayerChangeListener {
	/**
	 * @param player the player whose units we want to see
	 * @param model the driver model to build on
	 */
	public WorkerTree(final Player player, final IWorkerModel model) {
		tmodel = new WorkerTreeModelAlt(player, model);
		setModel(tmodel);
		setRootVisible(false);
		setDragEnabled(true);
		setShowsRootHandles(true);
		final TreeSelectionModel smodel = getSelectionModel();
		assert smodel != null;
		setTransferHandler(new WorkerTreeTransferHandler(smodel,
				tmodel));
		setCellRenderer(new UnitMemberCellRenderer());
		addMouseListener(new TreeMouseListener(model.getMap().getPlayers()));
		ToolTipManager.sharedInstance().registerComponent(this);
		addTreeSelectionListener(tsl);
	}

	/**
	 * A listener to set up pop-up menus.
	 * @author Jonathan Lovelace
	 */
	private class TreeMouseListener extends MouseAdapter {
		/**
		 * The collection of players in the map.
		 */
		private final PlayerCollection players;

		/**
		 * Constructor.
		 *
		 * @param playerColl the collection of players in the map
		 */
		protected TreeMouseListener(final PlayerCollection playerColl) {
			players = playerColl;
		}

		/**
		 * @param event the event to handle
		 */
		@Override
		public void mouseClicked(@Nullable final MouseEvent event) {
			handleMouseEvent(event);
		}

		/**
		 * @param event the event to handle
		 */
		@Override
		public void mousePressed(@Nullable final MouseEvent event) {
			handleMouseEvent(event);
		}

		/**
		 * @param event the event to handle
		 */
		@Override
		public void mouseReleased(@Nullable final MouseEvent event) {
			handleMouseEvent(event);
		}

		/**
		 * @param event the event to handle. Marked @Nullable so we only have to
		 *        handle the null-event case once.
		 */
		private void handleMouseEvent(@Nullable final MouseEvent event) {
			if (event != null && event.isPopupTrigger()
					&& event.getClickCount() == 1) {
				final Object path = getClosestPathForLocation(event.getX(),
								event.getY()).getLastPathComponent();
				if (path ==  null) {
					return;
				}
				final Object obj = ((IWorkerTreeModel) getModel())
						.getModelObject(path);
				if (obj instanceof IFixture) {
					new FixtureEditMenu((IFixture) obj, players).show(
							event.getComponent(), event.getX(), event.getY());
				}
			}
		}
		/**
		 * @return a String representation of the object
		 */
		@Override
		public String toString() {
			return "TreeMouseListener";
		}
	}

	/**
	 * @param evt an event indicating the mouse cursor
	 * @return a tooltip if over a worker, null otherwise
	 */
	@Override
	@Nullable
	public String getToolTipText(@Nullable final MouseEvent evt) {
		if (evt == null || getRowForLocation(evt.getX(), evt.getY()) == -1) {
			return null; // NOPMD
		}
		final Object path = getPathForLocation(evt.getX(), evt.getY())
				.getLastPathComponent();
		if (path == null) {
			return null; // NOPMD
		}
		return getStatsToolTip(path);
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
				return null;
			} else {
				return new StringBuilder(92)
						// NOPMD
						.append("<html><p>Str ")
						.append(getModifierString(stats.getStrength()))
						.append(", Dex ")
						.append(getModifierString(stats.getDexterity()))
						.append(", Con ")
						.append(getModifierString(stats.getConstitution()))
						.append(", Int ")
						.append(getModifierString(stats.getIntelligence()))
						.append(", Wis ")
						.append(getModifierString(stats.getWisdom()))
						.append(", Cha ")
						.append(getModifierString(stats.getCharisma()))
						.append("</p></html>").toString();
			}
		} else {
			return null;
		}
	}

	/**
	 * A selection listener.
	 * @author Jonathan Lovelace
	 */
	private class WorkerTreeSelectionListener implements TreeSelectionListener, UnitMemberSelectionSource, UnitSelectionSource {
		/**
		 * Constructor.
		 */
		protected WorkerTreeSelectionListener() {
			// Needed to change visibility.
		}

		/**
		 * @param evt the event to handle
		 */
		@Override
		public void valueChanged(@Nullable final TreeSelectionEvent evt) {
			if (evt != null) {
				final Object path = evt.getNewLeadSelectionPath()
						.getLastPathComponent();
				if (path != null) {
					handleSelection(((IWorkerTreeModel) getModel())
							.getModelObject(path));
				}
			}
		}

		/**
		 * Handle a selection.
		 *
		 * @param sel the new selection. Might be null.
		 */
		private void handleSelection(@Nullable final Object sel) {
			if (sel instanceof UnitMember || sel == null) {
				for (final UnitMemberListener list : umListeners) {
					list.memberSelected(null, (UnitMember) sel);
				}
			}
			if (sel instanceof Unit || sel == null) {
				for (final UnitSelectionListener list : usListeners) {
					list.selectUnit((Unit) sel);
				}
			}
		}
		/**
		 * @param list a listener to add
		 */
		@Override
		public void addUnitSelectionListener(final UnitSelectionListener list) {
			usListeners.add(list);
		}

		/**
		 * @param list a listener to remove
		 */
		@Override
		public void removeUnitSelectionListener(final UnitSelectionListener list) {
			usListeners.remove(list);
		}
		/**
		 * The list of unit-selection listeners listening to us.
		 */
		private final List<UnitSelectionListener> usListeners = new ArrayList<>();
		/**
		 * The list of listeners to notify of newly selected unit member.
		 */
		private final List<UnitMemberListener> umListeners = new ArrayList<>();

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
		@Override
		public String toString() {
			return "WorkerTreeSelectionListener";
		}
	}

	/**
	 * The tree model.
	 */
	private final WorkerTreeModelAlt tmodel;
	/**
	 * The listener to tell other listeners when a new worker has been selected.
	 */
	private final WorkerTreeSelectionListener tsl = new WorkerTreeSelectionListener();

	/**
	 * @param unit passed to the tree model
	 */
	@Override
	public void addNewUnit(final Unit unit) {
		tmodel.addNewUnit(unit);
	}
	/**
	 * @param old passed to tree model
	 * @param newPlayer passed to tree model
	 */
	@Override
	public void playerChanged(@Nullable final Player old, final Player newPlayer) {
		tmodel.playerChanged(old, newPlayer);
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
}
