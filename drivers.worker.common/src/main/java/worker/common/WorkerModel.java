package worker.common;

import common.map.HasName;
import common.map.HasOwner;
import java.util.Collection;
import java.util.Random;
import java.util.function.BiPredicate;
import java.util.Collections;
import java.util.Optional;
import common.map.fixtures.FixtureIterable;
import java.util.stream.Stream;
import lovelace.util.LovelaceLogger;
import org.javatuples.Pair;
import org.jetbrains.annotations.Nullable;
import java.util.stream.StreamSupport;
import java.util.stream.Collectors;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;
import java.util.Deque;

import common.map.HasKind;
import common.map.HasMutableKind;
import common.map.HasMutableName;
import common.map.HasMutableOwner;
import common.map.IFixture;
import common.map.IMapNG;
import common.map.IMutableMapNG;
import common.map.Point;
import common.map.Player;

import common.map.fixtures.UnitMember;

import common.map.fixtures.mobile.ProxyFor;
import common.map.fixtures.mobile.IMutableUnit;
import common.map.fixtures.mobile.IMutableWorker;
import common.map.fixtures.mobile.IUnit;
import common.map.fixtures.mobile.IWorker;
import common.map.fixtures.mobile.ProxyUnit;
import common.map.fixtures.towns.IFortress;
import common.map.fixtures.towns.IMutableFortress;
import drivers.common.SimpleMultiMapModel;
import drivers.common.IDriverModel;
import drivers.common.IWorkerModel;

import common.map.fixtures.mobile.worker.IMutableJob;
import common.map.fixtures.mobile.worker.IMutableSkill;
import common.map.fixtures.mobile.worker.ISkill;
import common.map.fixtures.mobile.worker.Job;
import common.map.fixtures.mobile.worker.Skill;

/**
 * A model to underlie the advancement GUI, etc.
 */
public class WorkerModel extends SimpleMultiMapModel implements IWorkerModel {
	/**
	 * If the argument is a {@link IFortress fortress},
	 * return a stream of its members; otherwise, return a stream
	 * containing only the argument. This allows callers to get a flattened
	 * stream of units, including those in fortresses.
	 */
	private static Stream<?> flatten(final Object fixture) {
		if (fixture instanceof IFortress f) {
			return f.stream();
		} else {
			return Stream.of(fixture);
		}
	}

	/**
	 * If the item in the entry is a {@link IFortress fortress}, return a
	 * stream of its contents paired with its location; otherwise, return a
	 * stream of just it.
	 */
	private static Stream<Pair<Point, IFixture>> flattenEntries(final Point point,
	                                                            final IFixture fixture) {
		if (fixture instanceof IFortress f) {
			return f.stream().map(m -> Pair.with(point, m));
		} else {
			return Stream.of(Pair.with(point, fixture));
		}
	}

	/**
	 * Add the given unit at the given location in the given map.
	 */
	private static void addUnitAtLocationImpl(final IUnit unit, final Point location, final IMutableMapNG map) {
		final IMutableFortress fortress = map.getFixtures(location).stream()
			.filter(IMutableFortress.class::isInstance).map(IMutableFortress.class::cast)
			.filter(f -> f.owner().equals(unit.owner())).findAny().orElse(null);
		if (fortress == null) {
			map.addFixture(location, unit.copy(IFixture.CopyBehavior.KEEP));
		} else {
			fortress.addMember(unit.copy(IFixture.CopyBehavior.KEEP));
		}
	}

	/**
	 * The current player, subject to change by user action.
	 */
	private @Nullable Player currentPlayerImpl = null;

	private final List<UnitMember> dismissedMembers = new ArrayList<>();

	public WorkerModel(final IMutableMapNG map) {
		super(map);
	}

	// TODO: Provide copyConstructor() static factory method?
	public WorkerModel(final IDriverModel model) {
		super(model);
	}

	/**
	 * The current player, subject to change by user action.
	 */
	@Override
	public Player getCurrentPlayer() {
		if (currentPlayerImpl == null) {
			for (final IMapNG localMap : getAllMaps()) {
				final Player temp = localMap.getCurrentPlayer();
				if (!getUnits(temp).isEmpty()) {
					currentPlayerImpl = temp;
					return temp;
				}
			}
			currentPlayerImpl = getMap().getCurrentPlayer();
			return currentPlayerImpl;
		} else {
			return currentPlayerImpl;
		}
	}

	/**
	 * Set the current player for the GUI. Note we <em>deliberately</em> do
	 * not pass this change through to the maps; this is a read-only
	 * operation as far as the map <em>files</em> are concerned.
	 */
	@Override
	public void setCurrentPlayer(final Player currentPlayer) {
		currentPlayerImpl = currentPlayer;
	}

