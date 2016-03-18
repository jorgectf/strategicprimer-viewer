package controller.map.drivers;

import controller.map.drivers.DriverUsage.ParamCount;
import controller.map.misc.CLIHelper;
import controller.map.misc.ICLIHelper;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import model.map.Player;
import model.map.fixtures.UnitMember;
import model.map.fixtures.mobile.IUnit;
import model.map.fixtures.mobile.IWorker;
import model.map.fixtures.mobile.worker.IJob;
import model.map.fixtures.mobile.worker.ISkill;
import model.map.fixtures.mobile.worker.Job;
import model.map.fixtures.mobile.worker.ProxyWorker;
import model.map.fixtures.mobile.worker.Skill;
import model.misc.IDriverModel;
import model.workermgmt.IWorkerModel;
import model.workermgmt.WorkerModel;
import util.NullCleaner;
import util.SingletonRandom;
import view.util.DriverQuit;

import static view.util.SystemOut.SYS_OUT;

/**
 * A driver to let the user add hours of experience to a player's workers from the command
 * line.
 *
 * This is part of the Strategic Primer assistive programs suite developed by Jonathan
 * Lovelace.
 *
 * Copyright (C) 2015-2015 Jonathan Lovelace
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
public final class AdvancementCLIDriver implements SimpleCLIDriver {
	/**
	 * An object indicating how to use and invoke this driver.
	 */
	private static final DriverUsage USAGE =
			new DriverUsage(false, "-a", "--adv", ParamCount.One,
								   "View a player's workers and manage their " +
										   "advancement",
								   "View a player's units, the workers in those units, " +
										   "each worker's Jobs, and his or her level in " +
										   "each Skill in each Job.",
								   AdvancementCLIDriver.class);
	/**
	 * @return an object indicating how to use and invoke this driver.
	 */
	@Override
	public DriverUsage usage() {
		return USAGE;
	}

	/**
	 * @return a String representation of the object
	 */
	@SuppressWarnings("MethodReturnAlwaysConstant")
	@Override
	public String toString() {
		return "AdvancementCLIDriver";
	}

	/**
	 * Run the driver. This form is, at the moment, primarily for use in test code, but
	 * that may change.
	 *
	 * @param model the driver-model that should be used by the app
	 * @throws DriverFailedException if the driver fails for some reason
	 */
	@Override
	public void startDriver(final IDriverModel model) throws DriverFailedException {
		final IWorkerModel wmodel;
		if (model instanceof IWorkerModel) {
			wmodel = (IWorkerModel) model;
		} else {
			wmodel = new WorkerModel(model);
		}
		final List<Player> playerList = wmodel.getPlayers();
		try (final ICLIHelper cli = new CLIHelper()) {
			final String hdr = "Available players:";
			final String none = "No players found.";
			final String prpt = "Chosen player: ";
			for (int playerNum = cli.chooseFromList(playerList, hdr, none, prpt,
					false); (playerNum >= 0)
									&& (playerNum < playerList.size()); playerNum = cli
																						  .chooseFromList(
																								  playerList,
																								  hdr,
																								  none,
																								  prpt,
																								  false)) {
				advanceWorkers(wmodel,
						NullCleaner.assertNotNull(playerList.remove(playerNum)), cli);
			}
		} catch (final IOException except) {
			throw new DriverFailedException("I/O error interacting with user",
												   except);
		}
	}

	/**
	 * Run the driver.
	 *
	 * @param args the command-line arguments (map files)
	 * @throws DriverFailedException if the driver fails to start
	 */
	@Override
	public void startDriver(final String... args) throws DriverFailedException {
		if (args.length == 0) {
			SYS_OUT.print("Usage: ");
			SYS_OUT.print(getClass().getSimpleName());
			SYS_OUT.println(" map [map ...]");
			DriverQuit.quit(1);
		}
		SimpleCLIDriver.super.startDriver(args);
	}

	/**
	 * Let the user add experience to a player's workers.
	 *
	 * @param model  the driver model
	 * @param player the player whose workers we're interested in
	 * @param cli the interface to the user
	 * @throws IOException on I/O error getting input from user
	 */
	private static void advanceWorkers(final IWorkerModel model,
			final Player player, final ICLIHelper cli) throws IOException {
		final boolean proxy =
				!cli.inputBoolean("Add experience to workers individually? ");
		final List<IUnit> units = model.getUnits(player);
		while (!units.isEmpty()) {
			final int unitNum = cli.chooseFromList(units,
					player.getName() + "'s units:",
					"No unadvanced units remain.", "Chosen unit: ", false);
			if ((unitNum >= 0) && (unitNum < units.size())) {
				if (proxy) {
					advanceSingleWorker(new ProxyWorker(
															   NullCleaner.assertNotNull(
																	   units.remove(
																			   unitNum)
															   )), cli);
				} else {
					advanceWorkersInUnit(
							NullCleaner.assertNotNull(units.remove(unitNum)), cli);
				}
			} else {
				break;
			}
		}
	}

	/**
	 * Let the user add experience to a worker or workers in a unit.
	 *
	 * @param unit the unit in question
	 * @param cli the interface to the user
	 * @throws IOException on I/O error getting input from user
	 */
	private static void advanceWorkersInUnit(final Iterable<UnitMember> unit,
			final ICLIHelper cli) throws IOException {
		final List<IWorker> workers = StreamSupport.stream(unit.spliterator(), false)
											  .filter(IWorker.class::isInstance)
											  .map(IWorker.class::cast)
											  .collect(Collectors.toList());
		while (!workers.isEmpty()) {
			final int workerNum = cli.chooseFromList(workers, "Workers in unit:",
					"No unadvanced workers remain.", "Chosen worker: ", false);
			if ((workerNum >= 0) && (workerNum < workers.size())) {
				advanceSingleWorker(
						NullCleaner.assertNotNull(workers.remove(workerNum)), cli);
			} else {
				break;
			}
		}
	}

	/**
	 * Let the user add experience to a worker.
	 *
	 * @param worker the worker in question
	 * @param cli the interface to the user
	 * @throws IOException on I/O error getting input from user
	 */
	private static void advanceSingleWorker(final IWorker worker,
			final ICLIHelper cli) throws IOException {
		final List<IJob> jobs = CLIHelper.toList(worker);
		final String hdr = "Jobs in worker:";
		final String none = "No existing jobs.";
		final String prpt = "Job to advance: ";
		for (int jobNum = cli.chooseFromList(jobs, hdr, none, prpt, false); jobNum <=
																					jobs
																							  .size();
				jobNum = cli.chooseFromList(jobs, hdr, none, prpt, false)) {
			if ((jobNum < 0) || (jobNum == jobs.size())) {
				worker.addJob(new Job(cli.inputString("Name of new Job: "), 0));
				jobs.clear();
				jobs.addAll(CLIHelper.toList(worker));
				SYS_OUT.println("Select the new job at the next prompt.");
				continue;
			} else {
				advanceJob(NullCleaner.assertNotNull(jobs.get(jobNum)), cli);
				if (!cli.inputBoolean("Select another Job in this worker? ")) {
					break;
				}
			}
		}
	}

	/**
	 * Let the user add hours to a skill or skills in a Job.
	 *
	 * @param job the job in question
	 * @param cli the interface to the user
	 * @throws IOException on I/O error getting input from user
	 */
	private static void advanceJob(final IJob job, final ICLIHelper cli)
			throws IOException {
		final List<ISkill> skills = CLIHelper.toList(job);
		final String hdr = "Jobs in worker:";
		final String none = "No existing jobs.";
		final String prpt = "Job to advance: ";
		for (int skillNum = cli.chooseFromList(skills, hdr, none, prpt, false);
				skillNum <= skills
									.size();
				skillNum = cli.chooseFromList(skills, hdr, none, prpt, false)) {
			if ((skillNum < 0) || (skillNum == skills.size())) {
				job.addSkill(new Skill(cli.inputString("Name of new Skill: "), 0, 0));
				skills.clear();
				skills.addAll(CLIHelper.toList(job));
				SYS_OUT.println("Select the new skill at the next prompt.");
				continue;
			} else {
				skills.get(skillNum)
						.addHours(cli.inputNumber("Hours of experience to add: "),
								SingletonRandom.RANDOM.nextInt(100));
				if (!cli.inputBoolean("Select another Skill in this Job? ")) {
					break;
				}
			}
		}
	}
}
