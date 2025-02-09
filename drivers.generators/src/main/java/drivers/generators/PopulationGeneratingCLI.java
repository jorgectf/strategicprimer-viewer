package drivers.generators;

import org.javatuples.Pair;
import drivers.common.CLIDriver;
import drivers.common.EmptyOptions;
import drivers.common.SPOptions;

import drivers.common.cli.ICLIHelper;

import common.map.Point;
import common.map.HasExtent;
import common.map.IMapNG;

import common.map.fixtures.mobile.Animal;

import java.util.Random;

import common.map.fixtures.resources.Grove;
import common.map.fixtures.resources.Shrub;
import common.map.fixtures.resources.Meadow;

import common.map.fixtures.terrain.Forest;

import exploration.common.SurroundingPointIterable;

import common.map.fixtures.towns.ITownFixture;

import java.math.BigDecimal;

import lovelace.util.SingletonRandom;

import java.util.function.Predicate;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Objects;

import java.util.stream.Collectors;
import java.math.RoundingMode;

/**
 * A driver to let the user generate animal and shrub populations, meadow and
 * grove sizes, and forest acreages.
 */
public class PopulationGeneratingCLI implements CLIDriver {
	/**
	 * Whether the given number is positive.
	 */
	private static boolean positiveNumber(final Number number) {
		return number.doubleValue() > 0.0;
	}

	/**
	 * Whether the given number is negative.
	 */
	private static boolean negativeNumber(final Number number) {
		return number.doubleValue() < 0.0;
	}

	private final ICLIHelper cli;
	private final PopulationGeneratingModel model;
	@Override
	public PopulationGeneratingModel getModel() {
		return model;
	}

	@Override
	public SPOptions getOptions() {
		return EmptyOptions.EMPTY_OPTIONS;
	}

	public PopulationGeneratingCLI(final ICLIHelper cli, final PopulationGeneratingModel model) {
		this.cli = cli;
		this.model = model;
		map = model.getMap();
	}

	private final IMapNG map;

	/**
	 * Generate {@link Animal} populations.
	 */
	private void generateAnimalPopulations(final boolean talking, final String kind) {
		// We assume there is at most one population of each kind of animal per tile.
		final List<Point> locations = map.streamLocations()
			.filter(l -> map.getFixtures(l).stream()
				.filter(Animal.class::isInstance).map(Animal.class::cast)
				.filter(a -> a.isTalking() == talking)
				.filter(a -> kind.equals(a.getKind()))
				.anyMatch(a -> a.getPopulation() <= 0))
			.collect(Collectors.toList());
		if (locations.isEmpty()) {
			return;
		}
		Collections.shuffle(locations);
		final int count = locations.size();
		final String key = (talking) ? "talking " + kind : kind;
		final int total = Optional.ofNullable(cli.inputNumber(String.format(
			"There are %d groups of %s in the world; what should their total population be?",
			count, key))).orElse(0);
		int remainingTotal = total;
		int remainingCount = count;
		final Random rng = SingletonRandom.SINGLETON_RANDOM;
		for (final Point location : locations) {
			final int temp = (remainingCount * 2) + 2;
			if (remainingTotal <= temp) {
				cli.println(String.format(
					"With %d groups left, there is only %d left, not enough for 2 or more each",
					remainingCount, remainingTotal));
				cli.println(String.format("Adjusting up by %d", remainingCount * 3));
				remainingTotal += remainingCount * 3;
			}
			final int nextPopulation;
			if (remainingCount == 1) {
				nextPopulation = remainingTotal;
			} else if (remainingCount < 1) {
				cli.println("Ran out of locations while generating " + key);
				return;
			} else {
				nextPopulation =
					rng.nextInt(remainingTotal - (remainingCount * 2) - 2) + 2;
			}
			if (model.setAnimalPopulation(location, talking, kind, nextPopulation)) {
				remainingCount--;
				remainingTotal -= nextPopulation;
			} // TODO: else log a warning
		}
	}

