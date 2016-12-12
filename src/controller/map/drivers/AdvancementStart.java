package controller.map.drivers;

import controller.map.misc.ICLIHelper;
import controller.map.misc.IOHandler;
import controller.map.misc.MenuBroker;
import javax.swing.SwingUtilities;
import model.misc.IDriverModel;
import model.workermgmt.IWorkerModel;
import model.workermgmt.WorkerModel;
import view.worker.AdvancementFrame;

/**
 * A class to start the worker management GUI.
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
public final class AdvancementStart implements SimpleDriver {
	/**
	 * An object indicating how to use and invoke this driver.
	 */
	private static final DriverUsage USAGE =
			new DriverUsage(true, "-a", "--adv", ParamCount.AtLeastOne,
								   "View a player's workers and manage their " +
										   "advancement",
								   "View a player's units, the workers in those units, " +
										   "each worker's Jobs, and his or her level in" +
										   " each Skill in each Job.");

	static {
		USAGE.addSupportedOption("--current-turn=NN");
	}

	/**
	 * Run the driver
	 *
	 * @param cli the interface for user I/O
	 * @param options command-line options passed in
	 * @param model   the driver model
	 */
	@Override
	public void startDriver(final ICLIHelper cli, final SPOptions options,
							final IDriverModel model) {
		final IWorkerModel workerModel;
		if (model instanceof IWorkerModel) {
			workerModel = (IWorkerModel) model;
		} else {
			workerModel = new WorkerModel(model);
		}
		final IOHandler ioh = new IOHandler(workerModel);
		final MenuBroker menuHandler = new MenuBroker();
		menuHandler.register(ioh, "load", "save", "save as", "new", "about",
				"load secondary", "save all", "open in map viewer",
				"open secondary map in map viewer", "go to tile", "close",
				"find a fixture", "find next", "change current player", "reload tree",
				"zoom in", "zoom out", "center", "quit");
		SwingUtilities.invokeLater(
				() -> {
					final AdvancementFrame frame =
							new AdvancementFrame(workerModel, menuHandler);
					ioh.addPlayerChangeListener(frame);
					frame.setVisible(true);
				});
	}

	/**
	 * @return an object indicating how to use and invoke this driver.
	 */
	@Override
	public IDriverUsage usage() {
		return USAGE;
	}

	/**
	 * @return a String representation of the object
	 */
	@SuppressWarnings("MethodReturnAlwaysConstant")
	@Override
	public String toString() {
		return "AdvancementStart";
	}
}