	/**
	 * Flatten and filter the stream to include only units, and only those owned by the given player.
	 */
	private static List<IUnit> getUnitsImpl(final Iterable<?> iter, final Player player) {
		return StreamSupport.stream(iter.spliterator(), false).flatMap(WorkerModel::flatten)
			.filter(IUnit.class::isInstance).map(IUnit.class::cast)
			.filter(u -> u.owner().getPlayerId() == player.getPlayerId())
			.collect(Collectors.toList());
	}

	@Override
	public Iterable<IFortress> getFortresses(final Player player) {
		return getMap().streamAllFixtures()
			.filter(IFortress.class::isInstance).map(IFortress.class::cast)
			.filter(f -> f.owner().equals(player))
			.collect(Collectors.toList());
	}

	/**
	 * All the players in all the maps.
	 */
	@Override
	public Iterable<Player> getPlayers() {
		return streamAllMaps()
			.flatMap(m -> StreamSupport.stream(m.getPlayers().spliterator(), false)).distinct()
			.collect(Collectors.toList());
	}

	/**
	 * Get all the given player's units, or only those of a specified kind.
	 */
	@Override
	public Collection<IUnit> getUnits(final Player player, final String kind) {
		return getUnits(player).stream().filter(u -> kind.equals(u.getKind())).collect(Collectors.toList());
	}

	/**
	 * Get all the given player's units, or only those of a specified kind.
	 */
	@Override
	public Collection<IUnit> getUnits(final Player player) {
		if (getSubordinateMaps().iterator().hasNext()) {
			final Iterable<IUnit> temp = streamAllMaps()
					.flatMap((indivMap) -> getUnitsImpl(indivMap.streamAllFixtures()
							.collect(Collectors.toList()), player).stream())
					.collect(Collectors.toList());
			final Map<Integer, ProxyUnit> tempMap = new TreeMap<>();
			for (final IUnit unit : temp) {
				final int key = unit.getId();
				final ProxyUnit proxy;
				if (tempMap.containsKey(key)) {
					proxy = tempMap.get(key);
				} else {
					final ProxyUnit newProxy = new ProxyUnit(key);
					tempMap.put(key, newProxy);
					proxy = newProxy;
				}
				proxy.addProxied(unit);
			}
			return tempMap.values().stream().sorted(Comparator.comparing(IUnit::getName,
					String.CASE_INSENSITIVE_ORDER)).collect(Collectors.toList());
		} else {
			// Just in case I missed something in the proxy implementation, make sure
			// things work correctly when there's only one map.
			return getUnitsImpl(getMap().streamAllFixtures()
					.collect(Collectors.toList()), player)
					.stream().sorted(Comparator.comparing(IUnit::getName,
							String.CASE_INSENSITIVE_ORDER))
					.collect(Collectors.toList());
		}
	}

	/**
	 * All the "kinds" of units the given player has.
	 */
	@Override
	public Iterable<String> getUnitKinds(final Player player) {
		return getUnits(player).stream().map(IUnit::getKind).distinct().sorted(String.CASE_INSENSITIVE_ORDER)
			.collect(Collectors.toList());
	}

	/**
	 * Add the given unit at the given location in all maps.
	 *
	 * FIXME: Should copy into subordinate maps, and return either the unit (in one-map case) or a proxy
	 */
	private void addUnitAtLocation(final IUnit unit, final Point location) {
		if (getSubordinateMaps().iterator().hasNext()) {
			for (final IMutableMapNG eachMap : getRestrictedAllMaps()) {
				addUnitAtLocationImpl(unit, location, eachMap);
				eachMap.setModified(true);
			}
		} else {
			addUnitAtLocationImpl(unit, location, getRestrictedMap());
			setMapModified(true);
		}
	}

	/**
	 * Add a unit to all the maps, at the location of its owner's HQ in the main map.
	 */
	@Override
	public void addUnit(final IUnit unit) {
		Pair<IMutableFortress, Point> temp = null;
		for (final Pair<Point, IMutableFortress> pair : getMap().streamLocations()
				.flatMap(l -> getMap().getFixtures(l).stream()
					.filter(IMutableFortress.class::isInstance)
					.map(IMutableFortress.class::cast)
					.filter(f -> f.owner().getPlayerId() ==
						unit.owner().getPlayerId())
					.map(f -> Pair.with(l, f))).toList()) {
			final Point point = pair.getValue0();
			final IMutableFortress fixture = pair.getValue1();
			if ("HQ".equals(fixture.getName())) {
				addUnitAtLocation(unit, point);
				return;
			} else if (temp == null) {
				temp = Pair.with(fixture, point);
			}
		}
		if (temp != null) {
			final IMutableFortress fortress = temp.getValue0();
			final Point loc = temp.getValue1();
			LovelaceLogger.info("Added unit at fortress %s, not HQ", fortress.getName());
			addUnitAtLocation(unit, loc);
			return;
		} else if (!unit.owner().isIndependent()) {
			LovelaceLogger.warning("No suitable location found for unit %s, owned by %s",
				unit.getName(), unit.owner());
		}
	}