	/**
	 * Generate {@link Grove grove and orchard} populations.
	 */
	private void generateGroveCounts(final String kind) {
		// We assume there is at most one grove or orchard of each kind per tile.
		final List<Point> locations = map.streamLocations()
			.filter(l -> map.getFixtures(l).stream()
				.filter(Grove.class::isInstance).map(Grove.class::cast)
				.filter(g -> kind.equals(g.getKind()))
				.anyMatch(g -> g.getPopulation() <= 0))
			.collect(Collectors.toList());
		if (locations.isEmpty()) {
			return;
		}
		Collections.shuffle(locations);
		final int count = locations.size();
		final int total = Optional.ofNullable(cli.inputNumber(String.format(
			"There are %d groves or orchards of %s in the world; what should their total population be? ",
			count, kind))).orElse(0);
		int remainingTotal = total;
		int remainingCount = count;
		final Random rng = SingletonRandom.SINGLETON_RANDOM;
		for (final Point location : locations) {
			if (remainingTotal < remainingCount) {
				cli.println(String.format("With %d groups left, there is only %s left",
					remainingCount, remainingTotal)); // TODO: adjust instead?
				return;
			}
			final int nextPopulation = (remainingCount == 1) ? remainingTotal :
				rng.nextInt(remainingTotal-remainingCount - 1) + 1;
			if (model.setGrovePopulation(location, kind, nextPopulation)) {
				remainingCount--;
				remainingTotal -= nextPopulation;
			} // TODO: else log a warning
		}
	}

	/**
	 * Generate {@link Shrub} populations.
	 */
	private void generateShrubCounts(final String kind) {
		// We assume there is at most one population of each kind of shrub per tile.
		final List<Point> locations = map.streamLocations()
			.filter(l -> map.getFixtures(l).stream()
				.filter(Shrub.class::isInstance).map(Shrub.class::cast)
				.filter(s -> kind.equals(s.getKind()))
				.anyMatch(s -> s.getPopulation() <= 0))
			.collect(Collectors.toList());
		if (locations.isEmpty()) {
			return;
		}
		Collections.shuffle(locations);
		final int count = locations.size();
		final int total = Optional.ofNullable(cli.inputNumber(String.format(
			"There are %d populations of %s in the world; what should their total population be? ",
			count, kind))).orElse(0);
		int remainingTotal = total;
		int remainingCount = count;
		final Random rng = SingletonRandom.SINGLETON_RANDOM;
		for (final Point location : locations) {
			if (remainingTotal < remainingCount) {
				cli.println(String.format("With %d groups left, there is only %s left",
					remainingCount, remainingTotal)); // TODO: adjust instead?
				return;
			}
			final int nextPopulation = (remainingCount == 1) ? remainingTotal :
				rng.nextInt(remainingTotal-remainingCount - 1) + 1;
			if (model.setShrubPopulation(location, kind, nextPopulation)) {
				remainingCount--;
				remainingTotal -= nextPopulation;
			}
		}
	}

	/**
	 * Generate {@link Meadow field and meadow} acreages.
	 */
	private void generateFieldExtents() {
		final List<Pair<Point, Meadow>> entries = map.streamLocations()
			.flatMap(l -> map.getFixtures(l).stream()
				.filter(Meadow.class::isInstance).map(Meadow.class::cast)
				.filter(m -> m.getAcres().doubleValue() < 0.0)
				.map(f -> Pair.with(l, f)))
			.collect(Collectors.toList());
		Collections.shuffle(entries);
		final Random rng = SingletonRandom.SINGLETON_RANDOM;
		for (final Pair<Point, Meadow> entry : entries) {
			final Point loc = entry.getValue0();
			final Meadow field = entry.getValue1();
			final double acres = rng.nextDouble() * 5.5 + 0.5;
			model.setFieldExtent(loc, field, acres);
		}
	}

	/**
	 * Whether any of the fixtures on the given tile are forests of the given kind.
	 */
	private Predicate<Point> hasForests(final String kind) {
		return (point) -> map.getFixtures(point).stream().filter(Forest.class::isInstance)
			.map(Forest.class::cast).anyMatch(f -> kind.equals(f.getKind()));
	}

	/**
	 * How many tiles adjacent to the given location have forests of the given kind.
	 */
	private int countAdjacentForests(final Point center, final String kind) {
		return (int)
				new SurroundingPointIterable(center, map.getDimensions(), 1).stream()
			.filter(hasForests(kind)).count();
	}

	private static BigDecimal perForestAcreage(final BigDecimal reserved, final int otherForests) {
		return new BigDecimal(160).subtract(reserved)
			.divide(new BigDecimal(otherForests), RoundingMode.HALF_EVEN);
	}

	private static Number acreageExtent(final HasExtent<?> item) {
		return item.getAcres();
	}

