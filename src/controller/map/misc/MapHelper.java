package controller.map.misc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import model.map.HasName;
import util.IsNumeric;
import view.util.SystemOut;

/**
 * A helper class to let drivers, especially CLI drivers, interact with a map.
 *
 * FIXME: Better name
 *
 * @author Jonathan Lovelace
 *
 */
public class MapHelper {
	/**
	 * The input stream we'll read from.
	 */
	private final BufferedReader istream;
	/**
	 * No-arg constructor.
	 */
	public MapHelper() {
		this(System.in);
	}
	/**
	 * Constructor.
	 * @param in the stream to read from.
	 */
	public MapHelper(final InputStream in) { // NOPMD
		istream = new BufferedReader(new InputStreamReader(in));
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
	 * Have the user choose an item from a list.
	 * @param <T> The type of things in the list
	 * @param items the list of items
	 * @param desc the description to give before printing the list
	 * @param none what to print if there are none
	 * @param prompt what to prompt the user with
	 * @param auto whether to automatically choose if there's only one choice
	 * @return the user's selection, or -1 if there are none
	 * @throws IOException on I/O error getting the user's input
	 */
	public <T extends HasName> int chooseFromList(
			final List<? extends T> items, final String desc,
			final String none, final String prompt, final boolean auto) throws IOException {
		if (items.isEmpty()) {
			SystemOut.SYS_OUT.println(none);
			return -1; // NOPMD
		}
		SystemOut.SYS_OUT.println(desc);
		if (auto && items.size() == 1) {
			SystemOut.SYS_OUT.print("Automatically choosing only item, ");
			SystemOut.SYS_OUT.println(items.get(0));
			return 0; // NOPMD
		} else {
			printList(SystemOut.SYS_OUT, items);
			return inputNumber(prompt);
		}
	}

	/**
	 * Turn an Iterable into a List. This is, of course, an eager
	 * implementation; make sure not to use on anything with an infinite
	 * iterator!
	 *
	 * FIXME: This is probably more generally useful and should be moved
	 * elsewhere, if it's not already somewhere I forgot about.
	 *
	 * @param <T> the type contained in the iterable.
	 * @param iter the thing to iterate over
	 * @return a List representing the same data.
	 */
	public <T> List<T> toList(final Iterable<T> iter) {
		final List<T> retval = new ArrayList<T>();
		for (final T item : iter) {
			retval.add(item);
		}
		return retval;
	}

	/**
	 * Read input from stdin repeatedly until a nonnegative integer is entered, and return it.
	 * @param prompt The prompt to prompt the user with
	 * @return the number entered
	 * @throws IOException on I/O error
	 */
	public int inputNumber(final String prompt) throws IOException {
		int retval = -1;
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
	 * Read input from stdin. (The input is trimmed of leading and trailing whitespace.)
	 * @param prompt The prompt to prompt the user with
	 * @return the string entered.
	 * @throws IOException on I/O error
	 */
	public String inputString(final String prompt) throws IOException {
		SystemOut.SYS_OUT.print(prompt);
		final String retval = istream.readLine();
		return retval == null ? "" : retval.trim();
	}
}
