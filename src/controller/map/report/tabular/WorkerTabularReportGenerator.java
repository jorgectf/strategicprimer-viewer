package controller.map.report.tabular;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.ToIntFunction;
import model.map.DistanceComparator;
import model.map.IFixture;
import model.map.Player;
import model.map.Point;
import model.map.fixtures.mobile.IWorker;
import model.map.fixtures.mobile.Worker;
import model.map.fixtures.mobile.worker.WorkerStats;
import util.Pair;
import util.PatientMap;

/**
 * A report generator for workers. We do not cover Jobs or Skills; see the main report
 * for that.
 *
 * This is part of the Strategic Primer assistive programs suite developed by Jonathan
 * Lovelace.
 *
 * Copyright (C) 2016 Jonathan Lovelace
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
public class WorkerTabularReportGenerator implements ITableGenerator<IWorker> {
	/**
	 * The player for whom this report is being produced.
	 */
	private final Player player;
	/**
	 * His or her HQ location.
	 */
	private final Point base;
	/**
	 * Constructor.
	 * @param currentPlayer the player for whom this report is being produced
	 * @param hq his or her HQ location
	 */
	public WorkerTabularReportGenerator(final Player currentPlayer, final Point hq) {
		player = currentPlayer;
		base = hq;
	}

	/**
	 * @param ostream the stream to write the row to
	 * @param fixtures the set of fixtures
	 * @param item the worker to base the line on
	 * @param loc its location
	 * @throws IOException on I/O error writing to the stream
	 */
	@Override
	public boolean produce(final Appendable ostream,
						   final PatientMap<Integer, Pair<Point, IFixture>> fixtures,
						   final IWorker item, final Point loc) throws IOException {
		writeField(ostream, distanceString(loc, base));
		writeFieldDelimiter(ostream);
		writeField(ostream, loc.toString());
		writeFieldDelimiter(ostream);
		writeField(ostream, item.getName());
		writeFieldDelimiter(ostream);
		final WorkerStats stats;
		if (item instanceof Worker) {
			stats = ((Worker) item).getStats();
		} else {
			stats = null;
		}
		if (stats == null) {
			for (int i = 0; i < 9; i++) {
				writeField(ostream, "--");
				writeFieldDelimiter(ostream);
			}
		} else {
			writeField(ostream, Integer.toString(stats.getHitPoints()));
			writeFieldDelimiter(ostream);
			writeField(ostream, Integer.toString(stats.getMaxHitPoints()));
			for (ToIntFunction<WorkerStats> field :
					Arrays.<ToIntFunction<WorkerStats>>asList(WorkerStats::getStrength,
							WorkerStats::getDexterity, WorkerStats::getConstitution,
							WorkerStats::getIntelligence, WorkerStats::getWisdom,
							WorkerStats::getCharisma)) {
				writeFieldDelimiter(ostream);
				writeField(ostream, WorkerStats.getModifierString(field.applyAsInt(stats)));
			}
		}
		ostream.append(getRowDelimiter());
		return true;
	}
	/**
	 * @return the header row for the tabular report
	 */
	@Override
	public String headerRow() {
		return "Distance,Location,Name,HP,\"Max HP\",Str,Dex,Con,Int,Wis,Cha";
	}

	/**
	 * @param one a Pair of one animal and its location (in the other order)
	 * @param two a Pair of another animal and its location (in the other order)
	 * @return the result of a comparison between the pairs.
	 */
	@Override
	public int comparePairs(final Pair<Point, IWorker> one,
							final Pair<Point, IWorker> two) {
		final DistanceComparator comparator = new DistanceComparator(base);
		final IWorker first = one.second();
		final IWorker second = two.second();
		final int cmp = comparator.compare(one.first(), two.first());
		if (cmp == 0) {
			return first.getName().compareTo(second.getName());
		} else {
			return cmp;
		}
	}
}
