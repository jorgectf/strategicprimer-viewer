package common.map.fixtures.mobile.worker;

import common.map.fixtures.Implement;
import common.map.fixtures.mobile.Animal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import java.util.function.Consumer;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import common.map.Player;
import common.map.IFixture;
import common.map.fixtures.UnitMember;
import common.map.fixtures.mobile.IUnit;
import common.map.fixtures.mobile.IWorker;
import common.map.fixtures.mobile.WorkerProxy;

import org.jetbrains.annotations.Nullable;

/**
 * An IWorker implementation to make the UI able to operate on all of a unit's workers at once.
 *
 * @deprecated We're trying to get rid of the notion of 'proxies' in favor of
 * driver model methods.
 */
@Deprecated
public class ProxyWorker implements WorkerProxy {
	/**
	 * If false, this is representing all the workers in a single unit; if
	 * true, it is representing the corresponding workers in corresponding
	 * units in different maps.
	 */
	private final boolean parallel;

	/**
	 * If false, this is representing all the workers in a single unit; if
	 * true, it is representing the corresponding workers in corresponding
	 * units in different maps.
	 */
	public boolean isParallel() {
		return parallel;
	}

	/**
	 * The proxy Jobs.
	 */
	// TODO: Extract "IJob&ProxyFor<IJob>" interface?
	private final List<ProxyJob> proxyJobs = new ArrayList<>();

	/**
	 * The jobs we're proxying for.
	 */
	private final Set<String> jobNames = new HashSet<>();

	/**
	 * The workers being proxied.
	 */
	private final List<IWorker> workers = new ArrayList<>();

	/**
	 * Equipment held by all the workers. (Cache, more or less.)
	 *
	 * TODO: Should this be the union instead?
	 */
	private final List<Implement> equipmentImpl = new ArrayList<>();

	/**
	 * Cached stats for the workers. Null if there are no workers being
	 * proxied or if they do not share identical stats.
	 */
	private @Nullable WorkerStats statsCache;

	private ProxyWorker(final boolean parallelProxy) {
		parallel = parallelProxy;
		statsCache = null;
	}

	public ProxyWorker(final IUnit unit) {
		this(false);
		for (final UnitMember member : unit) {
			if (member instanceof IWorker w) {
				final WorkerStats tempStats = w.getStats();
				final WorkerStats priorStats = statsCache;
				if (workers.isEmpty()) {
					statsCache = tempStats;
				} else if (!Objects.equals(tempStats, priorStats)) {
					statsCache = null;
				}
				workers.add(w);
				StreamSupport.stream(w.spliterator(), true)
					.map(IJob::getName).forEach(jobNames::add);
			}
		}
		for (final String job : jobNames) {
			final IWorker[] array = new IWorker[workers.size()];
			proxyJobs.add(new ProxyJob(job, false, workers.toArray(array)));
		}
		for (final Implement item : workers.stream().flatMap(w -> w.getEquipment().stream()).toList()) {
			if (workers.stream().map(IWorker::getEquipment).allMatch(l -> l.contains(item))) {
				equipmentImpl.add(item);
			}
		}
	}

	public ProxyWorker(final IWorker... proxiedWorkers) {
		this(true);
		for (final IWorker worker : proxiedWorkers) {
				final WorkerStats tempStats = (worker).getStats();
				final WorkerStats priorStats = statsCache;
				if (workers.isEmpty()) {
					statsCache = tempStats;
					equipmentImpl.addAll(worker.getEquipment());
				} else if (!Objects.equals(tempStats, priorStats)) {
					statsCache = null;
					equipmentImpl.retainAll(worker.getEquipment());
				}
				workers.add(worker);
				StreamSupport.stream(worker.spliterator(), true)
					.map(IJob::getName).forEach(jobNames::add);
		}
		for (final String job : jobNames) {
			proxyJobs.add(new ProxyJob(job, false, proxiedWorkers));
		}
	}

	@Override
	public IWorker copy(final CopyBehavior zero) {
		final ProxyWorker retval = new ProxyWorker(parallel);
		for (final IWorker worker : workers) {
			retval.addProxied(worker.copy(zero));
		}
		return retval;
	}

	@Override
	public int getId() {
		if (parallel) {
			final Integer retval = getConsensus(IWorker::getId);
			if (retval == null) {
				return -1;
			} else {
				return retval;
			}
		} else {
			return -1;
		}
	}

