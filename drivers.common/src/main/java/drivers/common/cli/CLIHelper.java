package drivers.common.cli;

import org.jetbrains.annotations.Nullable;
import org.javatuples.Pair;

import java.math.BigDecimal;
import java.util.Map;
import java.util.HashMap;
import common.map.HasName;
import static lovelace.util.NumParsingHelper.isNumeric;
import static lovelace.util.NumParsingHelper.parseInt;
import lovelace.util.SystemIn;
import java.util.List;
import java.util.function.Function;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.stream.Stream;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * A helper class to help command-line apps interact with the user,
 * encapsulating input and output streams.
 *
 * TODO: Port to use java.io.Console more directly and extensively?
 */
public final class CLIHelper implements ICLIHelper {
	private static final Logger LOGGER = Logger.getLogger(CLIHelper.class.getName());
	@FunctionalInterface
	public static interface IOSource {
		@Nullable String readLine() throws IOException;
	}

	@FunctionalInterface
	public static interface IOSink {
		void write(String string) throws IOException;
	}

	public CLIHelper(final IOSource istream, final IOSink ostream) {
		this.istream = istream;
		this.ostream = ostream;
	}

	public CLIHelper() {
		this(System.console()::readLine, System.out::print);
	}

	/**
	 * A way to read a line at a time, presumably from the user.
	 */
	private final IOSource istream;

	/**
	 * A consumer of output, presumably sending it to the user.
	 */
	private final IOSink ostream;

	/**
	 * The current state of the yes-to-all/no-to-all possibility. Absent if
	 * not set, present if set, and the boolean value is what to return.
	 */
	private final Map<String, Boolean> seriesState = new HashMap<>();

	private final Map<String, Long> intervals = new HashMap<>();

	/**
	 * Print the specified string.
	 */
	@Override
	public void print(final String... text) {
		long newlines = Stream.of(text)
			.mapToLong(s -> s.chars().filter(c -> c == '\n' || c == '\r').count()).sum();
		if (newlines > 0) {
			intervals.replaceAll((key, lines) -> lines + newlines);
		}
		for (String part : text) {
			try {
				ostream.write(part);
			} catch (final IOException except) {
				LOGGER.log(Level.WARNING, "I/O error", except);
				return;
			}
		}
	}

	/**
	 * Print the specified string, then a newline.
	 */
	@Override
	public void println(final String line) {
		print(line);
		print(System.lineSeparator());
	}

	/**
	 * Print a prompt, adding whitespace if the prompt didn't end with it.
	 */
	private void writePrompt(final String prompt) {
		print(prompt);
		if (!prompt.isEmpty() && !Character.isWhitespace(prompt.charAt(prompt.length() - 1))) {
			print(" ");
		}
	}

	/**
	 * Ask the user a yes-or-no question. Returns null on EOF.
	 */
	@Override
	public Boolean inputBoolean(final String prompt, final TrinaryPredicate<String> quitResultFactory) {
		while (true) {
			String input = Optional.ofNullable(inputString(prompt))
				.map(String::toLowerCase).orElse(null);
			if (input == null || quitResultFactory.test(input) == null) {
				return null;
			} else if ("yes".equals(input) || "true".equals(input) ||
					"y".equals(input) || "t".equals(input)) {
				return true;
			} else if ("no".equals(input) || "false".equals(input) ||
					"n".equals(input) || "f".equals(input)) {
				return false;
			} else {
				println("Please enter \"yes\", \"no\", \"true\", or \"false\",");
				println("or the first character of any of those.");
			}
		}
	}

	/**
	 * Print a list of things by name and number.
	 *
	 * TODO: Take Iterable instead of List?
	 */
	private <Element> void printList(final List<? extends Element> list, final Function<Element, String> func) {
		int index = 0;
		for (Element item : list) {
			println(String.format("%d: %s", index, func.apply(item)));
			index++;
		}
	}

	/**
	 * Implementation of {@link chooseFromList} and {@link chooseStringFromList}.
	 */
	private <Element> Pair<Integer, @Nullable Element> chooseFromListImpl(final List<? extends Element> items,
	                                                                      final String description, final String none, final String prompt, final boolean auto,
	                                                                      final Function<? super Element, String> func) {
		if (items.isEmpty()) {
			println(none);
			return Pair.with(-1, null);
		}
		println(description);
		if (auto && items.size() == 1) {
			Element first = items.get(0);
			println(String.format("Automatically choosing only item, %s.", func.apply(first)));
			return Pair.with(0, first);
		} else {
			printList(items, func);
			Integer retval = inputNumber(prompt);
			if (retval == null) {
				return Pair.with(-2, null);
			} else if (retval < 0 || retval >= items.size()) {
				return Pair.with(retval, null);
			} else {
				return Pair.with(retval, items.get(retval));
			}
		}
	}

	/**
	 * Have the user choose an item from a list.
	 */
	@Override
	public <Element extends HasName> Pair<Integer, @Nullable Element> chooseFromList(
			final List<? extends Element> list, final String description, final String none, final String prompt,
			final boolean auto) {
		return chooseFromListImpl(list, description, none, prompt, auto, HasName::getName);
	}

