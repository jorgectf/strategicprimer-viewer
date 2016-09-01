package controller.map.drivers;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Random;
import model.map.IMapNG;
import model.map.IMutableMapNG;
import model.map.MapDimensions;
import model.map.Point;
import model.map.PointFactory;
import model.misc.IDriverModel;
import model.misc.IMultiMapModel;
import model.viewer.TileViewSize;
import model.viewer.ViewerModel;
import util.NullCleaner;
import util.Pair;
import view.map.main.CachingTileDrawHelper;
import view.map.main.DirectTileDrawHelper;
import view.map.main.TileDrawHelper;
import view.map.main.Ver2TileDrawHelper;
import view.util.Coordinate;

import static view.util.SystemOut.SYS_OUT;

/**
 * A driver to compare the performance of TileDrawHelpers.
 *
 * This is part of the Strategic Primer assistive programs suite developed by Jonathan
 * Lovelace.
 *
 * Copyright (C) 2011-2016 Jonathan Lovelace
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of version 3 of the GNU General Public License as published by the Free Software
 * Foundation; see COPYING or
 * <a href="http://www.gnu.org/licenses/">http://www.gnu.org/licenses/</a>.
 *
 * @author Jonathan Lovelace
 */
public final class DrawHelperComparator implements SimpleDriver {
	/**
	 * The minimum row for the iteration-vs-filtering test.
	 */
	private static final int TEST_MIN_ROW = 20;
	/**
	 * The maximum row for the iteration-vs-filtering test.
	 */
	private static final int TEST_MAX_ROW = 40;
	/**
	 * The minimum col for the iteration-vs-filtering test.
	 */
	private static final int TEST_MIN_COL = 55;
	/**
	 * The maximum col for the iteration-vs-filtering test.
	 */
	private static final int TEST_MAX_COL = 82;

	/**
	 * An object indicating how to use and invoke this driver. We say that this is
	 * graphical, even though it's not, so we can share an option with the
	 * ReaderComparator.
	 */
	private static final DriverUsage USAGE =
			new DriverUsage(true, "-t", "--test", ParamCount.AtLeastOne,
								"Test drawing performance",
								String.format("Test the performance of the TileDrawHelper " +
										"classes---which do the heavy lifting of " +
										"rendering the map%nin the viewer---using a " +
										"variety of automated tests."),
								DrawHelperComparator.class);

	/**
	 * Label to put before every direct-helper test result.
	 */
	private static final String DIRECT = "Direct:  ";
	/**
	 * Label to put before every caching-helper test result.
	 */
	private static final String CACHING = "Caching:";
	/**
	 * Label to put before every version-2 helper test result.
	 */
	private static final String VER_TWO = "Ver. 2: ";

	/**
	 * The first test: all in one place.
	 *
	 * @param helper the helper to test
	 * @param map  the map being used for the test
	 * @param reps   the number of times to run this test between starting and stopping
	 *               the timer
	 * @param tileSize  the size to draw each tile
	 * @return how long the test took, in ns.
	 */
	private static long first(final TileDrawHelper helper, final IMapNG map,
							final int reps, final int tileSize) {
		final BufferedImage image = new BufferedImage(tileSize, tileSize,
															BufferedImage.TYPE_INT_RGB);
		final long start = System.nanoTime();
		firstBody(helper, image, map, reps, tileSize);
		final long end = System.nanoTime();
		return end - start;
	}

	/**
	 * The body of the first test.
	 *
	 * @param helper the helper to test
	 * @param image  the image used in the test.
	 * @param map  the map being used for the test
	 * @param reps   the number of times to run this test between starting and stopping
	 *               the timer
	 * @param tileSize  the size to draw each tile
	 */
	private static void firstBody(final TileDrawHelper helper,
								final BufferedImage image, final IMapNG map,
								final int reps,
								final int tileSize) {
		for (int rep = 0; rep < reps; rep++) {
			image.flush();
			for (final Point point : map.locations()) {
				helper.drawTileTranslated(
						NullCleaner.assertNotNull(image.createGraphics()),
						map, point, tileSize, tileSize);
			}
		}
	}

	/**
	 * The second test: Translating.
	 *
	 * @param helper the helper to test
	 * @param map    the map being used for the test
	 * @param reps   the number of times to run this test between starting and stopping
	 *               the timer
	 * @param tileSize  the size to draw each tile
	 * @return how long the test took, in ns.
	 */
	private static long second(final TileDrawHelper helper, final IMapNG map,
							final int reps, final int tileSize) {
		final MapDimensions dim = map.dimensions();
		final BufferedImage image = new BufferedImage(tileSize * dim.cols,
															tileSize * dim.rows,
															BufferedImage.TYPE_INT_RGB);
		final long start = System.nanoTime();
		secondBody(helper, image, map, reps, tileSize);
		final long end = System.nanoTime();
		return end - start;
	}

