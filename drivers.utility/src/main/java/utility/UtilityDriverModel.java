package utility;

import org.jetbrains.annotations.Nullable;

import common.map.TileType;
import java.util.stream.Collectors;
import java.util.Optional;
import common.map.River;
import common.map.fixtures.FixtureIterable;
import common.map.fixtures.UnitMember;
import common.map.fixtures.FortressMember;
import java.util.Collections;
import java.util.LinkedList;
import java.util.function.Consumer;
import org.javatuples.Quartet;
import java.nio.file.Path;
import java.util.Map;
import common.map.Direction;
import common.map.HasExtent;
import common.map.HasOwner;
import common.map.HasPopulation;
import common.map.IFixture;
import common.map.IMapNG;
import common.map.IMutableMapNG;
import common.map.Player;
import common.map.Point;
import common.map.Subsettable;
import common.map.TileFixture;

import drivers.common.SimpleMultiMapModel;
import drivers.common.IDriverModel;

import java.util.List;
import java.util.ArrayList;

import common.map.fixtures.Ground;

import common.map.fixtures.resources.CacheFixture;

import common.map.fixtures.mobile.Animal;
import common.map.fixtures.mobile.IMutableUnit;
import common.map.fixtures.mobile.IUnit;

import common.map.fixtures.terrain.Forest;

import common.map.fixtures.towns.IMutableFortress;
import common.map.fixtures.towns.ITownFixture;

import exploration.common.Speed;
import exploration.common.SurroundingPointIterable;
import exploration.common.SimpleMovementModel;

/**
 * A driver model for the various utility drivers.
 */
public class UtilityDriverModel extends SimpleMultiMapModel {
	private static <Desired, Provided> Consumer<Provided> ifApplicable(Consumer<Desired> func,
			Class<Desired> cls) {
		return (item) -> {
			if (cls.isInstance(item)) {
				func.accept((Desired) item);
			}
		};
	}

	private static boolean isSubset(IFixture one, IFixture two) {
		if (one instanceof Subsettable) { // TODO: Extract SubsettableFixture interface
			return ((Subsettable<IFixture>) one).isSubset(two, s -> {});
		} else {
			return one.equals(two);
		}
	}

	private static class Mock implements HasOwner {
		public Mock(Player owner) {
			this.owner = owner;
		}

		private final Player owner;

		@Override
		public Player getOwner() {
			return owner;
		}
	}

	public UtilityDriverModel(IMutableMapNG map) {
		super(map);
	}

	public UtilityDriverModel(IDriverModel model) { // TODO: Make protected/private and provide static copyConstructor() instead?
		super(model);
	}

	/**
	 * Copy rivers at the given {@link location} missing from subordinate
	 * maps, where they have other terrain information, from the main map.
	 */
	public void copyRiversAt(Point location) {
		IMapNG map = getMap();
		for (IMutableMapNG subordinateMap : getRestrictedSubordinateMaps()) {
			TileType mainTerrain = map.getBaseTerrain(location);
			TileType subTerrain = subordinateMap.getBaseTerrain(location);
			if (mainTerrain != null && subTerrain != null && mainTerrain.equals(subTerrain) &&
					!map.getRivers(location).isEmpty() &&
					subordinateMap.getRivers(location).isEmpty()) {
				subordinateMap.addRivers(location,
					map.getRivers(location).stream().toArray(River[]::new));
				subordinateMap.setModified(true);
			}
		}
	}

	/**
	 * Conditionally remove duplicate fixtures. Returns a list of fixtures
	 * that would be removed and a callback to do the removal; the initial
	 * caller of this asks the user for approval.
	 */
	public Iterable<Quartet<Consumer<TileFixture>, @Nullable Path, TileFixture, Iterable<? extends TileFixture>>>
			conditionallyRemoveDuplicates(Point location) {
		List<Quartet<Consumer<TileFixture>, @Nullable Path, TileFixture, Iterable<? extends TileFixture>>>
			duplicatesList = new ArrayList<>();
		List<TileFixture> checked = new ArrayList<>();
		for (IMutableMapNG map : getRestrictedAllMaps()) {
			for (TileFixture fixture : map.getFixtures(location)) {
				checked.add(fixture);
				if (fixture instanceof IUnit &&
						((IUnit) fixture).getKind().contains("TODO")) {
					continue;
				} else if (fixture instanceof CacheFixture) {
					continue;
				} else if (fixture instanceof HasPopulation &&
						((HasPopulation<?>) fixture).getPopulation() > 0) {
					continue;
				} else if (fixture instanceof HasExtent &&
						((HasExtent<?>) fixture).getAcres().doubleValue() > 0.0) {
					continue;
				}
				List<TileFixture> matching = map.getFixtures(location).stream()
					.filter(item -> checked.stream().noneMatch(inner -> item == inner))
					.filter(fixture::equalsIgnoringID)
					.collect(Collectors.toList());
				if (!matching.isEmpty()) {
					duplicatesList.add(Quartet.with(
						item -> map.removeFixture(location, item), 
						map.getFilename(), fixture, matching));
				}
			}
		}
		return duplicatesList;
	}

