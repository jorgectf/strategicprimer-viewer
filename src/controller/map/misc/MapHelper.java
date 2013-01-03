package controller.map.misc;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import util.Warning;
import view.util.SystemOut;
import controller.map.SPFormatException;
import controller.map.drivers.ExplorationCLI;

import model.map.HasName;
import model.map.IMap;
import model.map.Player;
import model.map.Point;
import model.map.Tile;
import model.map.TileFixture;
import model.map.fixtures.mobile.Unit;

/**
 * A helper class to let drivers, especially CLI drivers, interact with a map.
 *
 * FIXME: Better name
 *
 * FIXME: Replace static methods with either a singleton object or one instance
 * per driver instance.
 *
 * @author Jonathan Lovelace
 *
 */
public class MapHelper {

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
	 * Read maps.
	 * @param filenames the files to read from
	 * @param maps a list to put all of them in
	 * @param secondaries a list to put all but the first in
	 * @return the first map
	 * @throws SPFormatException on SP format problems
	 * @throws XMLStreamException on malformed XML
	 * @throws IOException on basic file I/O error
	 */
	public static IMap readMaps(final String[] filenames,
			final List<IMap> maps, final List<IMap> secondaries)
			throws IOException, XMLStreamException, SPFormatException {
		final MapReaderAdapter reader = new MapReaderAdapter();
		final IMap master = reader.readMap(filenames[0], Warning.INSTANCE);
		maps.add(master);
		for (int i = 1; i < filenames.length; i++) {
			final IMap map = reader.readMap(filenames[i], Warning.INSTANCE);
			secondaries.add(map);
			maps.add(map);
		}
		return master;
	}

	/**
	 * Write maps to disk.
	 * @param maps the list of maps to write
	 * @param filenames the list of files to write them to
	 * @throws IOException on I/O error
	 */
	public static void writeMaps(final List<IMap> maps,
			final String[] filenames) throws IOException {
		final MapReaderAdapter reader = new MapReaderAdapter();
		for (int i = 0; i < filenames.length; i++) {
			reader.write(filenames[i], maps.get(i));
		}
	}

	/**
	 * Have the user choose an item from a list.
	 * @param <T> The type of things in the list
	 * @param items the list of items
	 * @param desc the description to give before printing the list
	 * @param none what to print if there are none
	 * @param prompt what to prompt the user with
	 * @return the user's selection, or -1 if there are none
	 * @throws IOException on I/O error getting the user's input
	 */
	public static <T extends HasName> int chooseFromList(
			final List<? extends T> items, final String desc,
			final String none, final String prompt) throws IOException {
		if (items.isEmpty()) {
			SystemOut.SYS_OUT.println(none);
			return -1; // NOPMD
		}
		SystemOut.SYS_OUT.println(desc);
		printList(SystemOut.SYS_OUT, items);
		return ExplorationCLI.inputNumber(prompt);
	}

}