	/**
	 * The body of the second test.
	 *
	 * @param helper the helper to test
	 * @param image  the image used in the test.
	 * @param map    the map being used for the test
	 * @param reps   the number of times to run this test between starting and stopping
	 *               the timer
	 * @param tileSize  the size to draw each tile
	 */
	private static void secondBody(final TileDrawHelper helper,
								final BufferedImage image, final IMapNG map,
								final int reps,
								final int tileSize) {
		final Coordinate dimensions = PointFactory.coordinate(tileSize, tileSize);
		for (int rep = 0; rep < reps; rep++) {
			image.flush();
			for (final Point point : map.locations()) {
				helper.drawTile(
						NullCleaner.assertNotNull(image.createGraphics()),
						map, point,
						PointFactory.coordinate(point.getRow() * tileSize,
								point.getCol() * tileSize),
						dimensions);
			}
		}
	}

	/**
	 * Third test: in-place, reusing Graphics.
	 *
	 * @param helper the helper to test
	 * @param map  the map being used for the test
	 * @param reps   the number of times to run this test between starting and stopping
	 *               the timer
	 * @param tileSize  the size to draw each tile
	 * @return how long the test took, in ns.
	 */
	private static long third(final TileDrawHelper helper, final IMapNG map,
							final int reps, final int tileSize) {
		final BufferedImage image =
				new BufferedImage(tileSize, tileSize, BufferedImage.TYPE_INT_RGB);
		final long start = System.nanoTime();
		for (int rep = 0; rep < reps; rep++) {
			image.flush();
			thirdBody(helper,
					NullCleaner.assertNotNull(image.createGraphics()), map,
					tileSize);
		}
		final long end = System.nanoTime();
		return end - start;
	}

	/**
	 * The body of the third test.
	 *
	 * @param helper the helper being tested
	 * @param pen    the Graphics used to draw to the image
	 * @param map  the map being used for the test
	 * @param tileSize  the size to draw each tile
	 */
	private static void thirdBody(final TileDrawHelper helper,
								final Graphics pen, final IMapNG map,
								final int tileSize) {
		for (final Point point : map.locations()) {
			helper.drawTileTranslated(pen, map, point, tileSize, tileSize);
		}
	}

	/**
	 * Third test: translating, reusing Graphics.
	 *
	 * @param helper the helper to test
	 * @param map  the map being used for the test
	 * @param reps   the number of times to run this test between starting and stopping
	 *               the timer
	 * @param tileSize  the size to draw each tile
	 * @return how long the test took, in ns.
	 */
	private static long fourth(final TileDrawHelper helper, final IMapNG map,
							final int reps, final int tileSize) {
		final MapDimensions dim = map.dimensions();
		final BufferedImage image = new BufferedImage(tileSize * dim.cols, tileSize * dim.rows,
															BufferedImage.TYPE_INT_RGB);
		final long start = System.nanoTime();
		for (int rep = 0; rep < reps; rep++) {
			image.flush();
			fourthBody(helper,
					NullCleaner.assertNotNull(image.createGraphics()), map,
					tileSize);
		}
		final long end = System.nanoTime();
		return end - start;
	}

	/**
	 * The body of the fourth test.
	 *
	 * @param helper the helper being tested
	 * @param pen    the Graphics used to draw to the image
	 * @param map  the map being used for the test
	 * @param tileSize  the size to draw each tile
	 */
	private static void fourthBody(final TileDrawHelper helper,
								final Graphics pen, final IMapNG map,
								final int tileSize) {
		final Coordinate dimensions = PointFactory.coordinate(tileSize, tileSize);
		for (final Point point : map.locations()) {
			helper.drawTile(pen, map, point, PointFactory.coordinate(
					point.getRow() * tileSize, point.getCol() * tileSize), dimensions);
		}
	}

	/**
	 * Fifth test, part one: iterating.
	 *
	 * @param helper the helper to test
	 * @param map  the map being used for the test
	 * @param reps   the number of times to run this test between starting and stopping
	 *               the timer
	 * @param tileSize  the size to draw each tile
	 * @return how long the test took, in ns.
	 */
	private static long fifthOne(final TileDrawHelper helper, final IMapNG map,
								final int reps, final int tileSize) {
		final MapDimensions dim = map.dimensions();
		final BufferedImage image = new BufferedImage(tileSize * dim.cols, tileSize * dim.rows,
															BufferedImage.TYPE_INT_RGB);
		final long start = System.nanoTime();
		for (int rep = 0; rep < reps; rep++) {
			image.flush();
			fifthOneBody(map, helper,
					NullCleaner.assertNotNull(image.createGraphics()), tileSize);
		}
		final long end = System.nanoTime();
		return end - start;
	}

