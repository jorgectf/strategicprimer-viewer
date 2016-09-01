package model.workermgmt;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import model.map.IMutableMapNG;
import model.map.Player;
import model.map.Point;
import model.map.fixtures.mobile.IUnit;
import model.map.fixtures.mobile.ProxyFor;
import model.map.fixtures.mobile.ProxyUnit;
import model.map.fixtures.mobile.Unit;
import model.map.fixtures.towns.Fortress;
import model.misc.IDriverModel;
import model.misc.SimpleMultiMapModel;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import util.NullCleaner;
import util.Pair;
import view.util.SystemOut;

/**
 * A model to underlie the advancement GUI, etc.
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
public final class WorkerModel extends SimpleMultiMapModel implements IWorkerModel {
	/**
	 * Constructor.
	 *
	 * @param map  the map we're wrapping.
	 * @param file the file the map was loaded from or should be saved to
	 */
	public WorkerModel(final IMutableMapNG map, final Optional<Path> file) {
		super(map, file);
	}

	/**
	 * Copy constructor.
	 *
	 * @param model a driver model
	 */
	public WorkerModel(final IDriverModel model) {
		super(model);
	}

	/**
	 * @return a list of all the players in all the maps
	 */
	@Override
	public List<Player> getPlayers() {
		return new ArrayList<>(StreamSupport.stream(getAllMaps().spliterator(), false).flatMap(
				pair -> StreamSupport.stream(pair.first().players().spliterator(),
						false)).collect(Collectors.toSet()));
	}

	/**
	 * @param player a player in the map
	 * @return a list of that player's units
	 */
	@Override
	public List<IUnit> getUnits(final Player player) {
		if (getSubordinateMaps().iterator().hasNext()) {
			return new ArrayList<>(StreamSupport.stream(getAllMaps().spliterator(),
					false).map(Pair::first).flatMap(
							map -> map.locationStream().flatMap(
									point -> getUnits(map.streamOtherFixtures(point),
											player))).collect(
							(Supplier<TreeMap<Integer, IUnit>>) TreeMap::new,
							(retval, unit) -> {
								final IUnit proxy;
								if (retval.containsKey(Integer.valueOf(unit.getID()))) {
									proxy = retval.get(Integer.valueOf(unit.getID()));
									//noinspection unchecked
									((ProxyFor<IUnit>) proxy).addProxied(unit);
								} else {
									proxy = new ProxyUnit(unit.getID());
									//noinspection unchecked
									((ProxyFor<IUnit>) proxy).addProxied(unit);
									retval.put(NullCleaner.assertNotNull(
											Integer.valueOf(unit.getID())), proxy);
								}
							}, Map::putAll).values());
		} else {
			// Just in case I missed something in the proxy implementation, make
			// sure things work correctly when there's only one map.
			return getUnits(
					NullCleaner.assertNotNull(getMap().locationStream()
							.flatMap(point -> getMap()
									.streamOtherFixtures(point))),
					player).collect(Collectors.toList());
		}
	}

	/**
	 * @param iter   a sequence of members of that type
	 * @param player a player
	 * @return a list of the members of the sequence that are units owned by the player
	 */
	private static Stream<IUnit> getUnits(final Stream<@NonNull ? super Unit> iter,
										final Player player) {
		return NullCleaner.assertNotNull(iter.flatMap(item -> {
			if (item instanceof Fortress) {
				return StreamSupport.stream(((Fortress) item).spliterator(), false);
			} else {
				return Stream.of(item);
			}
		}).filter(IUnit.class::isInstance).map(IUnit.class::cast)
												.filter(unit -> unit.getOwner()
																		.equals(player)));
	}

	/**
	 * @param player a player in the map
	 * @return the "kinds" of unit that player has.
	 */
	@Override
	public List<String> getUnitKinds(final Player player) {
		return NullCleaner.assertNotNull(Collections.unmodifiableList(
				new ArrayList<>(getUnits(player).stream().map(IUnit::getKind)
										.collect(Collectors.toSet()))));
	}

	/**
	 * @param player a player in the map
	 * @param kind   a "kind" of unit.
	 * @return a list of the units of that kind in the map belonging to that player
	 */
	@Override
	public List<IUnit> getUnits(final Player player, final String kind) {
		return getUnits(player).stream().filter(unit -> kind.equals(unit.getKind()))
					.collect(Collectors.toList());
	}

	/**
	 * @param unit the unit to add
	 */
	@Override
	public void addUnit(final IUnit unit) {
		for (final Point point : getMap().locations()) {
			if (getMap().streamOtherFixtures(point).filter(Fortress.class::isInstance)
						.map(Fortress.class::cast).anyMatch(
							fort -> "HQ".equals(fort.getName()) &&
											unit.getOwner().equals(fort.getOwner()))) {
					addUnitAtLocation(unit, point);
					return;
				}
			}
		SystemOut.SYS_OUT.println("No suitable location found");
	}

	/**
	 * @param owner the unit's owner
	 * @param id    the ID # to search for
	 * @return the unit with that ID, or null if none.
	 */
	@Override
	public @Nullable IUnit getUnitByID(final Player owner, final int id) {
		final Optional<IUnit> retval =
				getUnits(owner).stream().filter(unit -> id == unit.getID()).findAny();
		if (retval.isPresent()) {
			return retval.get();
		} else {
			return null;
		}
	}
	/**
	 * Add a unit at the given location in all maps.
	 * @param unit the unit to add
	 * @param location where to add it
	 */
	@SuppressWarnings("NonBooleanMethodNameMayNotStartWithQuestion")
	private void addUnitAtLocation(final IUnit unit, final Point location) {
		if (getSubordinateMaps().iterator().hasNext()) {
			for (final Pair<IMutableMapNG, Optional<Path>> pair : getAllMaps()) {
				final Optional<Fortress> fort = NullCleaner.assertNotNull(
						pair.first().streamOtherFixtures(location)
								.filter(Fortress.class::isInstance)
								.map(Fortress.class::cast).filter(fix -> unit
										.getOwner().equals(fix.getOwner()))
						.findAny());
				if (fort.isPresent()) {
					fort.get().addMember(unit.copy(false));
				} else {
					pair.first().addFixture(location, unit.copy(false));
				}
			}
		} else {
			final Optional<Fortress> fort = NullCleaner
					.assertNotNull(getMap().streamOtherFixtures(location)
							.filter(Fortress.class::isInstance)
							.map(Fortress.class::cast).filter(fix -> unit
									.getOwner().equals(fix.getOwner()))
					.findAny());
			if (fort.isPresent()) {
				fort.get().addMember(unit.copy(false));
			} else {
				getMap().addFixture(location, unit.copy(false));
			}
		}
	}

	/**
	 * @return a String representation of the object
	 */
	@SuppressWarnings("MethodReturnAlwaysConstant")
	@Override
	public String toString() {
		return "WorkerModel";
	}
}
