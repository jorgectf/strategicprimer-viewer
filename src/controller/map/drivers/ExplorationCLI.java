package controller.map.drivers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import model.map.HasName;
import model.map.IMap;
import model.map.Player;
import model.map.Point;
import model.map.PointFactory;
import model.map.Tile;
import model.map.TileFixture;
import model.map.fixtures.Ground;
import model.map.fixtures.RiverFixture;
import model.map.fixtures.mobile.SimpleMovement;
import model.map.fixtures.mobile.SimpleMovement.TraversalImpossibleException;
import model.map.fixtures.mobile.Unit;
import model.map.fixtures.terrain.Forest;
import model.map.fixtures.terrain.Hill;
import model.map.fixtures.terrain.Mountain;
import model.map.fixtures.towns.Fortress;
import util.IsNumeric;
import util.Warning;
import view.util.SystemOut;
import controller.map.SPFormatException;
import controller.map.misc.MapReaderAdapter;

/**
 * A CLI to help running exploration. TODO: Some of this should be made more
 * usable from other UIs.
 *
 * @author Jonathan Lovelace
 *
 */
public class ExplorationCLI {
	/**
	 * Constructor.
	 *
	 * @param master The master map.
	 * @param secondaries Any player maps that should be updated with results of
	 *        exploration.
	 */
	public ExplorationCLI(final IMap master, final List<IMap> secondaries) {
		source = master;
		for (IMap map : secondaries) {
			if (map.getDimensions().equals(master.getDimensions())) {
				dests.add(map);
			} else {
				throw new IllegalArgumentException("Size mismatch");
			}
		}
	}
	/**
	 * The source map, which we'll get data from but only update to move the moving unit.
	 */
	private final IMap source;
	/**
	 * The destination maps, which will be updated with things a moving unit sees.
	 */
	private final List<IMap> dests = new ArrayList<IMap>();
	/**
	 * Given a list of maps, return a list (also a set, but we won't guarantee
	 * that) of players listed in all of them. TODO: move to a more general
	 * utility class?
	 * @param maps the maps to consider
	 * @return the list of players
	 */
	public static List<Player> getPlayerChoices(final List<IMap> maps) {
		if (maps.isEmpty()) {
			throw new IllegalArgumentException("Need at least one map.");
		}
		final List<Player> retval = new ArrayList<Player>();
		for (Player player : maps.get(0).getPlayers()) {
			retval.add(player);
		}
		final List<Player> temp = new ArrayList<Player>();
		for (IMap map : maps) {
			temp.clear();
			for (Player player : map.getPlayers()) {
				temp.add(player);
			}
			retval.retainAll(temp);
		}
		return retval;
	}
	/**
	 * @param map a map
	 * @param player a player
	 * @return a list of all units in the map belonging to that player.
	 */
	public static List<Unit> getUnits(final IMap map, final Player player) {
		final List<Unit> retval = new ArrayList<Unit>();
		for (final Point point : map.getTiles()) {
			final Tile tile = map.getTile(point);
			for (final TileFixture fix : tile) {
				if (fix instanceof Unit && ((Unit) fix).getOwner().equals(player)) {
					retval.add((Unit) fix);
				}
			}
		}
		return retval;
	}

	/**
	 * Find a fixture's location in the master map.
	 *
	 * @param fix the fixture to find.
	 * @return the first location found (search order is not defined) containing a
	 *         fixture "equal to" the specified one. (Using it on mountains,
	 *         e.g., will *not* do what you want ...)
	 */
	public Point find(final TileFixture fix) {
		for (Point point : source.getTiles()) {
			for (TileFixture item : source.getTile(point)) {
				if (fix.equals(item)) {
					return point; // NOPMD
				}
			}
		}
		return PointFactory.point(-1, -1);
	}

