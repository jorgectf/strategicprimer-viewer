package report.generators;

import common.map.fixtures.Implement;
import java.util.function.Consumer;
import org.jetbrains.annotations.Nullable;
import org.javatuples.Pair;
import lovelace.util.DelayedRemovalMap;
import java.util.stream.StreamSupport;
import java.util.stream.Collectors;
import java.util.List;

import common.map.Player;
import common.map.IFixture;
import common.map.Point;
import common.map.MapDimensions;
import common.map.IMapNG;
import common.map.fixtures.mobile.IWorker;
import common.map.fixtures.mobile.worker.WorkerStats;
import common.map.fixtures.mobile.worker.ISkill;
import common.map.fixtures.mobile.worker.IJob;

/**
 * A report generator for Workers.
 */
/* package */ class WorkerReportGenerator extends AbstractReportGenerator<IWorker> {

	private final boolean details;
	private final Player currentPlayer;
	private final AnimalReportGenerator animalReportGenerator;
	private final FortressMemberReportGenerator equipmentReportGenerator;

	public WorkerReportGenerator(final boolean details, final MapDimensions dimensions, final Player currentPlayer,
			final int currentTurn) {
		this(details, dimensions, currentPlayer, currentTurn, null);
	}

	public WorkerReportGenerator(final boolean details, final MapDimensions dimensions, final Player currentPlayer,
			final int currentTurn, final @Nullable Point hq) {
		super(dimensions, hq);
		this.details = details;
		this.currentPlayer = currentPlayer;
		animalReportGenerator = new AnimalReportGenerator(dimensions, currentTurn, hq);
		equipmentReportGenerator = new FortressMemberReportGenerator(currentPlayer, dimensions, currentTurn, hq);
	}

	private static String mod(final int stat) {
		return WorkerStats.getModifierString(stat);
	}

	/**
	 * Produce the sub-sub-report on a worker's stats.
	 * TODO: Take Consumer instead of returning String
	 */
	private static String statsString(final WorkerStats stats) {
		return String.format("He or she has the following stats: %d / %d Hit Points, Strength %s, Dexterity %s, Constitution %s, Intelligence %s, Wisdom %s, Charisma %s",
			stats.getHitPoints(), stats.getMaxHitPoints(), mod(stats.getStrength()),
			mod(stats.getDexterity()), mod(stats.getConstitution()),
			mod(stats.getIntelligence()), mod(stats.getWisdom()),
			mod(stats.getCharisma()));
	}

	// TODO: take Consumer instead of returning String
	private static String skillString(final ISkill skill) {
		return skill.getName() + " " + skill.getLevel();
	}

	/**
	 * Produce text describing the given Skills.
	 * TODO: Take Consumer instead of returning String?
	 */
	private static String skills(final Iterable<ISkill> job) {
		return (job.iterator().hasNext()) ? StreamSupport.stream(job.spliterator(), false)
			.map(WorkerReportGenerator::skillString)
			.collect(Collectors.joining(", ", "(", ")")) : "";
	}

	/**
	 * Produce a sub-sub-report on a worker (we assume we're already in the
	 * middle of a paragraph or bullet point).
	 */
	@Override
	public void produceSingle(final DelayedRemovalMap<Integer, Pair<Point, IFixture>> fixtures,
	                          final IMapNG map, final Consumer<String> ostream, final IWorker worker, final Point loc) {
		if (details && worker.getStats() != null) {
			ostream.accept("""

					%s, a %s.
					<p>%s</p>
					""".formatted(worker.getName(), worker.getRace(), statsString(worker.getStats())));
		} else {
			ostream.accept("""

					%s, a %s.""");
		}
		if (details && worker.iterator().hasNext()) {
			ostream.accept("""

					(S)he has training or experience in the following Jobs (Skills):
					<ul>
					""");
			for (final IJob job : worker) {
				ostream.accept("""
						<li>%d levels in %s %s</li>
						""".formatted(job.getLevel(), job.getName(), skills(job)));
			}
			println(ostream, "</ul>");
		}
		if (details && worker.getMount() != null) {
			ostream.accept("(S)he is mounted on the following animal:");
			animalReportGenerator.produceSingle(fixtures, map, ostream, worker.getMount(), loc);
		}
		if (details && !worker.getEquipment().isEmpty()) {
			ostream.accept("""
					(S)he has the following personal equipment:
					<ul>
					""");
			for (final Implement item : worker.getEquipment()) {
				ostream.accept("<li>");
				equipmentReportGenerator.produceSingle(fixtures, map, ostream, item, loc);
				println(ostream, "</li>");
			}
			println(ostream, "</ul>");
		}
		if (details && !worker.getNote(currentPlayer).isEmpty()) {
			ostream.accept("""
					<p>%s</p>
					""".formatted(worker.getNote(currentPlayer)));
		}
	}

	/**
	 * Produce a sub-sub-report on all workers. This should never be
	 * called, but we'll implement it properly anyway.
	 */
	@Override
	public void produce(final DelayedRemovalMap<Integer, Pair<Point, IFixture>> fixtures,
	                    final IMapNG map, final Consumer<String> ostream) {
		final List<Pair<IWorker, Point>> workers = fixtures.values().stream()
			.filter(p -> p.getValue1() instanceof IWorker)
			.sorted(pairComparator)
			.map(p -> Pair.with((IWorker) p.getValue1(), p.getValue0())).toList();
		if (!workers.isEmpty()) {
			ostream.accept("""
					<h5>Workers</h5>
					<ul>
					""");
			for (final Pair<IWorker, Point> pair : workers) {
				ostream.accept("<li>");
				produceSingle(fixtures, map, ostream, pair.getValue0(), pair.getValue1());
				println(ostream, "</li>");
			}
			println(ostream, "</ul>");
		}
	}
}