	/**
	 * The body of the first part of the fifth test.
	 *
	 * @param helper the helper being tested
	 * @param pen    the Graphics used to draw to the image
	 * @param map  the map being used for the test
	 * @param tileSize  the size to draw each tile
	 */
	private static void fifthOneBody(final IMapNG map,
									final TileDrawHelper helper, final Graphics pen,
									final int tileSize) {
		final Coordinate dimensions = PointFactory.coordinate(tileSize, tileSize);
		for (int row = TEST_MIN_ROW; row < TEST_MAX_ROW; row++) {
			for (int col = TEST_MIN_COL; col < TEST_MAX_COL; col++) {
				final Point point = PointFactory.point(row, col);
				helper.drawTile(pen, map, point,
						PointFactory.coordinate(row * tileSize, col * tileSize),
						dimensions);
			}
		}
	}

	/**
	 * Fifth test, part two: filtering.
	 *
	 * @param helper the helper to test
	 * @param map  the map being used for the test
	 * @param reps   the number of times to run this test between starting and stopping
	 *               the timer
	 * @param tileSize  the size to draw each tile
	 * @return how long the test took, in ns.
	 */
	private static long fifthTwo(final TileDrawHelper helper, final IMapNG map,
								final int reps, final int tileSize) {
		final MapDimensions dim = map.dimensions();
		final BufferedImage image = new BufferedImage(tileSize * dim.cols, tileSize * dim.rows,
															BufferedImage.TYPE_INT_RGB);
		final long start = System.nanoTime();
		for (int rep = 0; rep < reps; rep++) {
			image.flush();
			fifthTwoBody(helper,
					NullCleaner.assertNotNull(image.createGraphics()), map,
					tileSize);
		}
		final long end = System.nanoTime();
		return end - start;
	}

	/**
	 * The body of the first part of the fifth test.
	 *
	 * @param helper the helper being tested
	 * @param pen    the Graphics used to draw to the image
	 * @param map  the map being used for the test
	 * @param tileSize  the size to draw each tile
	 */
	private static void fifthTwoBody(final TileDrawHelper helper,
									final Graphics pen, final IMapNG map,
									final int tileSize) {
		final Coordinate dimensions = PointFactory.coordinate(tileSize, tileSize);
		for (final Point point : map.locations()) {
			if ((point.getRow() >= TEST_MIN_ROW) && (point.getRow() < TEST_MAX_ROW) &&
						(point.getCol() >= TEST_MIN_COL) &&
						(point.getCol() < TEST_MAX_COL)) {
				helper.drawTile(pen, map, point, PointFactory.coordinate(
						point.getRow() * tileSize, point.getCol() * tileSize),
						dimensions);
			}
		}
	}

	/**
	 * Run all the tests on the specified file.
	 *
	 * @param map         the map to use for the tests.
	 * @param repetitions how many times to repeat each test (more takes longer, but
	 *                       gives
	 *                    more precise result)
	 */
	private static void runAllTests(final IMapNG map, final int repetitions) {
		final int tileSize = TileViewSize.scaleZoom(ViewerModel.DEF_ZOOM_LEVEL,
				map.dimensions().version);
		final TileDrawHelper hThree =
				new Ver2TileDrawHelper((img, infoFlags, xCoordinate, yCoordinate, width,
										height) -> false,
											  fix -> true);
		SYS_OUT.println("1. All in one place:");
		final TileDrawHelper hOne = new CachingTileDrawHelper();
		long oneTotal =
				printStats(CACHING, first(hOne, map, repetitions, tileSize), repetitions);
		final TileDrawHelper hTwo = new DirectTileDrawHelper();
		long twoTotal =
				printStats(DIRECT, first(hTwo, map, repetitions, tileSize), repetitions);
		long threeTot = printStats(VER_TWO, first(hThree, map, repetitions, tileSize),
				repetitions);
		SYS_OUT.println("2. Translating:");
		oneTotal +=
				printStats(CACHING, second(hOne, map, repetitions, tileSize), repetitions);
		twoTotal +=
				printStats(DIRECT, second(hTwo, map, repetitions, tileSize), repetitions);
		threeTot +=
				printStats(VER_TWO, second(hThree, map, repetitions, tileSize),
						repetitions);
		SYS_OUT.println("3. In-place, reusing Graphics:");
		oneTotal +=
				printStats(CACHING, third(hOne, map, repetitions, tileSize), repetitions);
		twoTotal += printStats(DIRECT, third(hTwo, map, repetitions, tileSize),
				repetitions);
		threeTot +=
				printStats(VER_TWO, third(hThree, map, repetitions, tileSize), repetitions);
		SYS_OUT.println("4. Translating, reusing Graphics:");
		oneTotal +=
				printStats(CACHING, fourth(hOne, map, repetitions, tileSize), repetitions);
		twoTotal +=
				printStats(DIRECT, fourth(hTwo, map, repetitions, tileSize), repetitions);
		threeTot +=
				printStats(VER_TWO, fourth(hThree, map, repetitions, tileSize),
						repetitions);
		SYS_OUT.println("5. Ordered iteration vs filtering:");
		SYS_OUT.print("Iteration, ");
		oneTotal +=
				printStats(CACHING, fifthOne(hOne, map, repetitions, tileSize),
						repetitions);
		SYS_OUT.print("Iteration, ");
		twoTotal +=
				printStats(DIRECT, fifthOne(hTwo, map, repetitions, tileSize), repetitions);
		SYS_OUT.print("Iteration, ");
		threeTot += printStats(VER_TWO, fifthOne(hThree, map, repetitions, tileSize),
				repetitions);
		SYS_OUT.print("Filtering, ");
		oneTotal +=
				printStats(CACHING, fifthTwo(hOne, map, repetitions, tileSize),
						repetitions);
		SYS_OUT.print("Filtering, ");
		twoTotal +=
				printStats(DIRECT, fifthTwo(hTwo, map, repetitions, tileSize), repetitions);
		SYS_OUT.print("Filtering, ");
		threeTot += printStats(VER_TWO, fifthTwo(hThree, map, repetitions, tileSize),
				repetitions);
		SYS_OUT.println("--------------------------------------");
		SYS_OUT.print("Total:");
		printStats(CACHING, oneTotal, repetitions);
		printStats(DIRECT, twoTotal, repetitions);
		printStats(VER_TWO, threeTot, repetitions);
		SYS_OUT.println();
	}

