package model.workermgmt;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

import model.map.Player;
import model.map.fixtures.UnitMember;
import model.map.fixtures.mobile.Unit;
import util.IteratorWrapper;
/**
 * An alternative implementation of the worker tree model.
 * @author Jonathan Lovelace
 *
 */
public class WorkerTreeModelAlt extends DefaultTreeModel implements
		IWorkerTreeModel {
	/**
	 * Constructor.
	 * @param player the player whose units and workers will be shown in the tree
	 * @param wmodel the driver model
	 */
	public WorkerTreeModelAlt(final Player player, final IWorkerModel wmodel) {
		super(new PlayerNode(player, wmodel), true);
		model = wmodel;
	}
	/**
	 * The driver model.
	 */
	protected final IWorkerModel model;
	/**
	 * Move a member between units.
	 * @param member a unit member
	 * @param old the prior owner
	 * @param newOwner the new owner
	 */
	@Override
	public void moveMember(final UnitMember member, final Unit old, final Unit newOwner) {
		final PlayerNode pnode = (PlayerNode) root;
		final Player player = (Player) pnode.getUserObject();
		final List<Unit> units = model.getUnits(player);
		final UnitNode oldNode = (UnitNode) pnode.getChildAt(units.indexOf(old));
		final UnitNode newNode = (UnitNode) pnode.getChildAt(units.indexOf(newOwner));
		final Iterable<TreeNode> iter = new IteratorWrapper<TreeNode>(new EnumerationWrapper<TreeNode>(oldNode.children()));
		int index = -1;
		for (TreeNode node : iter) {
			if (node instanceof UnitMemberNode && ((UnitMemberNode) node).getUserObject().equals(member)) {
				index = oldNode.getIndex(node);
			}
		}
		final UnitMemberNode node = (UnitMemberNode) oldNode.getChildAt(index);
		oldNode.remove(node);
		fireTreeNodesRemoved(this, new Object[] { pnode, oldNode } , new int[] { index }, new Object[] { node });
		old.removeMember(member);
		newNode.add(node);
		fireTreeNodesInserted(this, new Object[] { pnode, newNode }, new int[] { newNode.getIndex(node) }, new Object[] { node });
		newOwner.addMember(member);
	}
	/**
	 * A node representing the player.
	 */
	public static class PlayerNode extends DefaultMutableTreeNode {
		/**
		 * Constructor.
		 * @param player the player the node represents
		 * @param model the worker model we're drawing from
		 */
		public PlayerNode(final Player player, final IWorkerModel model) {
			super(player, true);
			int index = 0;
			for (final Unit unit : model.getUnits(player)) {
				insert(new UnitNode(unit), index); // NOPMD
				index++;
			}
		}
	}
	/**
	 * A node representing a unit.
	 */
	public static class UnitNode extends DefaultMutableTreeNode {
		/**
		 * Constructor.
		 * @param unit the unit we represent.
		 */
		public UnitNode(final Unit unit) {
			super(unit, true);
			int index = 0;
			for (final UnitMember member : unit) {
				insert(new UnitMemberNode(member), index); // NOPMD
				index++;
			}
		}
	}
	/**
	 * A node representing a unit member.
	 */
	public static class UnitMemberNode extends DefaultMutableTreeNode {
		/**
		 * Constructor.
		 * @param member the unit member we represent.
		 */
		public UnitMemberNode(final UnitMember member) {
			super(member, false);
		}
	}
	/**
	 * A wrapper around an Enumeration to make it fit the Iterable interface.
	 * @param <T> the type parameter
	 */
	public static class EnumerationWrapper<T> implements Iterator<T> {
		/**
		 * @param enumer the object we're wrapping.
		 */
		public EnumerationWrapper(final Enumeration<T> enumer) {
			wrapped = enumer;
		}
		/**
		 * The object we're wrapping.
		 */
		private final Enumeration<T> wrapped;
		/**
		 * @return whether there are more elements
		 */
		@Override
		public boolean hasNext() {
			return wrapped.hasMoreElements();
		}
		/**
		 * @return the next element
		 * @throws NoSuchElementException if no more elements
		 */
		// ESCA-JAVA0126:
		// ESCA-JAVA0277:
		@Override
		public T next() throws NoSuchElementException {
			return wrapped.nextElement();
		}
		/**
		 * Not supported.
		 */
		@Override
		public void remove() {
			throw new UnsupportedOperationException("Not supported by Enumeration");
		}

	}
}
