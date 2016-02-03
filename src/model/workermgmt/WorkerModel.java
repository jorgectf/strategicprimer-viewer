package model.workermgmt;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import model.map.IMapNG;
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
 * Copyright (C) 2013-2015 Jonathan Lovelace
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
public final class WorkerModel extends SimpleMultiMapModel implements IWorkerModel {
	/**
	 * Constructor.
	 *
	 * @param map  the map we're wrapping.
	 * @param file the file the map was loaded from or should be saved to
	 */
	public WorkerModel(final IMutableMapNG map, final File file) {
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
						false))
				       .collect(Collectors.toSet()));
	}

	/**
	 * @param player a player in the map
	 * @return a list of that player's units
	 */
	@Override
	public List<IUnit> getUnits(final Player player) {
		if (getSubordinateMaps().iterator().hasNext()) {
			final Map<Integer, IUnit> retval = new TreeMap<>();
			for (final Pair<IMutableMapNG, File> pair : getAllMaps()) {
				final IMapNG map = pair.first();
				for (final Point point : map.locations()) {
					for (final IUnit unit : getUnits(map.getOtherFixtures(point),
							player)) {
						final IUnit proxy;
						if (retval.containsKey(Integer.valueOf(unit.getID()))) {
							proxy = retval.get(Integer.valueOf(unit.getID()));
							((ProxyFor<IUnit>) proxy).addProxied(unit);
						} else {
							proxy = new ProxyUnit(unit.getID());
							((ProxyFor<IUnit>) proxy).addProxied(unit);
							retval.put(NullCleaner.assertNotNull(
									Integer.valueOf(unit.getID())), proxy);
						}
					}
				}
			}
			return new ArrayList<>(retval.values());
		} else {
			// Just in case I missed something in the proxy implementation, make
			// sure things work correctly when there's only one map.
			// TODO: Improve Stream API usage
			return new ArrayList<>(getUnits(getMap().locationStream()
					                .flatMap(point -> getMap().streamOtherFixtures(point))
					                .collect(Collectors.toList()), player));
		}
	}

	/**
	 * TODO: Take Stream instead of Iterable
	 * @param iter   a sequence of members of that type
	 * @param player a player
	 * @return a list of the members of the sequence that are units owned by the player
	 */
	private static Collection<IUnit> getUnits(final Iterable<? super Unit> iter,
											  final Player player) {
		final Collection<IUnit> retval = new ArrayList<>();
		for (final Object obj : iter) {
			if ((obj instanceof IUnit) && ((IUnit) obj).getOwner().equals(player)) {
				retval.add((IUnit) obj);
			} else if (obj instanceof Fortress) {
				retval.addAll(getUnits((Fortress) obj, player));
			}
		}
		return retval;
	}

	/**
	 * @param player a player in the map
	 * @return the "kinds" of unit that player has.
	 */
	@Override
	public List<String> getUnitKinds(final Player player) {
		return NullCleaner.assertNotNull(Collections
												 .unmodifiableList(
														 new ArrayList<>(getUnits(player)
																				 .stream()
																				 .map
																						  (IUnit::getKind)

																				 .collect(
																						 Collectors
																								 .toSet()))));
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

	private void addUnitAtLocation(final IUnit unit, final Point location) {
		if (getSubordinateMaps().iterator().hasNext()) {
			for (final Pair<IMutableMapNG, File> pair : getAllMaps()) {
				Optional<Fortress> fort = pair.first().streamOtherFixtures(location)
						                          .filter(Fortress.class::isInstance)
						                          .map(Fortress.class::cast)
						                          .filter(fix -> unit.getOwner()
								                                          .equals(fix.getOwner()))

						                          .findAny();
				if (fort.isPresent()) {
					fort.get().addMember(unit.copy(false));
				} else {
					pair.first().addFixture(location, unit.copy(false));
				}
			}
		} else {
			Optional<Fortress> fort = getMap().streamOtherFixtures(location)
					                          .filter(Fortress.class::isInstance)
					                          .map(Fortress.class::cast)
					                          .filter(fix -> unit.getOwner()
							                                         .equals(fix.getOwner()))

					                          .findAny();
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
