package controller.map.misc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import model.map.HasName;
import org.eclipse.jdt.annotation.NonNull;
import util.EqualsAny;
import util.IsNumeric;
import util.NullCleaner;

/**
 * A helper class to let help CLIs interact with the user.
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
public final class CLIHelper implements ICLIHelper {
	/**
	 * The input stream we'll read from.
	 */
	private final BufferedReader istream;
	/**
	 * The output stream we'll write to.
	 */
	private final PrintWriter ostream;
	/**
	 * A parser for numbers.
	 */
	private static final NumberFormat NUM_PARSER =
			NullCleaner.assertNotNull(NumberFormat.getIntegerInstance());

	/**
	 * No-arg constructor.
	 */
	@SuppressWarnings("null") // System.in cannot be null
	public CLIHelper() {
		this(new InputStreamReader(System.in), new OutputStreamWriter(System.out));
	}

	/**
	 * Constructor.
	 *
	 * @param in the stream to read from.
	 * @param out the writer to write to
	 */
	public CLIHelper(final Reader in, final Writer out) { // NOPMD
		istream = new BufferedReader(in);
		ostream = new PrintWriter(out);
	}

	/**
	 * Print a list of things by name and number.
	 *
	 * @param list    the list to print.
	 */
	private void printList(final List<? extends HasName> list) {
		for (int i = 0; i < list.size(); i++) {
			ostream.append(Integer.toString(i));
			ostream.append(": ");
			ostream.println(list.get(i).getName());
		}
		ostream.flush();
	}

	/**
	 * Have the user choose an item from a list.
	 *
	 * @param <T>    The type of things in the list
	 * @param items  the list of items
	 * @param desc   the description to give before printing the list
	 * @param none   what to print if there are none
	 * @param prompt what to prompt the user with
	 * @param auto   whether to automatically choose if there's only one choice
	 * @return the user's selection, or -1 if there are none
	 * @throws IOException on I/O error getting the user's input
	 */
	@Override
	public <T extends HasName> int chooseFromList(final List<@NonNull ? extends T> items,
	                                              final String desc, final String none,
	                                              final String prompt, final boolean auto)
			throws IOException {
		if (items.isEmpty()) {
			ostream.println(none);
			ostream.flush();
			return -1; // NOPMD
		}
		ostream.println(desc);
		if (auto && (items.size() == 1)) {
			ostream.print("Automatically choosing only item, ");
			ostream.println(items.get(0));
			ostream.flush();
			return 0; // NOPMD
		} else {
			printList(items);
			return inputNumber(prompt);
		}
	}

	/**
	 * Turn an Iterable into a List. This is, of course, an eager implementation; make
	 * sure not to use on anything with an infinite iterator!
	 *
	 * FIXME: This is probably more generally useful and should be moved elsewhere, if
	 * it's not already somewhere I forgot about.
	 *
	 * @param <T>  the type contained in the iterable.
	 * @param iter the thing to iterate over
	 * @return a List representing the same data.
	 */
	public static <T> List<T> toList(final Iterable<T> iter) {
		return StreamSupport.stream(iter.spliterator(), false)
				       .collect(Collectors.toList());
	}

	/**
	 * Read input from stdin repeatedly until a nonnegative integer is entered, and
	 * return
	 * it.
	 *
	 * @param prompt The prompt to prompt the user with
	 * @return the number entered
	 * @throws IOException on I/O error
	 */
	@Override
	public int inputNumber(final String prompt) throws IOException {
		int retval = -1;
		while (retval < 0) {
			ostream.print(prompt);
			ostream.flush();
			final String input = istream.readLine();
			if (input == null) {
				throw new IOException("Null line of input");
			} else if (IsNumeric.isNumeric(input)) {
				try {
					retval = NUM_PARSER.parse(input).intValue();
				} catch (final ParseException e) {
					final NumberFormatException nexcept =
							new NumberFormatException("Failed to parse number from input");
					nexcept.initCause(e);
					throw nexcept;
				}
			}
		}
		return retval;
	}

	/**
	 * Read input from stdin. (The input is trimmed of leading and trailing whitespace.)
	 *
	 * @param prompt The prompt to prompt the user with
	 * @return the string entered.
	 * @throws IOException on I/O error
	 */
	@Override
	public String inputString(final String prompt) throws IOException {
		ostream.print(prompt);
		ostream.flush();
		final String line = istream.readLine();
		if (line == null) {
			return ""; // NOPMD
		} else {
			return NullCleaner.assertNotNull(line.trim());
		}
	}
	/**
	 * @param str a string
	 * @return its lower case equivalent
	 */
	private static String lower(final String str) {
		return NullCleaner.assertNotNull(str.toLowerCase(Locale.US));
	}
	/**
	 * Ask the user a yes-or-no question.
	 *
	 * @param prompt the string to prompt the user with
	 * @return true if yes, false if no
	 * @throws IOException on I/O error
	 */
	@Override
	public boolean inputBoolean(final String prompt) throws IOException {
		//noinspection ForLoopWithMissingComponent
		for (String input = lower(inputString(prompt)); ;
				input = lower(inputString(prompt))) {
			if (EqualsAny.equalsAny(input, "yes", "true", "y", "t")) {
				return true; // NOPMD
			} else if (EqualsAny.equalsAny(input, "no", "false", "n", "f")) {
				return false;
			} else {
				ostream.println("Please enter 'yes', 'no', 'true', or 'false',");
				ostream.println("or the first character of any of those.");
			}
		}
	}

	/**
	 * @return a String representation of the object
	 */
	@SuppressWarnings("MethodReturnAlwaysConstant")
	@Override
	public String toString() {
		return "CLIHelper";
	}

	/**
	 * Print a list of things by name and number.
	 *
	 * @param list    the list to print.
	 */
	private void printStringList(final List<String> list) {
		for (int i = 0; i < list.size(); i++) {
			ostream.append(Integer.toString(i));
			ostream.append(": ");
			ostream.println(list.get(i));
		}
		ostream.flush();
	}

	/**
	 * Have the user choose an item from a list.
	 *
	 * @param items  the list of items
	 * @param desc   the description to give before printing the list
	 * @param none   what to print if there are none
	 * @param prompt what to prompt the user with
	 * @param auto   whether to automatically choose if there's only one choice
	 * @return the user's selection, or -1 if there are none
	 * @throws IOException on I/O error getting the user's input
	 */
	@Override
	public int chooseStringFromList(final List<String> items, final String desc,
	                                final String none, final String prompt,
	                                final boolean auto)
			throws IOException {
		if (items.isEmpty()) {
			ostream.println(none);
			ostream.flush();
			return -1; // NOPMD
		}
		ostream.println(desc);
		if (auto && (items.size() == 1)) {
			ostream.print("Automatically choosing only item, ");
			ostream.println(items.get(0));
			ostream.flush();
			return 0; // NOPMD
		} else {
			printStringList(items);
			return inputNumber(prompt);
		}
	}
	/**
	 * Print a formatted string.
	 * @param format the format string
	 * @param args the arguments to fill into the format string.
	 */
	@Override
	public void printf(final String format, final Object ... args) {
		ostream.printf(format, args);
		ostream.flush();
	}
	/**
	 * Print the specified string, then a newline.
	 * @param line the line to print
	 */
	@Override
	public void println(final String line) {
		ostream.println(line);
		ostream.flush();
	}
	/**
	 * Print the specified string.
	 * @param text the string to print
	 */
	@Override
	public void print(final String text) {
		ostream.print(text);
		ostream.flush();
	}

	/**
	 * Close I/O streams.
	 */
	@Override
	public void close() throws IOException {
		istream.close();
		ostream.close();
	}
}
