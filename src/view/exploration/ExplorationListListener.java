package view.exploration;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import model.exploration.IExplorationModel;
import model.map.TileFixture;
import model.map.fixtures.mobile.IUnit;
import model.map.fixtures.mobile.SimpleMovement;
import util.NullCleaner;
import view.map.details.FixtureList;

/**
 * A list-data-listener to select a random but suitable set of fixtures to be 'discovered'
 * if the tile is explored.
 *
 * This is part of the Strategic Primer assistive programs suite developed by Jonathan
 * Lovelace.
 *
 * Copyright (C) 2013-2014 Jonathan Lovelace
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
public final class ExplorationListListener implements ListDataListener {
	/**
	 * The exploration model, which tells us the currently selected unit and tile.
	 */
	private final IExplorationModel model;
	/**
	 * The list this is attached to.
	 */
	private final FixtureList list;

	/**
	 * Constructor.
	 *
	 * @param mainList the list this is attached to
	 * @param emodel   the exploration model
	 */
	public ExplorationListListener(final IExplorationModel emodel,
	                               final FixtureList mainList) {
		model = emodel;
		list = mainList;
	}

	/**
	 * @param evt an event indicating items were removed from the list
	 */
	@SuppressWarnings("ParameterNameDiffersFromOverriddenParameter")
	@Override
	public void intervalRemoved(@Nullable final ListDataEvent evt) {
		randomizeSelection();
	}

	/**
	 * @param evt an event indicating items were added to the list
	 */
	@SuppressWarnings("ParameterNameDiffersFromOverriddenParameter")
	@Override
	public void intervalAdded(@Nullable final ListDataEvent evt) {
		randomizeSelection();
	}

	/**
	 * @param evt an event indicating items were changed in the list
	 */
	@SuppressWarnings("ParameterNameDiffersFromOverriddenParameter")
	@Override
	public void contentsChanged(@Nullable final ListDataEvent evt) {
		randomizeSelection();
	}

	/**
	 * Like a Pair<Integer, T>, but without the headaches induced by boxing an int into
	 * Integer.
	 *
	 * TODO: If we start using Guava, use of this class should be replaced by
	 * Multiset, or
	 * something?
	 *
	 * @param <T> the type in question.
	 * @author Jonathan Lovelace
	 */
	private static final class IntPair<@NonNull T> {
		/**
		 * The number in the pair.
		 */
		private final int number;
		/**
		 * The object in the pair.
		 */
		private final T object;

		/**
		 * Factory method.
		 *
		 * @param num the number in the pair
		 * @param obj the object in the pair
		 * @param <I> the type of object
		 * @return the pair
		 */
		protected static <@NonNull I> IntPair<I> of(final int num, final I obj) {
			return new IntPair<>(num, obj);
		}

		/**
		 * Constructor. Use the factory method rather than this constructor.
		 *
		 * @param num the number in the pair
		 * @param obj the object in the pair
		 */
		protected IntPair(final int num, final T obj) {
			number = num;
			object = obj;
		}

		/**
		 * @return the number in the pair
		 */
		public int first() {
			return number;
		}

		/**
		 * @return the object in the pair
		 */
		@SuppressWarnings("unused")
		public T second() {
			return object;
		}

		/**
		 * @return a String representation of the object
		 */
		@Override
		public String toString() {
			final String objStr = object.toString();
			return String.format("(%d, %s)", Integer.valueOf(number), objStr);
		}
	}

	/**
	 * Select a suitable but randomized selection of fixtures. Do nothing if there is no
	 * selected unit.
	 */
	private void randomizeSelection() {
		final IUnit selUnit = model.getSelectedUnit();
		if (selUnit != null) {
			list.clearSelection();
			final List<IntPair<TileFixture>> constants = new ArrayList<>();
			final List<IntPair<TileFixture>> possibles = new ArrayList<>();
			int i = 0;
			for (final TileFixture fix : new ListModelWrapper<>(
					NullCleaner.assertNotNull(list.getModel()))) {
				if (SimpleMovement.shouldAlwaysNotice(selUnit, fix)) {
					constants.add(IntPair.of(i, fix));
				} else if (SimpleMovement.shouldSometimesNotice(selUnit, fix)) {
					possibles.add(IntPair.of(i, fix));
				}
				i++;
			}
			Collections.shuffle(possibles);
			if (!possibles.isEmpty()) {
				constants.add(possibles.get(0));
			}
			final int[] indices = new int[constants.size()];
			for (int index = 0; index < constants.size(); index++) {
				indices[index] = constants.get(index).first();
			}
			list.setSelectedIndices(indices);
		}
	}

	/**
	 * @return a String representation of the object
	 */
	@SuppressWarnings("MethodReturnAlwaysConstant")
	@Override
	public String toString() {
		return "ExplorationListListener";
	}
	/**
	 * A wrapper around a ListModel.
	 * @param <E> the type of thing in the list model.
	 */
	private static class ListModelWrapper<E> extends AbstractList<E> {
		/**
		 * The wrapped object.
		 */
		private final ListModel<E> wrapped;
		/**
		 * @param lmodel the wrapped object
		 */
		protected ListModelWrapper(final ListModel<E> lmodel) {
			wrapped = lmodel;
		}

		/**
		 * @param index an index
		 * @return the object at that index
		 */
		@Override
		public E get(final int index) {
			return wrapped.getElementAt(index);
		}

		/**
		 * @return the size of the list-model
		 */
		@Override
		public int size() {
			return wrapped.getSize();
		}
		/**
		 * @return a diagnostic String
		 */
		@Override
		public String toString() {
			return "Wrapper around the following ListModel:\n" + wrapped.toString();
		}
	}
}
