package controller.map.misc;

import java.awt.Component;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import org.eclipse.jdt.annotation.Nullable;
import util.NullCleaner;
import util.TypesafeLogger;
import view.util.FilteredFileChooser;

import static javax.swing.JFileChooser.APPROVE_OPTION;
import static util.NullCleaner.assertNotNull;

/**
 * A class to hide the details of choosing a file from the caller.
 *
 * FIXME: Use an Optional-of-Path instead of a Path of "" for no-selection.
 *
 * This is part of the Strategic Primer assistive programs suite developed by Jonathan
 * Lovelace.
 *
 * Copyright (C) 2013-2014 Jonathan Lovelace
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
public final class FileChooser {
	/**
	 * Logger.
	 */
	private static final Logger LOGGER = TypesafeLogger.getLogger(FileChooser.class);
	/**
	 * The file we'll return, if valid.
	 */
	private Path file;
	/**
	 * Whether we should return the filename (if not, we'll show the dialog, then
	 * throw an
	 * exception if that fails).
	 */
	private boolean shouldWait = true;
	/**
	 * A file chooser.
	 */
	private final JFileChooser chooser;
	/**
	 * The method to call to ask the user to choose a file.
	 */
	private final ToIntFunction<@Nullable Component> chooserFunc;
	/**
	 * Constructor allowing the caller to pass in a file-chooser to have the user choose
	 * with.
	 * @param loc the file to return
	 * @param fileChooser the file-chooser to use
	 * @param operation which operation to use. Must be one of the two defined in
	 *                     JFileChooser (OPEN_DIALOG or SAVE_DIALOG; CUSTOM_DIALOG is
	 *                     not yet supported)
	 */
	public FileChooser(final Path loc, final JFileChooser fileChooser,
					   final int operation) {
		file = Paths.get("");
		setFile(loc);
		chooser = fileChooser;
		switch (operation) {
		case JFileChooser.OPEN_DIALOG:
			chooserFunc = fileChooser::showOpenDialog;
			break;
		case JFileChooser.SAVE_DIALOG:
			chooserFunc = fileChooser::showSaveDialog;
			break;
		default:
			throw new IllegalArgumentException("Only OPEN_DIALOG and SAVE_DIALOG " +
													"operations are supported");
		}
	}
	/**
	 * Constructor. When the filename is asked for, if the given value is valid, we'll
	 * return it instead of showing a dialog.
	 *
	 * @param loc the file to return.
	 */
	public FileChooser(final Path loc) {
		this(loc, new FilteredFileChooser(), JFileChooser.OPEN_DIALOG);
	}

	/**
	 * No-arg constructor. We'll show a dialog unconditionally when the filename is asked
	 * for.
	 */
	public FileChooser() {
		this(Paths.get(""));
	}

	/**
	 * If no valid filename was passed in, show a dialog for the user to select one;
	 * return the filename passed in or the filename the user selected.
	 *
	 * @return the file the caller or the user chose
	 * @throws ChoiceInterruptedException when the choice is interrupted or the user
	 *                                    declines to choose a file.
	 */
	@SuppressWarnings("NewExceptionWithoutArguments")
	public Path getFile() throws ChoiceInterruptedException {
		if (shouldWait) {
			if (SwingUtilities.isEventDispatchThread()) {
				final int status = chooserFunc.applyAsInt(null);
				if (status == APPROVE_OPTION) {
					final File selectedFile = chooser.getSelectedFile();
					setFile(assertNotNull(selectedFile.toPath()));
					if (selectedFile.toString().isEmpty()) {
						LOGGER.severe("JFileChooser produced empty file");
					} else if (file.toString().isEmpty()) {
						LOGGER.severe("Selection supposedly empty but shouldn't be");
					}
				} else {
					LOGGER.log(Level.INFO, "Chooser function returned %d",
							Integer.valueOf(status));
				}
			} else {
				//noinspection UnnecessaryLocalVariable
				final JFileChooser fileChooser = chooser;
				invoke(() -> {
					final int status = chooserFunc.applyAsInt(null);
					if (status == APPROVE_OPTION) {
						setFile(NullCleaner.valueOrDefault(fileChooser.getSelectedFile(),
								new File("")).toPath());
						if (file.toString().isEmpty()) {
							LOGGER.severe("Selection supposedly empty");
						}
					} else {
						LOGGER.log(Level.INFO, "Chooser function returned %d",
								Integer.valueOf(status));
					}
				});
			}
		}
		if (file.toString().isEmpty()) {
			throw new ChoiceInterruptedException();
		} else {
			return file;
		}
	}
	/**
	 * Allow the user to choose a file, if necessary, and pass that file to the given
	 * consumer. If the operation is canceled, do nothing.
	 * @param consumer something that takes a File.
	 */
	public void call(final Consumer<Path> consumer) {
		try {
			final Path chosenFile = getFile();
			consumer.accept(chosenFile);
		} catch (final ChoiceInterruptedException interruption) {
			LOGGER.log(Level.INFO, "Choice interrupted or user failed to choose",
					interruption);
		}
	}
	/**
	 * invokeAndWait(), and throw a ChoiceInterruptedException if interrupted or
	 * otherwise
	 * failing.
	 *
	 * @param runnable the runnable to run.
	 * @throws ChoiceInterruptedException on error
	 */
	private static void invoke(final Runnable runnable)
			throws ChoiceInterruptedException {
		try {
			SwingUtilities.invokeAndWait(runnable);
		} catch (final InvocationTargetException except) {
			final Throwable cause = except.getCause();
			if (cause == null) {
				throw new ChoiceInterruptedException(except);
			} else {
				throw new ChoiceInterruptedException(cause);
			}
		} catch (final InterruptedException except) {
			throw new ChoiceInterruptedException(except);
		}
	}

	/**
	 * (Re-)set the file to return.
	 *
	 * @param loc the file to return
	 */
	public void setFile(final Path loc) {
		if (loc.toString().isEmpty()) {
			file = Paths.get("");
			shouldWait = true;
		} else {
			file = loc;
			shouldWait = false;
		}
	}

	/**
	 * An exception to throw when no selection was made or selection was interrupted
	 * by an
	 * exception.
	 *
	 * @author Jonathan Lovelace
	 */
	public static final class ChoiceInterruptedException extends Exception {
		/**
		 * @param cause an exception that we caught that interrupted the choice
		 */
		public ChoiceInterruptedException(final Throwable cause) {
			super("Choice of a file was interrupted by an exception:", cause);
		}

		/**
		 * No-arg constructor.
		 */
		public ChoiceInterruptedException() {
			super("No file was selected");
		}
	}

	/**
	 * @return a String representation of the object
	 */
	@SuppressWarnings("MethodReturnAlwaysConstant")
	@Override
	public String toString() {
		return "FileChooser";
	}
}
