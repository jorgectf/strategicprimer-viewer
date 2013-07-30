package view.worker;

import static model.map.fixtures.mobile.worker.WorkerStats.getModifierString;

import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.TransferHandler;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import model.map.IFixture;
import model.map.Player;
import model.map.PlayerCollection;
import model.map.fixtures.UnitMember;
import model.map.fixtures.mobile.Unit;
import model.map.fixtures.mobile.Worker;
import model.map.fixtures.mobile.worker.WorkerStats;
import model.workermgmt.IWorkerModel;
import model.workermgmt.IWorkerTreeModel;
import model.workermgmt.UnitMemberTransferable;
import model.workermgmt.UnitMemberTransferable.UnitMemberPair;
import model.workermgmt.WorkerTreeModelAlt;
import util.PropertyChangeSource;
import view.map.details.FixtureEditMenu;
/**
 * A tree of a player's units.
 * @author Jonathan Lovelace
 *
 */
public class WorkerTree extends JTree implements PropertyChangeSource {
	/**
	 * @param player the player whose units we want to see
	 * @param model the driver model to build on
	 * @param sources things for the model to listen to for property changes
	 */
	public WorkerTree(final Player player, final IWorkerModel model,
			final PropertyChangeSource... sources) {
		super(new WorkerTreeModelAlt(player, model));
		final WorkerTreeModelAlt tmodel = (WorkerTreeModelAlt) getModel();
		for (final PropertyChangeSource source : sources) {
			source.addPropertyChangeListener(tmodel);
		}
		setRootVisible(false);
		setDragEnabled(true);
		setShowsRootHandles(true);
		setTransferHandler(new WorkerTreeTransferHandler(getSelectionModel(), (IWorkerTreeModel) getModel()));
		setCellRenderer(new UnitMemberCellRenderer());
		addMouseListener(new TreeMouseListener(model.getMap().getPlayers()));
		ToolTipManager.sharedInstance().registerComponent(this);
		addTreeSelectionListener(new WorkerTreeSelectionListener());
	}
	/**
	 * A replacement transfer handler to make drag-and-drop work properly.
	 *
	 * Based on the tutorial found at http://www.javaprogrammingforums.com/java-swing-tutorials/3141-drag-drop-jtrees.html
	 * @author helloworld922
	 * @author Jonathan Lovelace
	 */
	private static class WorkerTreeTransferHandler extends TransferHandler {
		/**
		 * Logger.
		 */
		private static final Logger LOGGER = Logger.getLogger(WorkerTree.WorkerTreeTransferHandler.class.getName());
		/**
		 * Constructor.
		 * @param selmodel the tree's selection model
		 * @param tmodel the tree's data model
		 */
		WorkerTreeTransferHandler(final TreeSelectionModel selmodel, final IWorkerTreeModel tmodel) {
			smodel = selmodel;
			model = tmodel;
		}
		/**
		 * The tree's selection model.
		 */
		private final TreeSelectionModel smodel;
		/**
		 * The tree's data model.
		 */
		private final IWorkerTreeModel model;
		/**
		 * @param component ignored
		 * @return the actions we support
		 */
		@Override
		public int getSourceActions(final JComponent component) {
			return TransferHandler.MOVE;
		}
		/**
		 * @param component the component being dragged from? In any case, ignored.
		 * @return a Transferable representing the selected node, or null if none selected
		 */
		@Override
		protected Transferable createTransferable(final JComponent component) {
			final TreePath path = smodel.getSelectionPath();
			final Object selection = model.getModelObject(path.getLastPathComponent());
			final Object parent = model.getModelObject(path.getPathComponent(path.getPathCount() - 2));
			if (selection instanceof UnitMember && parent instanceof Unit) {
				return new UnitMemberTransferable((UnitMember) selection, // NOPMD
						(Unit) parent);
			} else {
				return null;
			}
		}
		/**
		 * @param support the object containing the detail of the transfer
		 * @return whether the drop is possible
		 */
		@Override
		public boolean canImport(final TransferSupport support) {
			if (support.isDataFlavorSupported(UnitMemberTransferable.FLAVOR)) {
				final DropLocation dloc = support.getDropLocation();
				if (!(dloc instanceof JTree.DropLocation)) {
					return false; // NOPMD
				}
				final TreePath path = ((JTree.DropLocation) dloc).getPath();
				return path != null // NOPMD
						&& (model.getModelObject(path.getLastPathComponent()) instanceof Unit);
			} else {
				return false;
			}
		}
		/**
		 * @param support the object containing the details of the transfer
		 * @return whether the transfer succeeded
		 */
		@Override
		public boolean importData(final TransferSupport support) {
			if (canImport(support)) {
				final Transferable trans = support.getTransferable();
				final DropLocation dloc = support.getDropLocation();
				if (!(dloc instanceof JTree.DropLocation)) {
					return false; // NOPMD
				}
				final TreePath path = ((JTree.DropLocation) dloc).getPath();
				final Object tempTarget = model.getModelObject(path.getLastPathComponent());
				if (tempTarget instanceof Unit) {
					try {
						final UnitMemberTransferable.UnitMemberPair pair = (UnitMemberPair) trans
								.getTransferData(UnitMemberTransferable.FLAVOR);
						model.moveMember(pair.member, pair.unit, (Unit) tempTarget);
						return true; // NOPMD
					} catch (UnsupportedFlavorException except) {
						LOGGER.log(Level.SEVERE, "Impossible unsupported data flavor", except);
						return false; // NOPMD
					} catch (IOException except) {
						LOGGER.log(Level.SEVERE, "I/O error in transfer after we checked ... shouldn't happen", except);
						return false; // NOPMD
					}
				} else {
					return false; // NOPMD
				}
			} else {
				return false;
			}
		}
	}
	/**
	 * A listener to set up pop-up menus.
	 */
	private class TreeMouseListener extends MouseAdapter {
		/**
		 * The collection of players in the map.
		 */
		private final PlayerCollection players;
		/**
		 * Constructor.
		 * @param playerColl the collection of players in the map
		 */
		TreeMouseListener(final PlayerCollection playerColl) {
			players = playerColl;
		}
		/**
		 * @param event the event to handle
		 */
		@Override
		public void mouseClicked(final MouseEvent event) {
			handleMouseEvent(event);
		}
		/**
		 * @param event the event to handle
		 */
		@Override
		public void mousePressed(final MouseEvent event) {
			handleMouseEvent(event);
		}
		/**
		 * @param event the event to handle
		 */
		@Override
		public void mouseReleased(final MouseEvent event) {
			handleMouseEvent(event);
		}
		/**
		 * @param event the event to handle
		 */
		private void handleMouseEvent(final MouseEvent event) {
			if (event.isPopupTrigger() && event.getClickCount() == 1) {
				final Object obj = ((IWorkerTreeModel) getModel())
						.getModelObject(getClosestPathForLocation(event.getX(),
								event.getY()).getLastPathComponent());
				if (obj instanceof IFixture) {
					new FixtureEditMenu((IFixture) obj, players).show(
							event.getComponent(), event.getX(), event.getY());
				}
			}
		}
	}
	/**
	 * @param evt an event indicating the mouse cursor
	 * @return a tooltip if over a worker, null otherwise
	 */
	@Override
	public String getToolTipText(final MouseEvent evt) {
	    if (getRowForLocation(evt.getX(), evt.getY()) == -1) {
			return null; // NOPMD
		}
	    return getStatsToolTip(getPathForLocation(evt.getX(), evt.getY()).getLastPathComponent());
	  }
	/**
	 * @param node a node in the tree
	 * @return a tooltip if it's a worker or a worker node, null otherwise
	 */
	private String getStatsToolTip(final Object node) {
		final Object localNode = ((IWorkerTreeModel) getModel()).getModelObject(node);
		if (localNode instanceof Worker && ((Worker) localNode).getStats() != null) {
			final WorkerStats stats = ((Worker) localNode).getStats();
			return new StringBuilder("<html><p>Str ")// NOPMD
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
		} else {
			return null;
		}
	}
	/**
	 * A selection listener.
	 */
	private class WorkerTreeSelectionListener implements TreeSelectionListener {
		/**
		 * Constuctor.
		 */
		WorkerTreeSelectionListener() {
			// Needed to change visibility.
		}
		/**
		 * @param evt the event to handle
		 */
		@Override
		public void valueChanged(final TreeSelectionEvent evt) {
			handleSelection(((IWorkerTreeModel) getModel()).getModelObject(evt
					.getNewLeadSelectionPath().getLastPathComponent()));
		}
		/**
		 * Handle a selection.
		 * @param sel the new selection
		 */
		@SuppressWarnings("synthetic-access") // TODO: fix this properly
		private void handleSelection(final Object sel) {
			if (sel instanceof UnitMember || sel == null) {
				firePropertyChange("member", null, sel);
			}
		}
	}
}
