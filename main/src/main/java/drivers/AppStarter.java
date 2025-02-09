package drivers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import java.awt.GraphicsEnvironment;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.swing.SwingUtilities;

import drivers.common.IDriverUsage;
import drivers.common.SPOptions;
import drivers.common.DriverFailedException;
import drivers.common.SPOptionsImpl;
import drivers.common.DriverFactory;

import drivers.common.cli.ICLIHelper;

import lovelace.util.LovelaceLogger;
import org.jetbrains.annotations.Nullable;

/* package */ class AppStarter {
	private final Map<String, Iterable<DriverFactory>> driverCache =
			AppChooserState.createCache(); // TODO: Can we, and should we, inline that into here?

	private static boolean includeInCLIList(final DriverFactory driver) {
		return driver.getUsage().includeInList(false);
	}

	public void startDriverOnArguments(final ICLIHelper cli, final SPOptions options, final String... args) throws DriverFailedException {
		LovelaceLogger.trace("Inside AppStarter#startDriver()");
		boolean gui = !GraphicsEnvironment.isHeadless();
		final SPOptionsImpl currentOptions = new SPOptionsImpl(StreamSupport.stream(options.spliterator(), false).toArray(Map.Entry[]::new));
		if (!currentOptions.hasOption("--gui")) {
			currentOptions.addOption("--gui", Boolean.toString(gui));
		}
		final List<String> others = new ArrayList<>();

		// TODO: Try to make an instance method
		final BiConsumer<DriverFactory, SPOptions> startChosenDriver = (driver, currentOptionsTyped) -> {
			if (driver.getUsage().isGraphical()) {
				SwingUtilities.invokeLater(() -> new DriverWrapper(driver).startCatchingErrors(cli,
						currentOptionsTyped, others.stream().skip(1).toArray(String[]::new)));
			} else {
				new DriverWrapper(driver).startCatchingErrors(cli, currentOptionsTyped,
						others.stream().skip(1).toArray(String[]::new));
			}
		};

		for (final String arg : args) {
			if (arg == null) {
				continue;
			} else if ("-g".equals(arg) || "--gui".equals(arg)) {
				LovelaceLogger.trace("User specified either -g or --gui");
				currentOptions.addOption("--gui");
				gui = true;
			} else if ("-c".equals(arg) || "--cli".equals(arg)) {
				LovelaceLogger.trace("User specified either -c or --cli");
				currentOptions.addOption("--gui", "false");
				gui = false;
			} else if (arg.startsWith("--gui=")) {
				final String tempString = arg.substring(6);
				LovelaceLogger.trace("User specified --gui=%s", tempString);
				if ("true".equalsIgnoreCase(tempString)) {
					gui = true;
				} else if ("false".equalsIgnoreCase(tempString)) {
					gui = false;
				} else {
					throw new DriverFailedException(new IllegalArgumentException("--gui=nonBoolean"));
				}
				currentOptions.addOption("--gui", tempString);
			} else if (arg.startsWith("-") && arg.contains("=")) {
				final String[] broken = arg.split("=");
				final String param = broken[0];
				final String rest = Stream.of(broken).skip(1).collect(Collectors.joining("="));
				currentOptions.addOption(param, rest);
				LovelaceLogger.trace("User specified %s=%s", param, rest);
			} else if (arg.startsWith("-")) {
				LovelaceLogger.trace("User specified non-app-choosing option %s", arg);
				currentOptions.addOption(arg);
			} else {
				LovelaceLogger.trace("User specified non-option argument %s", arg);
				others.add(arg);
			}
		}

		LovelaceLogger.trace("Reached the end of arguments");
		// TODO: Use appletChooser so we can support prefixes
		final @Nullable DriverFactory currentDriver;
		final @Nullable String command = others.stream().findFirst().orElse(null);
		final List<DriverFactory> drivers = Optional.ofNullable(command).map(driverCache::get)
				// TODO: Drop StreamSupport use if driverCache is changed to specify List.
				.map(l -> StreamSupport.stream(l.spliterator(), false).collect(Collectors.toList()))
				.orElse(Collections.emptyList());
		if (command != null && !drivers.isEmpty()) {
			final DriverFactory first = drivers.stream().findFirst().orElse(null);
			LovelaceLogger.trace("Found a driver or drivers");
			if (drivers.size() == 1) {
				LovelaceLogger.trace("Only one driver registered for that command");
				currentDriver = first;
			} else {
				final boolean localGui = gui;
				LovelaceLogger.trace("Multiple drivers registered; filtering by interface");
				currentDriver = drivers.stream()
						.filter(d -> d.getUsage().isGraphical() == localGui).findAny().orElse(null);
			}
		} else {
			LovelaceLogger.trace("No matching driver found");
			currentDriver = null;
		}
		if (currentOptions.hasOption("--help")) {
			if (currentDriver == null) {
				LovelaceLogger.trace("No driver selected, so giving choices.");
				System.out.println("Strategic Primer assistive programs suite");
				System.out.println("No app specified; use one of the following invocations:");
				System.out.println();
				for (final DriverFactory driver : driverCache.values().stream()
						.flatMap(l -> StreamSupport.stream(l.spliterator(), false)).collect(Collectors.toSet())) {
					// TODO: in Java 11+ use String.lines()
					final String[] lines = AppChooserState.usageMessage(driver.getUsage(), "true".equals(
							options.getArgument("--verbose"))).split(System.lineSeparator());
					final String invocationExample = lines[0].replace("Usage: ", "");
					final String description = lines.length > 1 ? lines[1].replace(".", "") : "An unknown app";
					System.out.printf("%s: %s%n", description, invocationExample);
				}
			} else {
				final IDriverUsage currentUsage = currentDriver.getUsage();
				LovelaceLogger.trace("Giving usage information for selected driver");
				// TODO: Can we and should we move the usageMessage() method into this class?
				System.out.println(AppChooserState.usageMessage(currentUsage,
						"true".equals(options.getArgument("--verbose"))));
			}
		} else if (currentDriver != null) {
			LovelaceLogger.trace("Starting chosen app.");
			startChosenDriver.accept(currentDriver, currentOptions.copy());
		} else {
			LovelaceLogger.trace("Starting app-chooser.");
			final SPOptions currentOptionsTyped = currentOptions.copy();
			if (gui) {
//				try {
				SwingUtilities.invokeLater(
						() -> new AppChooserGUI(cli, currentOptionsTyped)
								.startDriver(others.toArray(String[]::new)));
//				} catch (DriverFailedException except) {
//					LovelaceLogger.error(except, except.getMessage());
//					SwingUtilities.invokeLater(() -> showErrorDialog(null,
//						"Strategic Primer Assistive Programs", except.getMessage()));
//				}
			} else {
				final DriverFactory chosenDriver = cli.chooseFromList(driverCache.values().stream()
						.flatMap(i -> StreamSupport.stream(i.spliterator(), false))
						.filter(AppStarter::includeInCLIList).collect(Collectors.toList()), "CLI apps available:", "No applications available", "App to start: ", ICLIHelper.ListChoiceBehavior.AUTO_CHOOSE_ONLY).getValue1();
				if (chosenDriver != null) {
					new DriverWrapper(chosenDriver).startCatchingErrors(cli, options,
							others.toArray(String[]::new));
				}
			}
		}
	}
}
