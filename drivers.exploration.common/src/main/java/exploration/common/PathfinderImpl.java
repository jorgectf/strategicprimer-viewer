package exploration.common;

import org.jetbrains.annotations.Nullable;
import org.javatuples.Pair;
import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import common.map.IMapNG;
import common.map.Direction;
import common.map.Point;
import common.map.fixtures.terrain.Forest;
import java.util.function.Predicate;
import java.util.Collections;
import java.util.Optional;
import java.util.logging.Logger;

/* package */ class PathfinderImpl implements Pathfinder {
	private static final Logger LOGGER = Logger.getLogger(PathfinderImpl.class.getName());
	public PathfinderImpl(final IMapNG map) {
		this.map = map;
	}
	private final IMapNG map;
	private final Map<Pair<Point, Point>, Integer> tentativeDistances = new HashMap<>();
	private Predicate<Map.Entry<Pair<Point, Point>, Integer>> forUs(final Point base, final Set<Point> unvisited) {
		return entry -> entry.getKey().getValue0().equals(base) &&
			unvisited.contains(entry.getKey().getValue1());
	}

	@Nullable
	private Point nextUnvisited(final Point base, final Set<Point> unvisited) {
		return tentativeDistances.entrySet().stream().filter(forUs(base, unvisited)).min(Map.Entry.comparingByValue())
			.map(Map.Entry::getKey).map(Pair::getValue1).orElse(null);
	}

	private static Direction getDirection(final Point one, final Point two) {
		if (one.getRow() < two.getRow()) {
			if (one.getColumn() < two.getColumn()) {
				return Direction.Northeast;
			} else if (one.getColumn() == two.getColumn()) {
				return Direction.North;
			} else {
				return Direction.Northwest;
			}
		} else if (one.getRow() == two.getRow()) {
			if (one.getColumn() < two.getColumn()) {
				return Direction.East;
			} else if (one.getColumn() == two.getColumn()) {
				return Direction.Nowhere;
			} else {
				return Direction.West;
			}
		} else {
			if (one.getColumn() < two.getColumn()) {
				return Direction.Southeast;
			} else if (one.getColumn() == two.getColumn()) {
				return Direction.South;
			} else {
				return Direction.Southwest;
			}
		}
	}

	/**
	 * The shortest-path distance, avoiding obstacles, in MP, between two
	 * points, using Dijkstra's algorithm.
	 */
	@Override
	public Pair<Integer, Iterable<Point>> getTravelDistance(final Point start, final Point end) {
		final Set<Point> unvisited = new HashSet<>();
		for (final Point point : map.getLocations()) {
			unvisited.add(point);
			if (!tentativeDistances.containsKey(Pair.with(start, point))) {
				tentativeDistances.put(Pair.with(start, point), Integer.MAX_VALUE - 1);
			}
		}
		tentativeDistances.put(Pair.with(start, start), 0);
		Point current = start;
		int iterations = 0;
		final Map<Point, Point> retval = new HashMap<>();
		while (!unvisited.isEmpty()) {
			iterations++;
			if (!tentativeDistances.containsKey(Pair.with(start, current))) {
				throw new IllegalStateException("Tentative distance missing");
			}
			final int currentDistance = tentativeDistances.get(Pair.with(start, current));
			if (current.equals(end)) {
				LOGGER.info(String.format("Reached the end after %d iterations",
					iterations));
				final List<Point> path = new ArrayList<>();
				path.add(current);
				while (retval.containsKey(current)) {
					path.add(retval.get(current));
					current = retval.get(current);
				}
				Collections.reverse(path);
				return Pair.with(currentDistance, Collections.unmodifiableList(path));
			} else if (currentDistance >= (Integer.MAX_VALUE - 1)) {
				LOGGER.info(String.format(
					"Considering an 'infinite-distance' tile after %d iterations",
					iterations));
				return Pair.with(currentDistance, Collections.emptyList());
			}
			for (final Point neighbor : new SurroundingPointIterable(current, map.getDimensions(), 1)) {
				LOGGER.fine(String.format("At %s, considering %s", current, neighbor));
				if (!unvisited.contains(neighbor)) {
					LOGGER.fine("Already checked, so skipping.");
					continue;
				}
				if (!tentativeDistances.containsKey(Pair.with(start, neighbor))) {
					throw new IllegalStateException("Missing prior estimate");
				}
				final int estimate = tentativeDistances.get(Pair.with(start, neighbor));
				final int tentativeDistance = currentDistance +
					SimpleMovementModel.movementCost(map.getBaseTerrain(neighbor),
						map.getFixtures(neighbor).stream().anyMatch(Forest.class::isInstance),
						map.isMountainous(neighbor),
						SimpleMovementModel.riversSpeedTravel(
							getDirection(current, neighbor),
							map.getRivers(current),
							map.getRivers(neighbor)),
						map.getFixtures(neighbor));
				LOGGER.fine(String.format("Old estimate %d, new estimate %d", estimate,
					tentativeDistance));
				if (tentativeDistance < estimate) {
					LOGGER.fine("Updating path");
					retval.put(neighbor, current);
					tentativeDistances.put(Pair.with(start, neighbor),
						tentativeDistance);
				}
				if (estimate < 0) {
					LOGGER.warning(String.format("Old estimate at %s was negative",
						neighbor));
					return Pair.with(Integer.MAX_VALUE - 1, Collections.emptyList());
				} else if (tentativeDistance < 0) {
					LOGGER.warning(String.format("Recomputed estimate at %s was negative",
						neighbor));
					return Pair.with(Integer.MAX_VALUE - 1, Collections.emptyList());
				}
			}
			LOGGER.fine("Finished checking neighbors of " + current);
			unvisited.remove(current);
			final Point next = nextUnvisited(start, unvisited);
			if (next == null) {
				LOGGER.info(String.format(
					"Couldn't find a smallest-estimate unchecked tile after %d iterations",
					iterations));
				return Pair.with(Integer.MAX_VALUE - 1, Collections.emptyList());
			} else {
				current = next;
			}
		}
		LOGGER.info(String.format("Apparently ran out of tiles after %d iterations", iterations));
		return Pair.with(Optional.ofNullable(tentativeDistances.get(Pair.with(start, end)))
			.orElse(Integer.MAX_VALUE - 1), Collections.emptyList());
	}
}