	/**
	 * Move a unit from the specified tile one tile in the specified direction.
	 * Moves the unit in all maps where the unit *was* in the specified tile,
	 * copying terrain information if the tile didn't exist in a subordinate
	 * map.
	 *
	 * @param unit the unit to move
	 * @param point the starting location
	 * @param direction the direction to move
	 * @return the movement cost
	 * @throws TraversalImpossibleException if movement in that direction is impossible
	 */
	public int move(final Unit unit, final Point point,
			final Direction direction) throws TraversalImpossibleException {
		final Point dest = getDestination(point, direction);
		final int retval = SimpleMovement.getMovementCost(source.getTile(dest));
		source.getTile(point).removeFixture(unit);
		source.getTile(dest).addFixture(unit);
		for (IMap map : dests) {
			final Tile stile = map.getTile(point);
			boolean hasUnit = false;
			for (final TileFixture fix : stile) {
				if (fix.equals(unit)) {
					hasUnit = true;
					break;
				}
			}
			if (!hasUnit) {
				continue;
			}
			Tile dtile = map.getTile(dest);
			if (dtile.isEmpty()) {
				dtile = new Tile(dest.row, dest.col, source.getTile(dest) // NOPMD
						.getTerrain());
				map.getTiles().addTile(dtile);
			}
			stile.removeFixture(unit);
			dtile.addFixture(unit);
		}
		return retval;
	}
	/**
	 * A "plus one" method with a configurable, low "overflow".
	 * @param num the number to increment
	 * @param max the maximum number we want to return
	 * @return either num + 1, if max or lower, or 0.
	 */
	public static int increment(final int num, final int max) {
		return num >= max - 1 ? 0 : num + 1;
	}
	/**
	 * A "minus one" method that "underflows" after 0 to a configurable, low value.
	 * @param num the number to decrement.
	 * @param max the number to "underflow" to.
	 * @return either num - 1, if 1 or higher, or max.
	 */
	public static int decrement(final int num, final int max) {
		return num == 0 ? max : num - 1;
	}
	/**
	 * @param point a point
	 * @param direction a direction
	 * @return the point one tile in that direction.
	 */
	private Point getDestination(final Point point, final Direction direction) {
		switch (direction) {
		case East:
			return PointFactory.point(point.row, // NOPMD
					increment(point.col, source.getDimensions().cols - 1));
		case North:
			return PointFactory.point(decrement(point.row, source.getDimensions().rows - 1), // NOPMD
					point.col);
		case Northeast:
			return PointFactory.point(decrement(point.row, source.getDimensions().rows - 1), // NOPMD
					increment(point.col, source.getDimensions().rows - 1));
		case Northwest:
			return PointFactory.point(decrement(point.row, source.getDimensions().rows - 1), // NOPMD
					decrement(point.col, source.getDimensions().cols - 1));
		case South:
			return PointFactory.point(increment(point.row, source.getDimensions().rows - 1), // NOPMD
					point.col);
		case Southeast:
			return PointFactory.point(increment(point.row, source.getDimensions().rows - 1), // NOPMD
					increment(point.col, source.getDimensions().cols - 1));
		case Southwest:
			return PointFactory.point(increment(point.row, source.getDimensions().rows - 1), // NOPMD
					decrement(point.col, source.getDimensions().cols - 1));
		case West:
			return PointFactory.point(point.row, // NOPMD
					decrement(point.col, source.getDimensions().cols - 1));
		default:
			throw new IllegalStateException("Unhandled case");
		}
	}
	/**
	 * Print a list of things by name and number.
	 * @param out the stream to write to
	 * @param list the list to print.
	 */
	private static void printList(final PrintStream out,
			final List<? extends HasName> list) {
		for (int i = 0; i < list.size(); i++) {
			out.print(i);
			out.print(": ");
			out.println(list.get(i).getName());
		}
	}
	/**
	 * Driver. Takes as its parameters the map files to use.
	 * @param args the command-line arguments
	 * @throws SPFormatException on SP format problems
	 * @throws XMLStreamException on malformed XML
	 * @throws IOException on basic file I/O error
	 */
	public static void main(final String[] args) throws IOException,
			XMLStreamException, SPFormatException {
		if (args.length == 0) {
			SystemOut.SYS_OUT.println("Usage: ExplorationCLI master-map [player-map ...]");
			System.exit(1);
		}
		final MapReaderAdapter reader = new MapReaderAdapter();
		final IMap master = reader.readMap(args[0], Warning.INSTANCE);
		final List<IMap> secondaries = new ArrayList<IMap>();
		final List<IMap> maps = new ArrayList<IMap>(
				Collections.singletonList(master));
		for (int i = 1; i < args.length; i++) {
			final IMap map = reader.readMap(args[i], Warning.INSTANCE);
			secondaries.add(map);
			maps.add(map);
		}
		final ExplorationCLI cli = new ExplorationCLI(master, secondaries);
		final List<Player> players = getPlayerChoices(maps);
		if (players.isEmpty()) {
			SystemOut.SYS_OUT.println("No players shared by all the maps.");
			return; // NOPMD
		}
		SystemOut.SYS_OUT.println("The players shared by all the maps:");
		printList(SystemOut.SYS_OUT, players);
		final Player player = players.get(inputNumber("Please make a selection: "));
		final List<Unit> units = getUnits(master, player);
		if (units.isEmpty()) {
			SystemOut.SYS_OUT.println("That player has no units in the master map.");
			return;
		}
		SystemOut.SYS_OUT.println("Player's units:");
		printList(SystemOut.SYS_OUT, units);
		final Unit unit = units.get(inputNumber("Please make a selection: "));
		SystemOut.SYS_OUT.println("Details of that unit:");
		SystemOut.SYS_OUT.println(unit.verbose());
		int movement = inputNumber("MP that unit has: ");
		final int totalMP = movement;
		int direction = -1;
		final List<TileFixture> allFixtures = new ArrayList<TileFixture>();
		// "contstants" is the fixtures that *always* get copied, such as
		// forests, mountains hills, and rivers.
		final List<TileFixture> constants = new ArrayList<TileFixture>();
		while (movement > 0) {
			SystemOut.SYS_OUT.print(movement);
			SystemOut.SYS_OUT.print(" MP of ");
			SystemOut.SYS_OUT.print(totalMP);
			SystemOut.SYS_OUT.println(" remaining.");
			SystemOut.SYS_OUT.print("0 = N, 1 = NE, 2 = E, 3 = SE, 4 = S, 5 = SW, ");
			SystemOut.SYS_OUT.println("6 = W, 7 = NW, 8 = Quit.");
			direction = inputNumber("Direction to move: ");
			if (direction > 7) {
				break;
			}
			final Point point = cli.find(unit);
			final Point dPoint = cli.getDestination(point, Direction.values()[direction]);
			// ESCA-JAVA0177:
			int cost;
			try {
				cost = cli.move(unit, point, Direction.values()[direction]);
			} catch (TraversalImpossibleException except) {
				movement--;
				for (IMap map : secondaries) {
					if (map.getTile(dPoint).isEmpty()) {
						map.getTiles().addTile(
								new Tile(dPoint.row, dPoint.col, master// NOPMD
										.getTile(dPoint).getTerrain()));
					}
				}
				SystemOut.SYS_OUT.println("That direction is impassable; we've made sure all maps show that at a cost of 1 MP");
				continue;
			}
			movement -= cost;
			allFixtures.clear();
			// "contstants" is the fixtures that *always* get copied, such as
			// forests, mountains hills, and rivers. Also fortresses, primarily
			// so we'll always see when we want to stop. FIXME: Find a better way
			// of doing that, so players can have a strategy based on secrecy.
			constants.clear();
			for (TileFixture fix : master.getTile(dPoint)) {
				if ((fix instanceof Ground && ((Ground) fix).isExposed())
						|| !(fix instanceof Ground)) {
					// FIXME: *Some* explorers would notice even unexposed ground.
					allFixtures.add(fix);
				}
				if (fix instanceof Mountain || fix instanceof RiverFixture
						|| fix instanceof Hill || fix instanceof Forest
						|| fix instanceof Fortress) {
					constants.add(fix);
				}
			}
			SystemOut.SYS_OUT.print("The explorer comes to ");
			SystemOut.SYS_OUT.print(dPoint.toString());
			SystemOut.SYS_OUT.print(", a tile with terrain ");
			SystemOut.SYS_OUT.println(master.getTile(dPoint).getTerrain());
			if (allFixtures.isEmpty()) {
				SystemOut.SYS_OUT.println("The following fixtures were automatically noticed:");
			} else {
				SystemOut.SYS_OUT.print("The following fixtures were noticed, all but the");
				SystemOut.SYS_OUT.println("first automtically:");
				Collections.shuffle(allFixtures);
				while (unit.equals(allFixtures.get(0))) {
					Collections.shuffle(allFixtures);
				}
				SystemOut.SYS_OUT.println(allFixtures.get(0));
			}
			for (TileFixture fix : constants) {
				SystemOut.SYS_OUT.println(fix);
			}
			for (IMap map : secondaries) {
				for (final TileFixture fix : constants) {
					map.getTile(dPoint).addFixture(fix);
				}
				if (!allFixtures.isEmpty()) {
					map.getTile(dPoint).addFixture(allFixtures.get(0));
				}
			}
		}
		for (int i = 0; i < args.length; i++) {
			reader.write(args[i], maps.get(i));
		}
	}
	/**
	 * Read input from stdin repeatedly until a nonnegative integer is entered, and return it.
	 * @param prompt The prompt to prompt the user with
	 * @return the number entered
	 * @throws IOException on I/O error
	 */
	public static int inputNumber(final String prompt) throws IOException {
		int retval = -1;
		final BufferedReader istream = new BufferedReader(new InputStreamReader(System.in));
		while (retval < 0) {
			SystemOut.SYS_OUT.print(prompt);
			final String input = istream.readLine();
			if (IsNumeric.isNumeric(input)) {
				retval = Integer.parseInt(input);
			}
		}
		return retval;
	}
	/**
	 * An enumeration of directions.
	 */
	public enum Direction {
		/**
		 * North.
		 */
		North,
		/**
		 * Northeast.
		 */
		Northeast,
		/**
		 * East.
		 */
		East,
		/**
		 * Southeast.
		 */
		Southeast,
		/**
		 * South.
		 */
		South,
		/**
		 * Southwest.
		 */
		Southwest,
		/**
		 * West.
		 */
		West,
		/**
		 * Northwest.
		 */
		Northwest;
	}
}