	/**
	 * Get a unit by its owner and ID.
	 */
	@Override
	public @Nullable IUnit getUnitByID(final Player owner, final int id) {
		return getUnits(owner).parallelStream()
			.filter(u -> u.getId() == id).findAny().orElse(null);
	}

	private static BiPredicate<Point, IFixture> unitMatching(final IUnit unit) {
		return (point, fixture) ->
			fixture instanceof IUnit u && fixture.getId() == unit.getId() &&
				u.owner().equals(unit.owner());
	}

	/**
	 * Remove the given unit from the map. It must be empty, and may be
	 * required to be owned by the current player. The operation will also
	 * fail if "matching" units differ in name or kind from the provided
	 * unit.  Returns true if the preconditions were met and the unit was
	 * removed, and false otherwise. To make an edge case explicit, if
	 * there are no matching units in any map the method returns false.
	 */
	@Override
	public boolean removeUnit(final IUnit unit) {
		LovelaceLogger.debug("In WorkerModel.removeUnit()");
		final List<Pair<IMutableMapNG, Pair<Point, IUnit>>> delenda = new ArrayList<>();
		for (final IMutableMapNG map : getRestrictedAllMaps()) {
			final Pair<Point, IFixture> pair = map.streamLocations()
					.flatMap(l -> map.getFixtures(l).stream()
					.map(f -> Pair.with(l, f)))
					.flatMap(p -> flattenEntries(p.getValue0(), p.getValue1()))
					.filter(p -> unitMatching(unit).test(p.getValue0(),
						p.getValue1()))
					.findAny().orElse(null);
			if (pair != null) {
				LovelaceLogger.debug("Map has matching unit");
				final Point location = pair.getValue0();
				final IUnit fixture = (IUnit) pair.getValue1();
				if (fixture.getKind().equals(unit.getKind()) &&
						fixture.getName().equals(unit.getName()) &&
						!fixture.iterator().hasNext()) {
					LovelaceLogger.debug("Matching unit meets preconditions");
					delenda.add(Pair.with(map, Pair.with(location, fixture)));
				} else {
					LovelaceLogger.warning(
						"Matching unit in %s fails preconditions for removal",
						Optional.ofNullable(map.getFilename())
							.map(Object::toString).orElse("an unsaved map"));
					return false;
				}
			}
		}
		if (delenda.isEmpty()) {
			LovelaceLogger.debug("No matching units");
			return false;
		}
		for (final Pair<IMutableMapNG, Pair<Point, IUnit>> pair : delenda) {
			final IMutableMapNG map = pair.getValue0();
			final Point location = pair.getValue1().getValue0();
			final IUnit fixture = pair.getValue1().getValue1();
			if (map.getFixtures(location).contains(fixture)) {
				map.removeFixture(location, fixture);
			} else {
				boolean any = false;
				for (final IMutableFortress fort : map.getFixtures(location).stream()
						.filter(IMutableFortress.class::isInstance)
						.map(IMutableFortress.class::cast).toList()) {
					if (fort.stream().anyMatch(fixture::equals)) {
						any = true;
						fort.removeMember(fixture);
						break;
					}
				}
				if (!any) {
					LovelaceLogger.warning(
						"Failed to find unit to remove that we thought might be in a fortress");
				}
			}
		}
		LovelaceLogger.debug("Finished removing matching unit(s) from map(s)");
		return true;
	}

	private static int iterableSize(final Iterable<?> iter) {
		return (int) StreamSupport.stream(iter.spliterator(), true).count();
	}