	private List<Quartet<Runnable, String, String, Iterable<? extends IFixture>>> coalesceImpl(
			String context, Iterable<? extends IFixture> stream, Consumer<IFixture> add,
			Consumer<IFixture> remove, Runnable setModFlag,
			Map<Class<? extends IFixture>, CoalescedHolder<? extends IFixture, ?>> handlers) {
		List<Quartet<Runnable, String, String, Iterable<? extends IFixture>>> retval =
			new ArrayList<>();
		for (IFixture fixture : stream) {
			if (fixture instanceof FixtureIterable) {
				String shortDesc = (fixture instanceof TileFixture) ?
					((TileFixture) fixture).getShortDescription() : fixture.toString();
				if (fixture instanceof IMutableUnit) {
					retval.addAll(coalesceImpl(
						String.format("%sIn %s: ", context, shortDesc),
						(IMutableUnit) fixture,
						ifApplicable(((IMutableUnit) fixture)::addMember,
							UnitMember.class),
						ifApplicable(((IMutableUnit) fixture)::removeMember,
							UnitMember.class),
						setModFlag, handlers));
				} else if (fixture instanceof IMutableFortress) {
					retval.addAll(coalesceImpl(
						String.format("%sIn %s: ", context, shortDesc),
						(IMutableFortress) fixture,
						ifApplicable(((IMutableFortress) fixture)::addMember,
							FortressMember.class),
						ifApplicable(((IMutableFortress) fixture)::removeMember,
							FortressMember.class),
						setModFlag, handlers));
				} // TODO: else log a warning about an unhandled case
			} else if (fixture instanceof Animal) {
				if (((Animal) fixture).isTalking()) {
					continue;
				}
				if (handlers.containsKey(Animal.class)) {
					handlers.get(Animal.class).addIfType(fixture);
				}
			} else if (fixture instanceof HasPopulation &&
					((HasPopulation<?>) fixture).getPopulation() < 0) {
				continue;
			} else if (fixture instanceof HasExtent &&
					((HasExtent<?>) fixture).getAcres().doubleValue() <= 0.0) {
				continue;
			} else if (handlers.containsKey(fixture.getClass())) {
				handlers.get(fixture.getClass()).addIfType(fixture);
			}
		}

		for (CoalescedHolder<? extends IFixture, ?> handler : handlers.values()) {
			for (List<? extends IFixture> list : handler) {
				if (list.isEmpty() || list.size() == 1) {
					continue;
				}
				retval.add(Quartet.with(() -> {
						IFixture combined = handler.combineRaw(
							list.stream().toArray(IFixture[]::new));
						list.forEach(remove);
						add.accept(combined);
						setModFlag.run();
					}, context, handler.getPlural().toLowerCase(), list));
			}
		}
		return retval;
	}

	/**
	 * Conditionally coalesce like resources in a location. Returns a list
	 * of resources that would be combined and a callback to do the
	 * operation; the initial caller of this ask the user for approval.
	 */
	public Iterable<Quartet<Runnable, String, String, Iterable<? extends IFixture>>>
			conditionallyCoalesceResources(Point location,
				Map<Class<? extends IFixture>,
					CoalescedHolder<? extends IFixture, ?>> handlers) {
		List<Quartet<Runnable, String, String, Iterable<? extends IFixture>>> retval = new ArrayList<>();
		for (IMutableMapNG map : getRestrictedAllMaps()) {
			retval.addAll(coalesceImpl(
				String.format("In %s: At %s: ",
					Optional.ofNullable(map.getFilename())
						.map(Path::toString).orElse("a new file"),
					location),
				map.getFixtures(location),
			ifApplicable(fix -> map.addFixture(location, fix), TileFixture.class),
			ifApplicable(fix -> map.removeFixture(location, fix), TileFixture.class),
			() -> map.setModified(true), handlers));
		}
		return retval;
	}

	/**
	 * Remove information in the main map from subordinate maps.
	 */
	public void subtractAtPoint(Point location) {
		IMapNG map = getMap();
		for (IMutableMapNG subMap : getRestrictedSubordinateMaps()) {
			subMap.setModified(true);
			TileType terrain = map.getBaseTerrain(location);
			TileType ours = subMap.getBaseTerrain(location);
			if (terrain != null && ours != null && terrain.equals(ours)) {
				subMap.setBaseTerrain(location, null);
			}
			subMap.removeRivers(location,
				map.getRivers(location).stream().toArray(River[]::new));
			Map<Direction, Integer> mainRoads = map.getRoads(location);
			Map<Direction, Integer> knownRoads = subMap.getRoads(location);
			for (Map.Entry<Direction, Integer> entry : knownRoads.entrySet()) {
				Direction direction = entry.getKey();
				int road = entry.getValue();
				if (mainRoads.getOrDefault(direction, 0) >= road) {
					subMap.setRoadLevel(location, direction, 0);
				}
			}
			if (map.isMountainous(location)) {
				subMap.setMountainous(location, false);
			}
			for (TileFixture fixture : subMap.getFixtures(location)) {
				if (map.getFixtures(location).stream()
						.anyMatch(item -> isSubset(item, fixture))) {
					subMap.removeFixture(location, fixture);
				}
			}
		}
	}

