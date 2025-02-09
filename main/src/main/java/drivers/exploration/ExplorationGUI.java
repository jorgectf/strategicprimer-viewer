package drivers.exploration;

import drivers.common.DriverFailedException;
import java.io.IOException;

import java.nio.file.Path;

import javax.swing.SwingUtilities;

import drivers.common.SPOptions;

import drivers.common.cli.ICLIHelper;

import drivers.gui.common.about.AboutDialog;

import drivers.IOHandler;

import exploration.common.IExplorationModel;
import exploration.common.ExplorationModel;

import drivers.gui.common.SPFrame;
import drivers.gui.common.WindowCloseListener;
import drivers.gui.common.MenuBroker;
import drivers.gui.common.SPFileChooser;

import lovelace.util.FileChooser;

import common.map.IMutableMapNG;
import lovelace.util.LovelaceLogger;

/**
 * An object to start the exploration GUI.
 */
/* package */ class ExplorationGUI implements IExplorationGUI {
	public ExplorationGUI(final ICLIHelper cli, final SPOptions options, final IExplorationModel model) {
		this.cli = cli;
		this.options = options;
		this.model = model;
	}

	private final ICLIHelper cli;

	private final IExplorationModel model;
	private final SPOptions options;

	@Override
	public IExplorationModel getModel() {
		return model;
	}

	@Override
	public SPOptions getOptions() {
		return options;
	}

	private void createWindow(final MenuBroker menuHandler) {
		// FIXME: Try to remove the use of 'this' here
		final SPFrame frame = new ExplorationFrame(this, menuHandler);
		frame.addWindowListener(new WindowCloseListener(menuHandler));
		try {
			menuHandler.registerWindowShower(new AboutDialog(frame,
				frame.getWindowName()), "about");
		} catch (final IOException except) {
			LovelaceLogger.error(except, "I/O error while loading About dialog text");
			// But go on anyway
		}
		frame.showWindow();
	}

	@Override
	public void startDriver() {
		final MenuBroker menuHandler = new MenuBroker();
		menuHandler.register(new IOHandler(this, cli),
			"load", "save", "save as", "new", "load secondary", "save all",
			"open in map viewer", "open secondary map in map viewer", "close", "quit");
		SwingUtilities.invokeLater(() -> createWindow(menuHandler));
	}

	/**
	 * Ask the user to choose a file or files.
	 */
	@Override
	public Iterable<Path> askUserForFiles() throws DriverFailedException {
		try {
			return SPFileChooser.open((Path) null).getFiles();
		} catch (final FileChooser.ChoiceInterruptedException except) {
			throw new DriverFailedException(except, "Choice interrupted or user didn't choose");
		}
	}

	@Override
	public void open(final IMutableMapNG map) {
		if (model.isMapModified()) {
			SwingUtilities.invokeLater(() -> new ExplorationGUI(cli, options,
				new ExplorationModel(map)).startDriver());
		} else {
			model.setMap(map);
		}
	}
}
