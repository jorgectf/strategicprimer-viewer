package controller.map.misc;

import java.io.File;

import model.map.FixtureIterable;
import model.map.IFixture;
import model.map.IMapNG;
import model.map.IMutableMapNG;
import model.map.Point;
import model.misc.IMultiMapModel;
import util.Pair;

/**
 * A class to create an IDFactory with all IDs in a map, or in a collection of
 * fixtures, already registered as used.
 *
 * @author Jonathan Lovelace
 */
public final class IDFactoryFiller {
	/**
	 * Don't instantiate.
	 */
	private IDFactoryFiller() {
		// Only static methods.
	}

	/**
	 * @param map a map
	 * @return an ID factory that won't generate an ID the map already uses
	 */
	public static IDFactory createFactory(final IMapNG map) {
		final IDFactory retval = new IDFactory();
		for (final Point point : map.locations()) {
			if (point == null) {
				continue;
			}
			// Ground, Forest, Rivers, and Mountains do not have IDs, so we
			// can skip them and just test the "other" fixtures
			for (IFixture fixture : map.getOtherFixtures(point)) {
				final int idNum = fixture.getID();
				if (!retval.used(idNum)) {
					// We don't want to set off duplicate-ID warnings for
					// the same fixture in multiple maps.
					retval.register(idNum);
				}
				if (fixture instanceof FixtureIterable<?>) {
					recursiveRegister(retval, (FixtureIterable<?>) fixture);
				}
			}
		}
		return retval;
	}

	/**
	 * @param model a collection of maps
	 * @return an ID factory that won't generate an ID any of the maps already
	 *         uses.
	 */
	public static IDFactory createFactory(final IMultiMapModel model) {
		final IDFactory retval = new IDFactory();
		for (final Pair<IMutableMapNG, File> pair : model.getAllMaps()) {
			for (final Point point : pair.first().locations()) {
				if (point == null) {
					continue;
				}
				// Ground, Forest, Rivers, and Mountains do not have IDs, so we
				// can skip them and just test the "other" fixtures
				for (IFixture fixture : pair.first().getOtherFixtures(point)) {
					final int idNum = fixture.getID();
					if (!retval.used(idNum)) {
						// We don't want to set off duplicate-ID warnings for
						// the same fixture in multiple maps.
						retval.register(idNum);
					}
					if (fixture instanceof FixtureIterable<?>) {
						recursiveRegister(retval, (FixtureIterable<?>) fixture);
					}
				}
			}
		}
		return retval;
	}

	/**
	 * @param iter a collection of fixtures
	 * @return an ID factory that won't generate an ID already used in the
	 *         collection
	 */
	public static IDFactory createFactory(final FixtureIterable<?> iter) {
		final IDFactory retval = new IDFactory();
		recursiveRegister(retval, iter);
		return retval;
	}

	/**
	 * @param idf an IDFactory instance
	 * @param iter a collection of fixtures, all of which (recursively) should
	 *        have their IDs marked as used.
	 */
	private static void recursiveRegister(final IDFactory idf,
			final FixtureIterable<?> iter) {
		for (final IFixture fix : iter) {
			final int idNum = fix.getID();
			if (!idf.used(idNum)) {
				// We don't want to set off duplicate-ID warnings for the same
				// fixture in multiple maps. Or for Mountains and the like.
				idf.register(idNum);
			}
			if (fix instanceof FixtureIterable<?>) {
				recursiveRegister(idf, (FixtureIterable<?>) fix);
			}
		}
	}
}
