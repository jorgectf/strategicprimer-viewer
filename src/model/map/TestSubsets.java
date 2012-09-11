package model.map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.PrintStream;

import model.map.fixtures.RiverFixture;
import model.map.fixtures.TextFixture;
import model.map.fixtures.mobile.Animal;
import model.map.fixtures.mobile.Unit;
import model.map.fixtures.resources.CacheFixture;
import model.map.fixtures.terrain.Mountain;
import model.map.fixtures.towns.Fortress;

import org.junit.Test;

import view.util.NullStream;
/**
 * Tests for Subsettable functionality.
 * @author Jonathan Lovelace
 *
 */
public class TestSubsets {
	/**
	 * A fake filename to use with all objects that require a filename.
	 */
	private static final String FAKE_FILENAME = "string";
	/**
	 * A bit-bucket to send subset output to.
	 */
	private static final PrintStream BIT_BUCKET = new PrintStream(new NullStream());
	/**
	 * A test of PlayerCollection's subset feature.
	 */
	@Test
	public void testPlayerCollectionSubset() {
		final PlayerCollection zero = new PlayerCollection();
		final PlayerCollection one = new PlayerCollection();
		final PlayerCollection two = new PlayerCollection();
		one.addPlayer(new Player(1, "one", FAKE_FILENAME));
		two.addPlayer(new Player(1, "one", FAKE_FILENAME));
		two.addPlayer(new Player(2, "two", FAKE_FILENAME));
		assertTrue("Empty is subset of self", zero.isSubset(zero, BIT_BUCKET));
		assertTrue("Empty is subset of one", one.isSubset(zero, BIT_BUCKET));
		assertTrue("Empty is subset of two", two.isSubset(zero, BIT_BUCKET));
		assertFalse("One is not subset of empty", zero.isSubset(one, BIT_BUCKET));
		assertTrue("One is subset of self", one.isSubset(one, BIT_BUCKET));
		assertTrue("One is subset of two", two.isSubset(one, BIT_BUCKET));
		assertFalse("Two is not subset of empty", zero.isSubset(two, BIT_BUCKET));
		assertFalse("Two is not subset of one", one.isSubset(two, BIT_BUCKET));
		assertTrue("Two is subset of self", two.isSubset(two, BIT_BUCKET));
	}
	/**
	 * A test of RiverFixture's subset feature.
	 */
	@Test
	public void testRiverSubset() {
		final RiverFixture zero = new RiverFixture();
		final RiverFixture one = new RiverFixture(River.Lake, River.South, River.East);
		final RiverFixture two = new RiverFixture(River.West, River.North);
		final RiverFixture three = new RiverFixture(River.Lake, River.South, River.East, River.North, River.West);
		assertTrue("None is a subset of all", three.isSubset(zero, BIT_BUCKET));
		assertTrue("Three are a subset of all", three.isSubset(one, BIT_BUCKET));
		assertTrue("Two are a subset of all", three.isSubset(two, BIT_BUCKET));
		assertTrue("All is a subset of all", three.isSubset(three, BIT_BUCKET));
		assertTrue("None is a subset of two", two.isSubset(zero, BIT_BUCKET));
		assertFalse("All is not a subset of two", two.isSubset(three, BIT_BUCKET));
		assertFalse("Three are not a subset of two", two.isSubset(one, BIT_BUCKET));
		assertTrue("Two are a subset of themselves", two.isSubset(two, BIT_BUCKET));
		assertTrue("None is a subset of three", one.isSubset(zero, BIT_BUCKET));
		assertFalse("All is not a subset of three", one.isSubset(three, BIT_BUCKET));
		assertFalse("A different two are not a subset of three", one.isSubset(two, BIT_BUCKET));
		assertTrue("Three are a subset of themselves", one.isSubset(one, BIT_BUCKET));
		assertTrue("None is a subset of itself", zero.isSubset(zero, BIT_BUCKET));
		assertFalse("All are not a subset of none", zero.isSubset(three, BIT_BUCKET));
		assertFalse("Three are not a subset of none", zero.isSubset(one, BIT_BUCKET));
		assertFalse("Two are not a subset of none", zero.isSubset(two, BIT_BUCKET));
	}
	/**
	 * A test of Fortress's subset feature.
	 */
	@Test
	public void testFortressSubset() {
		final Fortress one = new Fortress(new Player(1, "one", FAKE_FILENAME), "fOne", 1, FAKE_FILENAME);
		final Fortress two = new Fortress(new Player(2, "two", FAKE_FILENAME), "fOne", 1, FAKE_FILENAME);
		assertFalse("Subset requires same owner, first test", one.isSubset(two, BIT_BUCKET));
		assertFalse("Subset requires same owner, second test", two.isSubset(one, BIT_BUCKET));
		final Fortress three = new Fortress(new Player(1, "one", FAKE_FILENAME), "fTwo", 2, FAKE_FILENAME);
		assertFalse("Subset requires same name, first test", one.isSubset(three, BIT_BUCKET));
		assertFalse("Subset requires same name, second test", three.isSubset(one, BIT_BUCKET));
		final Fortress four = new Fortress(new Player(1, "one", FAKE_FILENAME), "fOne", 3, FAKE_FILENAME);
		assertTrue("Subset doesn't require identiy or ID equality", one.isSubset(four, BIT_BUCKET));
		assertTrue("Subset doesn't require identiy or ID equality", four.isSubset(one, BIT_BUCKET));
		four.addUnit(new Unit(new Player(2, "two", FAKE_FILENAME), "unit_type", "unit_name", 4, FAKE_FILENAME));
		assertTrue("Fortress without is a subset of fortress with unit", four.isSubset(one, BIT_BUCKET));
		assertFalse("Fortress with is not a subset of fortress without unit", one.isSubset(four, BIT_BUCKET));
	}
	/**
	 * A test of Tile's subset feature.
	 */
	@Test
	public void testTileSubset() {
		final Tile one = new Tile(0, 0, TileType.Steppe, FAKE_FILENAME);
		final Tile two = new Tile(0, 1, TileType.Steppe, FAKE_FILENAME);
		assertFalse("Subset tile must match in location", one.isSubset(two, BIT_BUCKET));
		assertFalse("Subset tile must match in location", two.isSubset(one, BIT_BUCKET));
		final Tile three = new Tile(0, 0, TileType.Desert, FAKE_FILENAME);
		assertFalse("Subset tile must match in type", one.isSubset(three, BIT_BUCKET));
		assertFalse("Subset tile must match in type", three.isSubset(one, BIT_BUCKET));
		final Tile four = new Tile(0, 0, TileType.Steppe, FAKE_FILENAME);
		assertTrue("Tile is subset of self", one.isSubset(one, BIT_BUCKET));
		assertTrue("Tile is subset of equal tile", one.isSubset(four, BIT_BUCKET));
		assertTrue("Tile is subset of equal tile", four.isSubset(one, BIT_BUCKET));
		four.addFixture(new Mountain(FAKE_FILENAME));
		assertTrue("Tile with fixture is subset of tile without", four.isSubset(one, BIT_BUCKET));
		assertFalse("Tile without fixture is not subset of tile with", one.isSubset(four, BIT_BUCKET));
		one.addFixture(new Mountain(FAKE_FILENAME));
		assertTrue("Adding equal fixture makes it again a subset", one.isSubset(four, BIT_BUCKET));
		four.addFixture(new CacheFixture("category", "contents", 1, FAKE_FILENAME));
		assertTrue("Subset calculation skips Caches", one.isSubset(four, BIT_BUCKET));
		four.addFixture(new TextFixture("text", -1));
		assertTrue("Subset calculation skips arbitrary-text", one.isSubset(four, BIT_BUCKET));
		four.addFixture(new Animal("animal", true, false, 2, FAKE_FILENAME));
		assertTrue("Subset calculation skips animal tracks ...", one.isSubset(four, BIT_BUCKET));
		four.addFixture(new Animal("animal", false, false, 3, FAKE_FILENAME));
		assertFalse("But not the animals themselves", one.isSubset(four, BIT_BUCKET));
	}
	/**
	 * Test the TileCollection subset feature.
	 */
	@Test
	public void testTileCollectionSubset() {
		final TileCollection zero = new TileCollection(FAKE_FILENAME);
		final TileCollection one = new TileCollection(FAKE_FILENAME);
		one.addTile(new Tile(0, 0, TileType.Jungle, FAKE_FILENAME));
		final TileCollection two = new TileCollection(FAKE_FILENAME);
		two.addTile(new Tile(0, 0, TileType.Jungle, FAKE_FILENAME));
		two.addTile(new Tile(1, 1, TileType.Ocean, FAKE_FILENAME));
		assertTrue("None is a subset of itself", zero.isSubset(zero, BIT_BUCKET));
		assertTrue("None is a subset of one", one.isSubset(zero, BIT_BUCKET));
		assertTrue("None is a subset of two", two.isSubset(zero, BIT_BUCKET));
		assertFalse("One is not a subset of none", zero.isSubset(one, BIT_BUCKET));
		assertTrue("One is a subset of itself", one.isSubset(one, BIT_BUCKET));
		assertTrue("One is a subset of two", two.isSubset(one, BIT_BUCKET));
		assertFalse("TWo is not a subset of none", zero.isSubset(two, BIT_BUCKET));
		assertFalse("Two is not a subset of one", one.isSubset(two, BIT_BUCKET));
		assertTrue("Two is a subset of itself", two.isSubset(two, BIT_BUCKET));
		one.addTile(new Tile(1, 1, TileType.Plains, FAKE_FILENAME));
		assertFalse("Corresponding but non-matching tile breaks subset", two.isSubset(one, BIT_BUCKET));
		assertFalse("Corresponding but non-matching tile breaks subset", one.isSubset(two, BIT_BUCKET));
	}
}