	private static List<Forest> extractForests(IMapNG map, Point location) {
		return map.getFixtures(location).stream().filter(Forest.class::isInstance)
			.map(Forest.class::cast).collect(Collectors.toList());
	}

	private static List<Ground> extractGround(IMapNG map, Point location) {
		return map.getFixtures(location).stream().filter(Ground.class::isInstance)
			.map(Ground.class::cast).collect(Collectors.toList());
	}

	public void fixForestsAndGround(Consumer<String> ostream) {
		for (IMapNG map : getSubordinateMaps()) {
			ostream.accept(String.format("Starting %s",
				Optional.ofNullable(map.getFilename())
					.map(Path::toString).orElse("a map with no associated path")));

			for (Point location : map.getLocations()) {
				List<Forest> mainForests = extractForests(getMap(), location);
				List<Forest> subForests = extractForests(map, location);
				for (Forest forest : subForests) {
					if (mainForests.contains(forest)) {
						continue;
					}
					Forest matching = mainForests.stream()
						.filter(forest::equalsIgnoringID).findAny().orElse(null);
					if (matching != null) { // TODO: invert
						forest.setId(matching.getId());
						setMapModified(true);
					} else {
						ostream.accept(String.format("Unmatched forest in %s: %s",
							location, forest));
						getRestrictedMap().addFixture(location, forest.copy(false));
						setMapModified(true);
					}
				}

				List<Ground> mainGround = extractGround(getMap(), location);
				List<Ground> subGround = extractGround(map, location);
				for (Ground ground : subGround) {
					if (mainGround.contains(ground)) {
						continue;
					}
					Ground matching = mainGround.stream()
						.filter(ground::equalsIgnoringID).findAny().orElse(null);
					if (matching != null) { // TODO: invert
						ground.setId(matching.getId());
						setMapModified(true);
					} else {
						ostream.accept(String.format("Unmatched ground in %s: %s",
							location, ground));
						getRestrictedMap().addFixture(location, ground.copy(false));
						setMapModified(true);
					}
				}
			}
		}
	}

	private void safeAdd(IMutableMapNG map, Player currentPlayer, Point point, TileFixture fixture) {
		if (map.getFixtures(point).stream().anyMatch(fixture::equals)) {
			return;
		} else if (fixture instanceof HasOwner && !(fixture instanceof ITownFixture)) {
			TileFixture zeroed = fixture.copy(!((HasOwner) fixture).getOwner()
				.equals(currentPlayer));
			if (map.getFixtures(point).stream().noneMatch(zeroed::equals)) {
				map.addFixture(point,
					fixture.copy(!((HasOwner) fixture).getOwner()
						.equals(currentPlayer)));
			}
		} else {
			TileFixture zeroed = fixture.copy(true);
			if (map.getFixtures(point).stream().noneMatch(zeroed::equals)) {
				map.addFixture(point, fixture.copy(true));
			}
		}
	}

	public void expandAroundPoint(Point center, Player currentPlayer) {
		Mock mock = new Mock(currentPlayer);
		IMapNG map = getMap();
		for (IMutableMapNG subMap : getRestrictedSubordinateMaps()) {
			if (!subMap.getCurrentPlayer().equals(currentPlayer)) {
				continue;
			}

			for (Point neighbor : new SurroundingPointIterable(center, subMap.getDimensions())) {
				if (subMap.getBaseTerrain(neighbor) == null) {
					subMap.setBaseTerrain(neighbor, map.getBaseTerrain(neighbor));
					if (map.isMountainous(neighbor)) {
						subMap.setMountainous(neighbor, true);
					}
				}
				LinkedList<TileFixture> possibilities = new LinkedList<>();
				for (TileFixture fixture : map.getFixtures(neighbor)) {
					if (fixture instanceof CacheFixture ||
							subMap.getFixtures(neighbor).contains(fixture)) {
						continue;
					} else if (SimpleMovementModel.shouldAlwaysNotice(mock, fixture)) {
						safeAdd(subMap, currentPlayer, neighbor, fixture);
					} else if (SimpleMovementModel.shouldSometimesNotice(mock,
							Speed.Careful, fixture)) {
						possibilities.add(fixture);
					}
				}
				Collections.shuffle(possibilities); // FIXME: If multiple sub-maps, this copies different fixtures to each one
				if (!possibilities.isEmpty()) {
					TileFixture first = possibilities.removeFirst();
					safeAdd(subMap, currentPlayer, neighbor, first);
				}
			}
			subMap.setModified(true);
		}
	}
}