	/**
	 * Read an input line, logging any exceptions but returning null on I/O exception.
	 */
	private @Nullable String readLine() {
		try {
			return istream.readLine();
		} catch (final IOException except) {
			LOGGER.log(Level.WARNING, "I/O error", except);
			return null;
		}
	}

	/**
	 * Read input from the input stream repeatedly until a non-negative
	 * integer is entered, then return it. Returns null on EOF.
	 */
	@Override
	public @Nullable Integer inputNumber(final String prompt) {
		int retval = -1;
		while (retval < 0) {
			writePrompt(prompt);
			String input = readLine();
			if (input == null) {
				return null;
			}
			if (isNumeric(input)) {
				OptionalInt temp = parseInt(input);
				if (temp.isPresent()) {
					retval = temp.getAsInt();
				}
			}
		}
		return retval;
	}

	/**
	 * Read from the input stream repeatedly until a valid non-negative
	 * decimal number is entered, then return it. Returns null on EOF.
	 */
	@Override
	public @Nullable BigDecimal inputDecimal(final String prompt) {
		final BigDecimal zero = BigDecimal.ZERO;
		BigDecimal retval = zero.subtract(BigDecimal.ONE);
		while (retval.compareTo(zero) < 0) {
			writePrompt(prompt);
			String input = readLine();
			if (input == null) {
				return null;
			}
			try {
				retval = new BigDecimal(input.trim());
			} catch (final NumberFormatException except) {
				println("Invalid number.");
				LOGGER.log(Level.FINER, "Invalid number", except);
			}
		}
		return retval;
	}

	/**
	 * Read a line of input from the input stream. It is trimmed of leading
	 * and trailing whitespace. Returns null on EOF (or other I/O error).
	 */
	@Override
	public @Nullable String inputString(final String prompt) {
		writePrompt(prompt);
		return Optional.ofNullable(readLine()).map(String::trim).orElse(null);
	}

	/**
	 * Ask the user a yes-or-no question, allowing yes-to-all or no-to-all to skip further questions.
	 */
	@Override
	public @Nullable Boolean inputBooleanInSeries(final String prompt, final String key,
	                                              final TrinaryPredicate<String> quitResultFactory) {
		if (seriesState.containsKey(key)) {
			writePrompt(prompt);
			boolean retval = seriesState.get(key);
			println(retval ? "yes" : "no");
			return retval;
		} // else
		while (true) {
			String input = Optional.ofNullable(inputString(prompt))
				.map(String::toLowerCase).orElse(null);
			if (input == null || quitResultFactory.test(input) == null) {
				return null;
			}
			if ("all".equals(input) || "ya".equals(input) || "ta".equals(input) ||
					"always".equals(input)) {
				seriesState.put(key, true);
				return true;
			} else if ("none".equals(input) || "na".equals(input) ||
					"fa".equals(input) || "never".equals(input)) {
				seriesState.put(key, false);
				return false;
			} else if ("yes".equals(input) || "true".equals(input) ||
					"y".equals(input) || "t".equals(input)) {
				return true;
			} else if ("no".equals(input) || "false".equals(input) ||
					"n".equals(input) || "f".equals(input)) {
				return false;
			} else {
				println("Please enter \"yes\", \"no\", \"true\", or \"false\", the first");
				println("character of any of those, or \"all\", \"none\", \"always\", or");
				println("\"never\" to use the same answer for all further questions.");
			}
		}
	}

	/**
	 * Have the user choose an item from a list.
	 */
	@Override
	public Pair<Integer, @Nullable String> chooseStringFromList(final List<String> items,
	                                                            final String description, final String none, final String prompt, final boolean auto) {
		return chooseFromListImpl(items, description, none, prompt, auto, s -> s);
	}

	/**
	 * Ask the user for a multiline string.
	 */
	@Override
	public @Nullable String inputMultilineString(final String prompt) {
		StringBuilder builder = new StringBuilder();
		printlnAtInterval("Type . on a line by itself to end input, or , to start over.");
		while (true) {
			if (builder.length() == 0) {
				writePrompt(prompt);
			} else {
				print("> ");
			}
			String line = readLine();
			if (line == null) {
				return null;
			} else if (".".equals(line.trim())) {
				String retval = builder.toString();
				if (retval.endsWith(System.lineSeparator() + System.lineSeparator())) {
					return retval.trim() + System.lineSeparator() +
						System.lineSeparator();
				} else {
					return retval.trim();
				}
			} else if (",".equals(line.trim())) {
				builder.setLength(0);
			} else {
				builder.append(line);
				builder.append(System.lineSeparator());
			}
		}
	}

	@Override
	public void printlnAtInterval(final String line, final int interval) {
		if (intervals.containsKey(line) && intervals.get(line) < interval) {
			// do nothing
		} else {
			println(line);
			intervals.put(line, 0L);
		}
	}
}