	/**
	 * Generate {@link Forest} acreages.
	 */
	private void generateForestExtents() {
		final List<Point> locations = map.streamLocations()
			.filter(l -> map.getFixtures(l).stream()
				.filter(Forest.class::isInstance).map(Forest.class::cast)
				.anyMatch(f -> f.getAcres().doubleValue() <= 0.0))
			.collect(Collectors.toList());
		Collections.shuffle(locations);
		for (final Point location : locations) {
			final Forest primaryForest = map.getFixtures(location).stream()
				.filter(Forest.class::isInstance).map(Forest.class::cast)
				.findFirst().orElseThrow(
					() -> new IllegalStateException("Not found despite double-checking"));
			BigDecimal reserved = BigDecimal.ZERO;
			if (primaryForest.getAcres().doubleValue() > 0.0) {
				cli.println(String.format("First forest at %s had acreage set already.",
					location));
				reserved = map.getFixtures(location).stream()
					.filter(Forest.class::isInstance).map(Forest.class::cast)
					.map(Forest::getAcres).filter(n -> n.doubleValue() > 0.0)
					.map(n -> {// FIXME: Use lovelace.util.Decimalize
						if (n instanceof BigDecimal) {
							return (BigDecimal) n;
						} else {
							return new BigDecimal(n.doubleValue());
						}
					}).reduce(reserved, BigDecimal::add);
			}
			final List<Forest> otherForests = map.getFixtures(location).stream()
				.filter(Forest.class::isInstance).map(Forest.class::cast)
				.filter(f -> !Objects.equals(f, primaryForest))
				.filter(f -> f.getAcres().doubleValue() <= 0.0).toList();
			final int adjacentCount = countAdjacentForests(location, primaryForest.getKind());
			for (final ITownFixture town : map.getFixtures(location).stream()
					.filter(ITownFixture.class::isInstance).map(ITownFixture.class::cast).toList()) {
				reserved = switch (town.getTownSize()) { // TODO: Pull the reserved.add() to outside the expression
					case Small -> reserved.add(new BigDecimal(15));
					case Medium -> reserved.add(new BigDecimal(40));
					case Large -> reserved.add(new BigDecimal(80));
				};
			}
			reserved = reserved.add(new BigDecimal(map.getFixtures(location).stream()
				.filter(Grove.class::isInstance).map(Grove.class::cast)
				.mapToInt(Grove::getPopulation).filter(p -> p > 0).sum())
				.divide(new BigDecimal(500)));
			reserved = map.getFixtures(location).stream().filter(HasExtent.class::isInstance)
				.filter(f -> !(f instanceof Forest)) // already counted above
				.map(HasExtent.class::cast).map(HasExtent::getAcres)
				.filter(n -> n.doubleValue() > 0.0).map(n -> {// FIXME: lovelace.util.Decimalize
					if (n instanceof BigDecimal) {
						return (BigDecimal) n;
					} else {
						return new BigDecimal(n.doubleValue());
					}
				}).reduce(reserved, BigDecimal::add);
			final BigDecimal fullTile = new BigDecimal(160);
			if (reserved.compareTo(fullTile) >= 0) {
				cli.println("The whole tile or more was reserved, despite forests, at " + location);
				continue;
			}
			if (otherForests.isEmpty()) {
				final BigDecimal acreage;
				if (adjacentCount > 7) {
					acreage = fullTile.subtract(reserved);
				} else if (adjacentCount > 4) {
					acreage = fullTile.subtract(reserved)
						.multiply(new BigDecimal(4).divide(new BigDecimal(5)));
				} else {
					acreage = fullTile.subtract(reserved)
						.multiply(new BigDecimal(2).divide(new BigDecimal(5)));
				}
				model.setForestExtent(location, primaryForest, acreage);
			} else {
				final BigDecimal acreage;
				if (adjacentCount > 4) {
					acreage = fullTile.subtract(reserved)
						.multiply(new BigDecimal(4).divide(new BigDecimal(5)));
				} else {
					acreage = fullTile.subtract(reserved)
						.multiply(new BigDecimal(2).divide(new BigDecimal(5)));
				}
				model.setForestExtent(location, primaryForest, acreage);
				reserved = reserved.add(acreage);
				for (final Forest forest : otherForests) {
					model.setForestExtent(location, forest,
						perForestAcreage(reserved, otherForests.size()));
				}
			}
		}
	}

	@Override
	public void startDriver() {
		for (final String kind : map.streamAllFixtures()
				.filter(Animal.class::isInstance).map(Animal.class::cast)
				.filter(a -> a.getPopulation() <= 0).map(Animal::getKind)
				.collect(Collectors.toSet())) {
			generateAnimalPopulations(true, kind);
			generateAnimalPopulations(false, kind);
		}
		// TODO: filter these to only those without un-counted instances?
		map.streamAllFixtures()
			.filter(Grove.class::isInstance).map(Grove.class::cast)
			.map(Grove::getKind).distinct().forEach(this::generateGroveCounts);
		map.streamAllFixtures()
			.filter(Shrub.class::isInstance).map(Shrub.class::cast)
			.map(Shrub::getKind).distinct().forEach(this::generateShrubCounts);
		generateFieldExtents();
		generateForestExtents();
		model.setMapModified(true);
	}
}
