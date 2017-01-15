package view.worker;

import controller.map.misc.IDRegistrar;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;
import model.listeners.NewWorkerListener;
import model.listeners.UnitSelectionListener;
import model.map.fixtures.mobile.IUnit;
import model.map.fixtures.mobile.Worker;
import model.workermgmt.IWorkerTreeModel;
import org.eclipse.jdt.annotation.Nullable;
import util.TypesafeLogger;

import static view.util.ErrorShower.showErrorDialog;

/**
 * A listener to keep track of the currently selected unit and listen for new-worker
 * notifications, then pass this information on to the tree model.
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
public final class WorkerCreationListener
		implements ActionListener, UnitSelectionListener, NewWorkerListener {
	/**
	 * What to say to the user when a worker is created but no unit is selected.
	 */
	private static final String NO_UNIT_TEXT =
			"As no unit was selected, the new worker wasn't added to a unit.";
	/**
	 * The logger to use for logging.
	 */
	private static final Logger LOGGER =
			TypesafeLogger.getLogger(WorkerCreationListener.class);
	/**
	 * The tree model.
	 */
	private final IWorkerTreeModel treeModel;
	/**
	 * The ID factory to pass to the worker-creation window.
	 */
	private final IDRegistrar idf;
	/**
	 * The current unit. May be null, if nothing is selected.
	 */
	@Nullable
	private IUnit selUnit = null;

	/**
	 * Constructor.
	 *
	 * @param model the tree model
	 * @param idFac the ID factory to pass to the worker-creation window.
	 */
	public WorkerCreationListener(final IWorkerTreeModel model,
								  final IDRegistrar idFac) {
		treeModel = model;
		idf = idFac;
	}

	/**
	 * Update our currently-selected-unit reference.
	 * @param unit the newly selected unit.
	 */
	@Override
	public void selectUnit(@Nullable final IUnit unit) {
		selUnit = unit;
	}

	/**
	 * Handle button press.
	 *
	 * @param evt the event to handle.
	 */
	@SuppressWarnings("ParameterNameDiffersFromOverriddenParameter")
	@Override
	public void actionPerformed(@Nullable final ActionEvent evt) {
		if ((evt != null) &&
					evt.getActionCommand().toLowerCase().startsWith("add worker")) {
			final WorkerConstructionFrame frame = new WorkerConstructionFrame(idf);
			frame.addNewWorkerListener(this);
			frame.setVisible(true);
		}
	}

	/**
	 * Handle a new user-created worker.
	 *
	 * @param worker the worker to handle
	 */
	@Override
	public void addNewWorker(final Worker worker) {
		final IUnit local = selUnit;
		if (local == null) {
			LOGGER.warning("New worker created when no unit selected");
			showErrorDialog(null, NO_UNIT_TEXT);
		} else {
			treeModel.addUnitMember(local, worker);
		}
	}

	/**
	 * A trivial toString().
	 * @return a String representation of the object
	 */
	@SuppressWarnings("MethodReturnAlwaysConstant")
	@Override
	public String toString() {
		return "WorkerCreationListener";
	}
}
