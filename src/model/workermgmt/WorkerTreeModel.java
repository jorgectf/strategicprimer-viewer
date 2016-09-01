package model.workermgmt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.StreamSupport;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreePath;
import model.map.HasKind;
import model.map.HasMutableName;
import model.map.Player;
import model.map.fixtures.UnitMember;
import model.map.fixtures.mobile.IUnit;
import org.eclipse.jdt.annotation.Nullable;
import util.NullCleaner;
import util.TypesafeLogger;

/**
 * A TreeModel implementation for a player's units and workers.
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
public final class WorkerTreeModel implements IWorkerTreeModel {
	/**
	 * The player to whom the units and workers belong, the root of the tree.
	 */
	private Player root;
	/**
	 * The driver model.
	 */
	private final IWorkerModel model;
	/**
	 * A list of unit members that have been dismissed.
	 */
	private final Collection<UnitMember> dismissedMembers = new ArrayList<>();
	/**
	 * The listeners registered to listen for model changes.
	 */
	private final Collection<TreeModelListener> listeners = new ArrayList<>();

	/**
	 * Logger.
	 */
	private static final Logger LOGGER = TypesafeLogger.getLogger(WorkerTreeModel.class);

	/**
	 * Constructor.
	 *
	 * @param player the player whose units and workers will be shown in the tree
	 * @param workerModel the driver model
	 */
	public WorkerTreeModel(final Player player, final IWorkerModel workerModel) {
		root = player;
		model = workerModel;
	}

	/**
	 * @return the root of the tree, the player.
	 */
	@Override
	public Player getRoot() {
		return root;
	}

	/**
	 * @param parent an object in the tree
	 * @param index  the index of the child we want
	 * @return the specified child
	 */
	@SuppressWarnings("ProhibitedExceptionThrown")
	@Override
	public Object getChild(@Nullable final Object parent, final int index) {
		if (index < 0) {
			throw new ArrayIndexOutOfBoundsException(index);
		} else if ((parent instanceof Player) && parent.equals(root) &&
						(index < model.getUnitKinds(root).size())) {
			return NullCleaner.assertNotNull(model.getUnitKinds(root).get(index));
		} else if ((parent instanceof String) &&
						model.getUnitKinds(root).contains(parent) &&
						(index < model.getUnits(root, (String) parent).size())) {
			// A String here is a unit's kind.
			return NullCleaner.assertNotNull(model.getUnits(root,
					(String) parent).get(index));
		} else if (parent instanceof IUnit) {
			final Iterator<UnitMember> iter = ((IUnit) parent).iterator();
			for (int i = 0; i < index; i++) {
				if (iter.hasNext()) {
					iter.next();
				} else {
					throw new ArrayIndexOutOfBoundsException(index);
				}
			}
			if (iter.hasNext()) {
				return NullCleaner.assertNotNull(iter.next());
			} else {
				throw new ArrayIndexOutOfBoundsException(index);
			}
		} else {
			throw new ArrayIndexOutOfBoundsException("Unrecognized parent");
		}
	}

	/**
	 * @param parent an object in the tree
	 * @return how many children it has
	 */
	@Override
	public int getChildCount(@Nullable final Object parent) {
		if (parent instanceof Player) {
			return model.getUnits((Player) parent).size();
		} else if (parent instanceof IUnit) {
			return (int) StreamSupport.stream(((IUnit) parent).spliterator(), false)
								.count();
		} else {
			throw new IllegalArgumentException("Not a possible member of the tree");
		}
	}

	/**
	 * @param node a node in the tree
	 * @return whether it's a leaf node
	 */
	@Override
	public boolean isLeaf(@Nullable final Object node) {
		return !(node instanceof Player) && !(node instanceof IUnit) &&
					!(node instanceof String);
	}

	/**
	 * @param path     a path indicating a node
	 * @param newValue the new value for that place
	 */
	@Override
	public void valueForPathChanged(@Nullable final TreePath path,
									@Nullable final Object newValue) {
		LOGGER.severe("valueForPathChanged needs to be implemented");
	}

	/**
	 * @param parent an object presumably in the tree
	 * @param child  something that's presumably one of its children
	 * @return which child it is, or -1 if preconditions broken
	 */
	@Override
	public int getIndexOfChild(@Nullable final Object parent,
							@Nullable final Object child) {
		if ((parent instanceof Player) && parent.equals(root) && (child instanceof IUnit)) {
			return model.getUnits(root).indexOf(child);
		} else if (parent instanceof IUnit) {
			int index = 0;
			for (final UnitMember member : (IUnit) parent) {
				if (member.equals(child)) {
					return index;
				}
				index++;
			}
			return -1;
		} else {
			return -1;
		}
	}

	/**
	 * @param list something to listen for tree model changes
	 */
	@SuppressWarnings("ParameterNameDiffersFromOverriddenParameter")
	@Override
	public void addTreeModelListener(@Nullable final TreeModelListener list) {
		if (list != null) {
			listeners.add(list);
		}
	}

	/**
	 * @param list something that doesn't want to listen for tree model changes anymore
	 */
	@SuppressWarnings("ParameterNameDiffersFromOverriddenParameter")
	@Override
	public void removeTreeModelListener(@Nullable final TreeModelListener list) {
		listeners.remove(list);
	}

	/**
	 * Move a member between units.
	 *
	 * @param member   a unit member
	 * @param old      the prior owner
	 * @param newOwner the new owner
	 */
	@Override
	public void moveMember(final UnitMember member, final IUnit old,
						final IUnit newOwner) {
		final int oldIndex = getIndexOfChild(old, member);
		old.removeMember(member);
		final TreeModelEvent removedEvent =
				new TreeModelEvent(this, new TreePath(asArray(root, old)),
										singletonInt(oldIndex), singletonObj(member));
		final TreeModelEvent removedChEvent =
				new TreeModelEvent(this, new TreePath(asArray(root, old)));
		for (final TreeModelListener listener : listeners) {
			listener.treeNodesRemoved(removedEvent);
			listener.treeStructureChanged(removedChEvent);
		}
		newOwner.addMember(member);
		final TreeModelEvent insertedEvent =
				new TreeModelEvent(this, new TreePath(asArray(root, newOwner)),
										singletonInt(getIndexOfChild(newOwner, member)),
										singletonObj(member));
		final TreeModelEvent insertedChEvent =
				new TreeModelEvent(this, new TreePath(asArray(root, newOwner)));
		for (final TreeModelListener listener : listeners) {
			listener.treeNodesInserted(insertedEvent);
			listener.treeStructureChanged(insertedChEvent);
		}
	}

	/**
	 * Add a unit.
	 *
	 * @param unit the unit to add
	 */
	@Override
	public void addUnit(final IUnit unit) {
		model.addUnit(unit);
		final TreePath path = new TreePath(root);
		final int[] indices = singletonInt(model.getUnits(root).size());
		final Object[] children = singletonObj(unit);
		final TreeModelEvent event = new TreeModelEvent(this, path, indices, children);
		for (final TreeModelListener listener : listeners) {
			listener.treeNodesInserted(event);
		}
	}

	/**
	 * Create a singleton array.
	 *
	 * @param obj the object it should contain
	 * @return the array
	 */
	private static Object[] singletonObj(final Object obj) {
		return new Object[]{obj};
	}

	/**
	 * Create a singleton array.
	 *
	 * @param num the integer it should contain
	 * @return the array
	 */
	private static int[] singletonInt(final int num) {
		return new int[]{num};
	}

	/**
	 * Handle the user's request to add a unit.
	 *
	 * @param unit the unit to add.
	 */
	@Override
	public void addNewUnit(final IUnit unit) {
		addUnit(unit);
	}

	/**
	 * Handle notification that a new map was loaded.
	 */
	@Override
	public void mapChanged() {
		root = model.getMap().getCurrentPlayer();
		final TreePath path = new TreePath(root);
		final TreeModelEvent event = new TreeModelEvent(this, path);
		for (final TreeModelListener listener : listeners) {
			listener.treeNodesChanged(event);
		}
	}

	/**
	 * @param old       the old current player
	 * @param newPlayer the new current player
	 */
	@Override
	public void playerChanged(@Nullable final Player old, final Player newPlayer) {
		root = newPlayer;
		final TreePath path = new TreePath(root);
		final TreeModelEvent event = new TreeModelEvent(this, path);
		for (final TreeModelListener listener : listeners) {
			listener.treeNodesChanged(event);
		}
	}

	/**
	 * @param obj an object
	 * @return it
	 */
	@Override
	public Object getModelObject(final Object obj) {
		return obj;
	}

	/**
	 * Add a member to a unit.
	 *
	 * @param unit   the unit to contain the member
	 * @param member the member to add to it
	 */
	@Override
	public void addUnitMember(final IUnit unit, final UnitMember member) {
		unit.addMember(member);
		final TreePath path = new TreePath(asArray(root, unit));
		final int[] indices = singletonInt(getIndexOfChild(unit, member));
		final Object[] children = singletonObj(member);
		final TreeModelEvent event = new TreeModelEvent(this, path, indices, children);
		for (final TreeModelListener listener : listeners) {
			listener.treeNodesInserted(event);
		}
	}

	/**
	 * @return a String representation of the object
	 */
	@Override
	public String toString() {
		return "WorkerTreeModel representing units of player " + Objects.toString(root);
	}

	/**
	 * @param item the item that has changed
	 */
	@Override
	public void renameItem(final HasMutableName item) {
		final TreePath path;
		final int[] indices;
		final Object[] children;
		if (item instanceof IUnit) {
			path = new TreePath(singletonObj(root));
			indices = singletonInt(getIndexOfChild(root, item));
			children = singletonObj(item);
		} else if (item instanceof UnitMember) {
			IUnit parent = null;
			boolean found = false;
			for (final IUnit unit : model.getUnits(root)) {
				for (final UnitMember member : unit) {
					if (item.equals(member)) {
						found = true;
						parent = unit;
					}
				}
				if (found) {
					break;
				}
			}
			if (!found) {
				return;
			}
			path = new TreePath(asArray(root, parent));
			indices = singletonInt(getIndexOfChild(parent, item));
			children = singletonObj(item);
		} else {
			// Probably the player. In any case, ignore.
			return;
		}
		final TreeModelEvent event = new TreeModelEvent(this, path, indices, children);
		for (final TreeModelListener listener : listeners) {
			listener.treeNodesChanged(event);
		}
	}

	/**
	 * @param item the item to move to this point in the tree
	 */
	@Override
	public void moveItem(final HasKind item) {
		final TreePath path;
		final int[] indices;
		final Object[] children;
		if (item instanceof IUnit) {
			path = new TreePath(singletonObj(root));
			indices = singletonInt(getIndexOfChild(root, item));
			children = singletonObj(item);
		} else if (item instanceof UnitMember) {
			IUnit parent = null;
			for (final IUnit unit : model.getUnits(root)) {
				for (final UnitMember member : unit) {
					if (item.equals(member)) {
						parent = unit;
					}
				}
				//noinspection VariableNotUsedInsideIf
				if (parent != null) {
					break;
				}
			}
			if (parent == null) {
				return;
			}
			path = new TreePath(asArray(root, parent));
			indices = singletonInt(getIndexOfChild(parent, item));
			children = singletonObj(item);
		} else {
			// Impossible at present, so ignore
			return;
		}
		final TreeModelEvent event = new TreeModelEvent(this, path, indices, children);
		for (final TreeModelListener listener : listeners) {
			listener.treeNodesChanged(event);
		}
	}
	/**
	 * @param args a series of objects
	 * @return them as an array
	 */
	private static Object[] asArray(final Object... args) {
		return args;
	}
	/**
	 * Dismiss a member from a unit and the player's service.
	 *
	 * @param member the member to dismiss
	 */
	@Override
	public void dismissUnitMember(final UnitMember member) {
		for (final IUnit unit : model.getUnits(root)) {
			for (final UnitMember item : unit) {
				if (item.equals(member)) {
					final int index = getIndexOfChild(unit, item);
					dismissedMembers.add(member);
					unit.removeMember(member);
					final TreeModelEvent evt =
							new TreeModelEvent(this, new TreePath(asArray(root, unit)),
													singletonInt(index),
													singletonObj(member));
					for (final TreeModelListener listener : listeners) {
						listener.treeNodesRemoved(evt);
					}
					break;
				}
			}
		}
	}

	/**
	 * @return the unit members that have been dismissed.
	 */
	@Override
	public Iterable<UnitMember> dismissed() {
		return new ArrayList<>(dismissedMembers);
	}
}