	@Override
	public boolean equalsIgnoringID(final IFixture fixture) {
		if (fixture instanceof ProxyWorker pw) {
			return parallel == pw.parallel && proxyJobs.equals(pw.proxyJobs);
		} else {
			return false;
		}
	}

	@Override
	public Iterator<IJob> iterator() {
		return (Iterator<IJob>)(Iterator<? extends IJob>)proxyJobs.iterator();
	}

	@Override
	public boolean isSubset(final IFixture obj, final Consumer<String> report) {
		report.accept("isSubset called on ProxyWorker");
		return false;
	}

	@Override
	public void addProxied(final IWorker item) {
		if (item == this) {
			return;
		}
		final WorkerStats tempStats = item.getStats();
		final WorkerStats priorStats = statsCache;
		// FIXME: The algorithm here doesn't quite match that of the constructors
		if (workers.isEmpty()) {
			statsCache = tempStats;
			equipmentImpl.addAll(item.getEquipment());
		} else if (tempStats != null) {
			if (priorStats != null && !Objects.equals(tempStats, priorStats)) {
				statsCache = null;
			}
			equipmentImpl.retainAll(item.getEquipment());
		} else if (priorStats != null) {
			statsCache = null;
			equipmentImpl.retainAll(item.getEquipment());
		}
		workers.add(item);
		for (final IJob job : item) {
			final String name = job.getName();
			if (jobNames.contains(name)) {
				for (final ProxyJob proxyJob : proxyJobs) {
					if (proxyJob.getName().equals(name)) {
						proxyJob.addProxied(job);
					}
				}
			} else {
				jobNames.add(name);
				final IWorker[] array = new IWorker[workers.size()];
				proxyJobs.add(new ProxyJob(name, parallel, workers.toArray(array)));
			}
		}
	}

	@Override
	public Collection<IWorker> getProxied() {
		return new ArrayList<>(workers);
	}

	@Override
	public String getDefaultImage() {
		String retval = null;
		for (final IWorker worker : workers) {
			if (retval == null) {
				retval = worker.getDefaultImage();
			} else if (!retval.equals(worker.getDefaultImage())) {
				return "worker.png";
			}
		}
		return (retval == null) ? "worker.png" : retval;
	}

	@Override
	public String getImage() {
		final String retval = getConsensus(IWorker::getImage);
		if (retval == null) {
			return "";
		} else {
			return retval;
		}
	}

	@Override
	public String getRace() {
		final String retval = getConsensus(IWorker::getRace);
		return (retval == null) ? "proxied" : retval;
	}

	@Override
	public String getName() {
		final String retval = getConsensus(IWorker::getName);
		return (retval == null) ? "proxied" : retval;
	}

	@Override
	public String getPortrait() {
		final String retval = getConsensus(IWorker::getPortrait);
		return (retval == null) ? "" : retval;
	}

	@Override
	public IJob getJob(final String jobName) {
		final Optional<ProxyJob> temp =
			proxyJobs.stream().filter(j -> jobName.equals(j.getName())).findAny();
		if (temp.isPresent()) {
			return temp.get();
		}
		final IWorker[] array = new IWorker[workers.size()];
		final ProxyJob retval = new ProxyJob(jobName, parallel, workers.toArray(array));
		jobNames.add(jobName);
		proxyJobs.add(retval);
		return retval;
	}

	@Override
	public @Nullable WorkerStats getStats() {
		return statsCache;
	}

	@Override
	public String getNote(final Player player) {
		final String retval = getConsensus(worker -> worker.getNote(player));
		return (retval == null) ? "" : retval;
	}

	@Override
	public String getNote(final int player) {
		final String retval = getConsensus(worker -> worker.getNote(player));
		return (retval == null) ? "" : retval;
	}

	@Override
	public void setNote(final Player key, final String item) {
		for (final IWorker proxy : workers) {
			proxy.setNote(key, item);
		}
	}

	@Override
	public Collection<Integer> getNotesPlayers() {
		return workers.stream().flatMap(w -> StreamSupport.stream(w.getNotesPlayers().spliterator(),
			true)).collect(Collectors.toSet());
	}

	@Override
	public Collection<Implement> getEquipment() {
		return Collections.unmodifiableList(equipmentImpl);
	}

	@Override
	public @Nullable Animal getMount() {
		return getConsensus(IWorker::getMount);
	}
}