	/**
	 * Move a unit-member from one unit to another in the presence of
	 * proxies, that is, when each unit and unit-member represents
	 * corresponding units and unit members in multiple maps and the same
	 * operations must be applied to all of them.
	 *
	 * The proxy code is some of the most difficult and delicate code in
	 * the entire suite, and I'm <em>pretty</em> sure the algorithm this
	 * method implements is correct ...
	 *
	 * Returns true if our preconditions were met and so we did the move,
	 * and false when preconditions were not met and the caller should fall
	 * back to the non-proxy algorithm.
	 *
	 * TODO: Add a test of this method.
	 */
	private boolean moveProxied(/*UnitMember&*/final ProxyFor<? extends UnitMember> member, final ProxyUnit old,
	                                           final ProxyUnit newOwner) {
		if (old.getProxied().size() == newOwner.getProxied().size() &&
				old.getProxied().size() == member.getProxied().size()) {
			final LinkedList<UnitMember> memberProxied = new LinkedList<>(member.getProxied());
			final LinkedList<IUnit> oldProxied = new LinkedList<>(old.getProxied());
			final LinkedList<IUnit> newProxied = new LinkedList<>(newOwner.getProxied());
			final Deque<UnitMember> members = new LinkedList<>();
			final Deque<IMutableUnit> newList = new LinkedList<>();
			while (!memberProxied.isEmpty() && !oldProxied.isEmpty() && !newProxied.isEmpty()) {
				final UnitMember item = memberProxied.removeFirst();
				final IUnit innerOld = oldProxied.removeFirst();
				final IUnit innerNew = newProxied.removeFirst();
				if (innerOld instanceof IMutableUnit oldUnit && innerNew instanceof IMutableUnit newUnit) {
					oldUnit.removeMember(item);
					members.addLast(item);
					newList.addLast(newUnit);
				} else {
					LovelaceLogger.warning("Immutable unit in moveProxied()");
					return false;
				}
			}
			while (!newList.isEmpty() && !members.isEmpty()) {
				final IMutableUnit unit = newList.removeFirst();
				final UnitMember innerMember = members.removeFirst();
				unit.addMember(innerMember);
			}
			for (final IMutableMapNG map : getRestrictedAllMaps()) {
				map.setModified(true);
			}
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Move a unit-member from one unit to another. If all three objects
	 * are proxies, we use a special algorithm that unwraps the proxies,
	 * which was extracted as {@link #moveProxied}.
	 */
	@Override
	public void moveMember(final UnitMember member, final IUnit old, final IUnit newOwner) {
		if (member instanceof ProxyFor proxyMember && old instanceof ProxyUnit proxyOld &&
				newOwner instanceof ProxyUnit proxyNew && moveProxied(proxyMember, proxyOld, proxyNew)) {
			return;
		}
		for (final IMutableMapNG map : getRestrictedAllMaps()) {
			final IMutableUnit matchingOld = getUnitsImpl(map.streamAllFixtures()
						.collect(Collectors.toList()), old.owner()).stream()
				.filter(IMutableUnit.class::isInstance).map(IMutableUnit.class::cast)
				.filter(u -> u.getKind().equals(old.getKind()))
				.filter(u -> u.getName().equals(old.getName()))
				.filter(u -> u.getId() == old.getId())
				.findAny().orElse(null);
			if (matchingOld != null) {
				final UnitMember matchingMember = matchingOld.stream().filter(member::equals) // TODO: equals() isn't ideal for finding a matching member ...
					.findAny().orElse(null);
				final IMutableUnit matchingNew = getUnitsImpl(map.streamAllFixtures()
							.collect(Collectors.toList()), newOwner.owner())
						.stream()
						.filter(IMutableUnit.class::isInstance)
						.map(IMutableUnit.class::cast)
						.filter(u -> u.getKind().equals(newOwner.getKind()))
						.filter(u -> u.getName().equals(newOwner.getName()))
						.filter(u -> u.getId() == newOwner.getId())
						.findAny().orElse(null);
				if (matchingMember != null && matchingNew != null) {
					matchingOld.removeMember(matchingMember);
					matchingNew.addMember(matchingMember);
					map.setModified(true);
				}
			}
		}
	}

	@Override
	public void dismissUnitMember(final UnitMember member) {
		boolean any = false;
		// TODO: Handle proxies specially?
		for (final IMutableMapNG map : getRestrictedAllMaps()) {
			for (final IMutableUnit unit : getUnitsImpl(map.streamAllFixtures()
							.collect(Collectors.toList()),
						getCurrentPlayer()).stream()
					.filter(IMutableUnit.class::isInstance)
					.map(IMutableUnit.class::cast).toList()) {
				final UnitMember matching = unit.stream().filter(member::equals) // FIXME: equals() will really not do here ...
					.findAny().orElse(null);
				if (matching != null) {
					any = true;
					unit.removeMember(matching);
					map.setModified(true);
					break;
				}
			}
		}
		if (any) {
			dismissedMembers.add(member);
		}
	}

	@Override
	public Iterable<UnitMember> getDismissed() {
		return Collections.unmodifiableList(dismissedMembers);
	}

	// TODO: Notification events should come from the map, instead of here
	// (as we might add one to this method), so UI could just call this and
	// the tree model could listen to the map---so the worker-mgmt UI would
	// update if a unit were added through the map-viewer UI.
	@Override
	public void addUnitMember(final IUnit unit, final UnitMember member) {
		for (final IMutableMapNG map : getRestrictedAllMaps()) {
			final IMutableUnit matching = getUnitsImpl(map.streamAllFixtures()
						.collect(Collectors.toList()), unit.owner()).stream()
				.filter(IMutableUnit.class::isInstance)
				.map(IMutableUnit.class::cast)
				.filter(u -> u.getName().equals(unit.getName()))
				.filter(u -> u.getKind().equals(unit.getKind()))
				.filter(u -> u.getId() == unit.getId())
				.findAny().orElse(null);
			if (matching != null) {
				matching.addMember(member.copy(IFixture.CopyBehavior.KEEP));
				map.setModified(true);
				continue;
			}
		}
	}

	@Override
	public boolean renameItem(final HasName item, final String newName) {
		boolean any = false;
		if (item instanceof IUnit unit) {
			for (final IMutableMapNG map : getRestrictedAllMaps()) {
				final IUnit matching =
					getUnitsImpl(map.streamAllFixtures()
							.collect(Collectors.toList()),
						unit.owner()).stream()
					.filter(u -> u.getName().equals(item.getName()))
					.filter(u -> u.getKind().equals(unit.getKind()))
					.filter(u -> u.getId() == unit.getId())
					.findAny().orElse(null);
				if (matching instanceof HasMutableName matchNamed) {
					any = true;
					matchNamed.setName(newName);
					map.setModified(true);
				}
			}
			if (!any) {
				LovelaceLogger.warning("Unable to find unit to rename");
			}
			return any;
		} else if (item instanceof UnitMember memberItem) {
			for (final IMutableMapNG map : getRestrictedAllMaps()) {
				final UnitMember matching =
					getUnitsImpl(map.streamAllFixtures()
							.collect(Collectors.toList()), getCurrentPlayer())
						.stream().flatMap(FixtureIterable::stream)
						.filter(HasMutableName.class::isInstance)
						.filter(m -> m.getId() == memberItem.getId())
						.filter(m -> ((HasMutableName) m).getName()
							.equals(item.getName()))
						.findAny().orElse(null); // FIXME: We should have a firmer identification than just name and ID
				if (matching != null) {
					any = true;
					((HasMutableName) matching).setName(newName);
					map.setModified(true);
				}
			}
			if (!any) {
				LovelaceLogger.warning("Unable to find unit member to rename");
			}
			return any;
		} else {
			LovelaceLogger.warning("Unable to find item to rename");
			return false;
		}
	}

	@Override
	public boolean changeKind(final HasKind item, final String newKind) {
		boolean any = false;
		if (item instanceof IUnit unit) {
			for (final IMutableMapNG map : getRestrictedAllMaps()) {
				final IUnit matching = getUnitsImpl(map.streamAllFixtures()
							.collect(Collectors.toList()),
						unit.owner()).stream()
					.filter(u -> u.getName().equals(unit.getName()))
					.filter(u -> u.getKind().equals(item.getKind()))
					.filter(u -> u.getId() == unit.getId())
					.findAny().orElse(null);
				if (matching instanceof HasMutableKind kinded) {
					any = true;
					kinded.setKind(newKind);
					map.setModified(true);
				}
			}
			if (!any) {
				LovelaceLogger.warning("Unable to find unit to change kind");
			}
			return any;
		} else if (item instanceof UnitMember member) {
			for (final IMutableMapNG map : getRestrictedAllMaps()) {
				final HasMutableKind matching = getUnitsImpl(map.streamAllFixtures()
							.collect(Collectors.toList()), getCurrentPlayer())
					.stream().flatMap(FixtureIterable::stream)
					.filter(m -> m.getId() == member.getId())
					.filter(HasMutableKind.class::isInstance)
					.map(HasMutableKind.class::cast)
					.filter(m -> m.getKind().equals(item.getKind()))
					.findAny().orElse(null); // FIXME: We should have a firmer identification than just kind and ID
				if (matching != null) {
					any = true;
					matching.setKind(newKind);
					map.setModified(true);
				}
			}
			if (!any) {
				LovelaceLogger.warning("Unable to find unit member to change kind");
			}
			return any;
		} else {
			LovelaceLogger.warning("Unable to find item to change kind");
			return false;
		}
	}

	@Override
	public boolean addSibling(final UnitMember existing, final UnitMember sibling) {
		boolean any = false;
		for (final IMutableMapNG map : getRestrictedAllMaps()) {
			for (final IMutableUnit unit : getUnitsImpl(map.streamAllFixtures()
						.collect(Collectors.toList()), getCurrentPlayer())
					.stream().filter(IMutableUnit.class::isInstance)
					.map(IMutableUnit.class::cast).toList()) {
				if (unit.stream().anyMatch(existing::equals)) {
					// TODO: look beyond equals() for matching-in-existing?
					unit.addMember(sibling.copy(IFixture.CopyBehavior.KEEP));
					any = true;
					map.setModified(true);
					break;
				}
			}
		}
		return any;
	}

	private static Stream<IFixture> flattenIncluding(final IFixture fixture) {
		if (fixture instanceof FixtureIterable) {
			return Stream.concat(Stream.of(fixture), ((FixtureIterable<?>) fixture).stream());
		} else {
			return Stream.of(fixture);
		}
	}

	/**
	 * Change the owner of the given item in all maps. Returns true if this
	 * succeeded in any map, false otherwise.
	 */
	@Override
	public boolean changeOwner(final HasOwner item, final Player newOwner) {
		boolean any = false;
		for (final IMutableMapNG map : getRestrictedAllMaps()) {
			final HasMutableOwner matching = map.streamAllFixtures()
				.flatMap(WorkerModel::flattenIncluding)
				.flatMap(WorkerModel::flattenIncluding).filter(HasMutableOwner.class::isInstance)
				.map(HasMutableOwner.class::cast)
				.filter(item::equals) // TODO: equals() is not the best way to find it ...
				.findAny().orElse(null);
			if (matching != null) {
				if (StreamSupport.stream(map.getPlayers().spliterator(), true)
						.noneMatch(newOwner::equals)) {
					map.addPlayer(newOwner);
				}
				matching.setOwner(map.getPlayers().getPlayer(newOwner.getPlayerId()));
				map.setModified(true);
				any = true;
			}
		}
		return any;
	}

	@Override
	public boolean sortFixtureContents(final IUnit fixture) {
		boolean any = false;
		for (final IMutableMapNG map : getRestrictedAllMaps()) {
			final IMutableUnit matching = getUnitsImpl(map.streamAllFixtures()
					.collect(Collectors.toList()), getCurrentPlayer()).stream()
				.filter(IMutableUnit.class::isInstance).map(IMutableUnit.class::cast)
				.filter(u -> u.getName().equals(fixture.getName()))
				.filter(u -> u.getKind().equals(fixture.getKind()))
				.filter(u -> u.getId() == fixture.getId())
				.findAny().orElse(null);
			if (matching != null) {
				matching.sortMembers();
				map.setModified(true);
				any = true;
			}
		}
		return any;
	}

	/**
	 * Add a Job to the matching worker in all maps. Returns true if a
	 * matching worker was found in at least one map, false otherwise.
	 * If an existing Job by that name already existed, it is left alone.
	 */
	@Override
	public boolean addJobToWorker(final IWorker worker, final String jobName) {
		boolean any = false;
		for (final IMutableMapNG map : getRestrictedAllMaps()) {
			final IMutableWorker matching = getUnitsImpl(map.streamAllFixtures()
					.collect(Collectors.toList()), getCurrentPlayer()).stream()
				.flatMap(FixtureIterable::stream).filter(IMutableWorker.class::isInstance)
				.map(IMutableWorker.class::cast)
				.filter(w -> w.getRace().equals(worker.getRace()))
				.filter(w -> w.getName().equals(worker.getName()))
				.filter(w -> w.getId() == worker.getId())
				.findAny().orElse(null);
			if (matching != null) {
				if (StreamSupport.stream(matching.spliterator(), true)
						.noneMatch(j -> jobName.equals(j.getName()))) {
					map.setModified(true);
					matching.addJob(new Job(jobName, 0));
				}
				any = true;
			}
		}
		return any;
	}

	/**
	 * Add a skill, without any hours in it, to the specified worker in the
	 * specified Job in all maps. Returns true if a matching worker was
	 * found in at least one map, false otherwise. If no existing Job by
	 * that name already exists, a zero-level Job with that name is added
	 * first. If a Skill by that name already exists in the corresponding
	 * Job, it is left alone.
	 */
	@Override
	public boolean addSkillToWorker(final IWorker worker, final String jobName, final String skillName) {
		boolean any = false;
		for (final IMutableMapNG map : getRestrictedAllMaps()) {
			final IMutableWorker matching =
					getUnitsImpl(map.streamAllFixtures().collect(Collectors.toList()), getCurrentPlayer()).stream()
							.flatMap(IUnit::stream).filter(IMutableWorker.class::isInstance).map(IMutableWorker.class::cast)
							.filter(w -> w.getRace().equals(worker.getRace())).filter(w -> w.getName().equals(worker.getName()))
							.filter(w -> w.getId() == worker.getId()).findAny().orElse(null);
			if (matching != null) {
				final IMutableJob job = StreamSupport.stream(matching.spliterator(), false)
						.filter(IMutableJob.class::isInstance).map(IMutableJob.class::cast)
						.filter(j -> j.getName().equals(jobName)).findAny().orElse(null);
				if (job == null) {
					map.setModified(true);
					final Job newJob = new Job(jobName, 0);
					newJob.addSkill(new Skill(skillName, 0, 0));
					matching.addJob(newJob);
				} else if (StreamSupport.stream(job.spliterator(), false).map(ISkill::getName).noneMatch(skillName::equals)) {
					map.setModified(true);
					job.addSkill(new Skill(skillName, 0, 0));
				}
				any = true;
			}
		}
		return any;
	}

	/**
	 * Add a skill, without any hours in it, to all workers in the
	 * specified Job in all maps. Returns true if at least one matching
	 * worker was found in at least one map, false otherwise. If a worker
	 * is in a different unit in some map, the Skill is still added to it.
	 * If no existing Job by that name already exists, a zero-level Job
	 * with that name is added first. If a Skill by that name already
	 * exists in the corresponding Job, it is left alone.
	 */
	@Override
	public boolean addSkillToAllWorkers(final IUnit unit, final String jobName, final String skillName) {
		boolean any = false;
		for (final IWorker worker : unit.stream().filter(IWorker.class::isInstance).map(IWorker.class::cast).toList()) {
			if (addSkillToWorker(worker, jobName, skillName)) {
				any = true;
			}
		}
		return any;
	}

	/**
	 * Add hours to a Skill to the specified Job in the matching worker in
	 * all maps.  Returns true if a matching worker was found in at least
	 * one map, false otherwise. If the worker doesn't have that Skill in
	 * that Job, it is added first; if the worker doesn't have that Job, it
	 * is added first as in {@link #addJobToWorker}, then the skill is added
	 * to it. The "contextValue" is passed to {@link
	 * common.map.fixtures.mobile.worker.IMutableSkill#addHours}; it should
	 * be a random number between 0 and 99.
	 */
	@Override
	public boolean addHoursToSkill(final IWorker worker, final String jobName, final String skillName, final int hours,
	                               final int contextValue) {
		boolean any = false;
		for (final IMutableMapNG map : getRestrictedAllMaps()) {
			final IMutableWorker matching = getUnitsImpl(map.streamAllFixtures()
					.collect(Collectors.toList()), getCurrentPlayer()).stream()
				.flatMap(FixtureIterable::stream).filter(IMutableWorker.class::isInstance)
				.map(IMutableWorker.class::cast)
				.filter(w -> w.getRace().equals(worker.getRace()))
				.filter(w -> w.getName().equals(worker.getName()))
				.filter(w -> w.getId() == worker.getId())
				.findAny().orElse(null);
			if (matching != null) {
				map.setModified(true);
				any = true;
				final IMutableJob job;
				final IMutableJob temp = StreamSupport.stream(matching.spliterator(), true)
					.filter(IMutableJob.class::isInstance).map(IMutableJob.class::cast)
					.filter(j -> jobName.equals(j.getName())).findAny().orElse(null);
				if (temp == null) {
					job = new Job(jobName, 0);
					matching.addJob(job); // FIXME: The IWorker API doc explicitly says the Job object can't be assumed to have been preserved
				} else {
					job = temp;
				}
				final IMutableSkill skill;
				final IMutableSkill tempSkill = StreamSupport.stream(job.spliterator(), true)
					.filter(IMutableSkill.class::isInstance)
					.map(IMutableSkill.class::cast)
					.filter(s -> skillName.equals(s.getName()))
					.findAny().orElse(null);
				if (tempSkill == null) {
					skill = new Skill(skillName, 0, 0);
					job.addSkill(skill); // FIXME: Similarly, assumes behavior the API doc explicitly warns against
				} else {
					skill = tempSkill;
				}
				skill.addHours(hours, contextValue);
			}
		}
		return any;
	}

	/**
	 * Add hours to a Skill to the specified Job in all workers in the
	 * given unit in all maps. (If a worker is in a different unit in some
	 * maps, that worker will still receive the hours.) Returns true if at
	 * least one worker received hours, false otherwise. If a worker
	 * doesn't have that skill in that Job, it is added first; if it
	 * doesn't have that Job, it is added first as in {@link
	 * #addJobToWorker}, then the skill is added to it. The
	 * "contextValue" is used to calculate a new value passed to {@link
	 * common.map.fixtures.mobile.worker.IMutableSkill#addHours} for each
	 * worker.
	 *
	 * TODO: Take a level-up listener?
	 */
	@Override
	public boolean addHoursToSkillInAll(final IUnit unit, final String jobName, final String skillName,
			final int hours, final int contextValue) {
		boolean any = false;
		final Random rng = new Random(contextValue);
		for (final UnitMember member : unit) {
			if (member instanceof IWorker w && addHoursToSkill(w, jobName, skillName, hours,
					rng.nextInt(100))) {
				any = true;
			}
		}
		return any;
	}

	/**
	 * Replace one skill, "delenda", with
	 * another, "replacement", in the specified job in the specified worker in all maps.
	 * Unlike {@link #addHoursToSkill}, if a map does not have an
	 * <em>equal</em> Job in the matching worker, that map is completely
	 * skipped.  If the replacement is already present, just remove the
	 * first skill. Returns true if the operation was carried out in any of
	 * the maps, false otherwise.
	 */
	@Override
	public boolean replaceSkillInJob(final IWorker worker, final String jobName, final ISkill delenda,
	                                 final ISkill replacement) {
		boolean any = false;
		for (final IMutableMapNG map : getRestrictedAllMaps()) {
			final IMutableWorker matchingWorker = getUnitsImpl(map.streamAllFixtures()
					.collect(Collectors.toList()), getCurrentPlayer()).stream()
				.flatMap(FixtureIterable::stream)
				.filter(IMutableWorker.class::isInstance)
				.map(IMutableWorker.class::cast)
				.filter(w -> w.getRace().equals(worker.getRace()))
				.filter(w -> w.getName().equals(worker.getName()))
				.filter(w -> w.getId() == worker.getId())
				.findAny().orElse(null);
			if (matchingWorker != null) {
				final IMutableJob matchingJob = StreamSupport.stream(
						matchingWorker.spliterator(), true)
					.filter(IMutableJob.class::isInstance).map(IMutableJob.class::cast)
					.filter(j -> jobName.equals(j.getName())).findAny().orElse(null);
				if (matchingJob == null) {
					LovelaceLogger.warning("No matching skill in matching worker");
				} else {
					final ISkill matchingSkill = StreamSupport.stream(
									matchingJob.spliterator(), true)
							.filter(delenda::equals).findAny().orElse(null);
					if (matchingSkill == null) {
						LovelaceLogger.warning("No matching skill in matching worker");
					} else {
						map.setModified(true);
						any = true;
						matchingJob.removeSkill(matchingSkill);
						matchingJob.addSkill(replacement.copy());
					}
				}
			}
		}
		return any;
	}

	/**
	 * Set the given unit's orders for the given turn to the given text.
	 * Returns true if a matching (and mutable) unit was found in at
	 * least one map, false otherwise.
	 */
	@Override
	public boolean setUnitOrders(final IUnit unit, final int turn, final String results) {
		boolean any = false;
		for (final IMutableMapNG map : getRestrictedAllMaps()) {
			// TODO: Why not use getUnitsImpl?
			final IMutableUnit matching = map.streamAllFixtures()
				.flatMap(WorkerModel::flatten)
				.filter(IMutableUnit.class::isInstance).map(IMutableUnit.class::cast)
				.filter(u -> u.owner().equals(unit.owner()))
				.filter(u -> u.getKind().equals(unit.getKind()))
				.filter(u -> u.getName().equals(unit.getName()))
				.filter(u -> u.getId() == unit.getId())
				.findAny().orElse(null);
			if (matching != null) {
				matching.setOrders(turn, results);
				map.setModified(true);
				any = true;
			}
		}
		return any;
	}

	/**
	 * Set the given unit's results for the given turn to the given text.
	 * Returns true if a matching (and mutable) unit was found in at least
	 * one map, false otherwise.
	 */
	@Override
	public boolean setUnitResults(final IUnit unit, final int turn, final String results) {
		boolean any = false;
		for (final IMutableMapNG map : getRestrictedAllMaps()) {
			// TODO: Why not use getUnitsImpl?
			final IMutableUnit matching = map.streamAllFixtures()
				.flatMap(WorkerModel::flatten)
				.filter(IMutableUnit.class::isInstance).map(IMutableUnit.class::cast)
				.filter(u -> u.owner().equals(unit.owner()))
				.filter(u -> u.getKind().equals(unit.getKind()))
				.filter(u -> u.getName().equals(unit.getName()))
				.filter(u -> u.getId() == unit.getId())
				.findAny().orElse(null);
			if (matching != null) {
				matching.setResults(turn, results);
				map.setModified(true);
				any = true;
			}
		}
		return any;
	}
}