	/**
	 * A helper method to reduce repeated strings.
	 *
	 * @param prefix what to print before the total
	 * @param total  the total time
	 * @param reps   how many times the test ran
	 * @return that total
	 */
	private static long printStats(final String prefix, final long total,
								final int reps) {
		SYS_OUT.print(prefix);
		SYS_OUT.print('\t');
		SYS_OUT.print(total);
		SYS_OUT.print(", average of\t");
		SYS_OUT.print(Long.toString(total / reps));
		SYS_OUT.println(" ns.");
		return total;
	}

	/**
	 * @return a String representation of this object
	 */
	@SuppressWarnings("MethodReturnAlwaysConstant")
	@Override
	public String toString() {
		return "DrawHelperComparator";
	}

	/**
	 * Start the driver.
	 *
	 * @param model the driver model to run on
	 */
	@Override
	public void startDriver(final IDriverModel model) {
		final Random random = new Random();
		final int reps = 50;
		if (model instanceof IMultiMapModel) {
			for (final Pair<IMutableMapNG, Optional<Path>> pair : ((IMultiMapModel) model)
																.getAllMaps()) {
				SYS_OUT.print("Testing using ");
				final Optional<Path> file = pair.second();
				if (file.isPresent()) {
					SYS_OUT.println(file.get());
				} else {
					SYS_OUT.println("a map not loaded from file");
				}
				final IMapNG map = pair.first();
				PointFactory.clearCache();
				if (random.nextBoolean()) {
					PointFactory.shouldUseCache(true);
					SYS_OUT.println("Using cache:");
					runAllTests(map, reps);
					PointFactory.shouldUseCache(false);
					SYS_OUT.println("Not using cache:");
					runAllTests(map, reps);
				} else {
					PointFactory.shouldUseCache(false);
					SYS_OUT.println("Not using cache:");
					runAllTests(map, reps);
					PointFactory.shouldUseCache(true);
					SYS_OUT.println("Using cache:");
					runAllTests(map, reps);
				}
			}
		} else {
			SYS_OUT.print("Testing using ");
			SYS_OUT.println(model.getMapFile());
			final IMapNG map = model.getMap();
			PointFactory.clearCache();
			if (random.nextBoolean()) {
				PointFactory.shouldUseCache(true);
				SYS_OUT.println("Using cache:");
				runAllTests(map, reps);
				PointFactory.shouldUseCache(false);
				SYS_OUT.println("Not using cache:");
				runAllTests(map, reps);
			} else {
				PointFactory.shouldUseCache(false);
				SYS_OUT.println("Not using cache:");
				runAllTests(map, reps);
				PointFactory.shouldUseCache(true);
				SYS_OUT.println("Using cache:");
				runAllTests(map, reps);
			}
		}
	}

	/**
	 * @return an object indicating how to use and invoke this driver.
	 */
	@Override
	public DriverUsage usage() {
		return USAGE;
	}
}
