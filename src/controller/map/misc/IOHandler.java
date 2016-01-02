package controller.map.misc;

import controller.map.formatexceptions.SPFormatException;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.xml.stream.XMLStreamException;
import model.listeners.PlayerChangeListener;
import model.listeners.PlayerChangeSource;
import model.map.IMapNG;
import model.map.IMutableMapNG;
import model.map.Player;
import model.map.PlayerCollection;
import model.map.SPMapNG;
import model.misc.IDriverModel;
import model.misc.IMultiMapModel;
import model.viewer.IViewerModel;
import model.viewer.ViewerModel;
import org.eclipse.jdt.annotation.Nullable;
import util.NullCleaner;
import util.Pair;
import util.TypesafeLogger;
import util.Warning;
import view.map.main.FindDialog;
import view.map.main.SelectTileDialog;
import view.map.main.ViewerFrame;
import view.map.main.ZoomListener;
import view.util.AboutDialog;
import view.util.DriverQuit;
import view.util.ErrorShower;
import view.util.FilteredFileChooser;
import view.util.TreeExpansionOrderListener;

/**
 * An ActionListener to dispatch file I/O.
 *
 * This is part of the Strategic Primer assistive programs suite developed by Jonathan
 * Lovelace.
 *
 * Copyright (C) 2011-2015 Jonathan Lovelace
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
public final class IOHandler implements ActionListener, PlayerChangeSource {
	/**
	 * Error message fragment when file not found.
	 */
	protected static final String NOT_FOUND_ERROR = " not found";
	/**
	 * Error message when the map contains invalid data.
	 */
	protected static final String INV_DATA_ERROR = "Map contained invalid data";
	/**
	 * An error message refactored from at least four uses.
	 */
	protected static final String XML_ERROR_STRING = "Error reading XML file";
	/**
	 * Logger.
	 */
	private static final Logger LOGGER = TypesafeLogger
												 .getLogger(IOHandler.class);
	/**
	 * File chooser.
	 */
	protected final JFileChooser chooser;

	/**
	 * The list of player-change listeners.
	 */
	private final Collection<PlayerChangeListener> playerChangeListeners = new ArrayList<>();
	/**
	 * The list of tree-expansion-order listeners.
	 */
	private final Collection<TreeExpansionOrderListener> treeExpansionListeners =
			new ArrayList<>();
	/**
	 * The current player.
	 */
	private Player currentPlayer;
	/**
	 * The map model, which needs to be told about newly loaded maps and holds maps to be
	 * saved.
	 */
	private final IDriverModel model;
	/**
	 * The "find" dialog, if this is for a map viewer.
	 */
	@Nullable
	private FindDialog finder = null;
	/**
	 * The handler for zoom-related items.
	 *
	 * TODO: Consider combining with this class.
	 */
	@Nullable
	private final ZoomListener zoomer;


	/**
	 * Handle the "load" menu item.
	 *
	 * @param source the source of the event. May be null, since JFileChooser doesn't
	 *                  seem
	 *               to care
	 */
	private void handleLoadMenu(@Nullable final Component source) {
		if (chooser.showOpenDialog(source) == JFileChooser.APPROVE_OPTION) {
			final File file = chooser.getSelectedFile();
			if (file == null) {
				return;
			}
			try {
				model.setMap(readMap(file, Warning.INSTANCE), file);
			} catch (final IOException | SPFormatException | XMLStreamException e) {
				handleError(e, NullCleaner.valueOrDefault(file.getPath(),
						"a null path"), source);
			}
		}
	}

	/**
	 * Handle menu selections.
	 *
	 * @param event the event to handle
	 */
	@Override
	public void actionPerformed(@Nullable final ActionEvent event) {
		if (event != null) { // it wouldn't be @Nullable except that the JDK
			// isn't annotated
			final Component source = eventSource(event.getSource());
			@Nullable
			final Frame parent = getContainingFrame(source);
			switch(event.getActionCommand().toLowerCase()) {
			case "load":
				handleLoadMenu(source);
				break;
			case "save":
				saveMap(source);
				break;
			case "save as":
				saveMapAs(model.getMap(), source);
				break;
			case "new":
				startNewViewerWindow();
				break;
			case "about":
				// FIXME: This should use the title of whatever app it was called from
				new AboutDialog(source, "Exploration Helper").setVisible(true);
				break;
			case "load secondary":
				handleSecondaryLoadMenu(source);
				break;
			case "save all":
				saveAll(source);
				break;
			case "open in map viewer":
				final ViewerModel nmodel = new ViewerModel(model);
				SwingUtilities.invokeLater(
						() -> new ViewerFrame(nmodel, new IOHandler(nmodel, chooser))
								      .setVisible(true));
				break;
			case "open secondary map in map viewer":
				if (model instanceof IMultiMapModel) {
					final Pair<IMutableMapNG, File> mapPair =
							((IMultiMapModel) model).getSubordinateMaps().iterator()
									.next();
					final ViewerModel submodel =
							new ViewerModel(mapPair.first(), mapPair.second());
					SwingUtilities.invokeLater(
							() -> new ViewerFrame(submodel, new IOHandler(submodel, chooser))
									      .setVisible(true));
				}
				break;
			case "go to tile":
				if (model instanceof IViewerModel) {
					SwingUtilities.invokeLater(() -> new SelectTileDialog(parent, (IViewerModel) model)
							                                 .setVisible(true));
				}
				break;
			case "close":
				if (parent != null) {
					parent.setVisible(false);
					parent.dispose();
				}
				break;
			case "find a fixture":
				if (model instanceof IViewerModel) {
					SwingUtilities
							.invokeLater(() -> {
								FindDialog fdialog = getFindDialog(source);
								if (fdialog != null) {
									fdialog.setVisible(true);
								}
							});
				}
				break;
			case "find next":
				if (model instanceof IViewerModel) {
					SwingUtilities.invokeLater(() -> {
						FindDialog fdialog = getFindDialog(source);
						if (fdialog != null) {
							fdialog.search();
						}
					});
				}
				break;
			case "Change current player":
				final Player retval = (Player) JOptionPane.showInputDialog(parent,
						"Player to view:", "Choose New Player",
						JOptionPane.PLAIN_MESSAGE, null, playersAsArray(model
								                                                .getMap()
								                                                .players()),

						currentPlayer);
				if (retval != null) {
					for (final PlayerChangeListener list : playerChangeListeners) {
						list.playerChanged(currentPlayer, retval);
					}
					currentPlayer = retval;
				}
				break;
			case "reload tree":
				for (final PlayerChangeListener listener : playerChangeListeners) {
					listener.playerChanged(currentPlayer, currentPlayer);
				}
				break;
			case "zoom in":
			case "zoom out":
			case "center":
				if (zoomer != null) {
					zoomer.actionPerformed(event);
				}
				break;
			case "expand all":
				treeExpansionListeners.forEach(TreeExpansionOrderListener::expandAll);
				break;
			case "collapse all":
				treeExpansionListeners.forEach(TreeExpansionOrderListener::collapseAll);
				break;
			case "expand unit kinds":
				treeExpansionListeners.forEach(listener -> listener.expandSome(2));
				break;
			case "quit":
				DriverQuit.quit(0);
			}
		}
	}

	/**
	 * @param obj an object
	 * @return it if it's a component, or null
	 */
	@SuppressWarnings("ReturnOfNull")
	@Nullable
	private static Component eventSource(@Nullable final Object obj) {
		if (obj instanceof Component) {
			return (Component) obj; // NOPMD
		} else {
			return null;
		}
	}

	/**
	 * Start a new viewer window with a blank map of the same size as the model's current
	 * map.
	 */
	private void startNewViewerWindow() {
		final ViewerModel nmodel = new ViewerModel(new SPMapNG(model.getMapDimensions(),
				                                                      new PlayerCollection(),
				                                                      model.getMap()
						                                                      .getCurrentTurn()),

				                                          new File(""));
		SwingUtilities.invokeLater(
				() -> new ViewerFrame(nmodel, new IOHandler(nmodel)).setVisible(true));
	}

	/**
	 * Constructor.
	 *
	 * @param map      the map model
	 * @param fchooser the file chooser
	 */
	public IOHandler(final IDriverModel map, final JFileChooser fchooser) {
		model = map;
		chooser = fchooser;
		if (model instanceof IViewerModel) {
			zoomer = new ZoomListener((IViewerModel) model);
		} else {
			zoomer = null;
		}
		currentPlayer = model.getMap().getCurrentPlayer();
	}
	/**
	 * Constructor. File-chooser defaults to the current directory filtered to include
	 * only maps.
	 *
	 * @param map      the map model
	 */
	public IOHandler(final IDriverModel map) {
		this(map, new FilteredFileChooser());
	}

	/**
	 * Display an appropriate error message.
	 *
	 * @param except   an Exception
	 * @param filename the file we were trying to process
	 * @param source   the component to use as the parent of the error dialog. May be
	 *                 null.
	 */
	protected static void handleError(final Exception except,
									  final String filename,
									  @Nullable final Component source) {
		final String msg;
		if (except instanceof XMLStreamException) {
			msg = XML_ERROR_STRING + ' ' + filename;
		} else if (except instanceof FileNotFoundException) {
			msg = "File " + filename + NOT_FOUND_ERROR;
		} else if (except instanceof IOException) {
			msg = "I/O error reading file " + filename;
		} else if (except instanceof SPFormatException) {
			msg = INV_DATA_ERROR + " in file " + filename;
		} else {
			throw new IllegalStateException("Unknown exception type", except);
		}
		LOGGER.log(Level.SEVERE, msg, except);
		ErrorShower.showErrorDialog(source, msg);
	}

	/**
	 * Save a map to the filename it was loaded from.
	 *
	 * @param source the source of the event that triggered this. May be null if it
	 *                  wasn't
	 *               a Component.
	 */
	private void saveMap(@Nullable final Component source) {
		try {
			new MapReaderAdapter().write(model.getMapFile(), model.getMap());
		} catch (final IOException e) {
			ErrorShower.showErrorDialog(source, "I/O error writing to file "
														+ model.getMapFile().getPath());
			LOGGER.log(Level.SEVERE, "I/O error writing XML", e);
		}
	}

	/**
	 * Save a map.
	 *
	 * @param map    the map to save.
	 * @param source the source of the event. May be null if the source wasn't a
	 *               component.
	 */
	private void saveMapAs(final IMapNG map, @Nullable final Component source) {
		if (chooser.showSaveDialog(source) == JFileChooser.APPROVE_OPTION) {
			final File file = chooser.getSelectedFile();
			if (file == null) {
				return;
			}
			try {
				new MapReaderAdapter().write(file, map);
			} catch (final IOException e) {
				ErrorShower.showErrorDialog(source,
						"I/O error writing to file "
								+ file.getPath());
				LOGGER.log(Level.SEVERE, "I/O error writing XML", e);
			}
		}
	}

	/**
	 * @param file   a file to load a map from
	 * @param warner the Warning instance to use for warnings.
	 * @return the map in that file
	 * @throws IOException        on other I/O error
	 * @throws XMLStreamException if the XML isn't well-formed
	 * @throws SPFormatException  if the file contains invalid data
	 */
	protected static IMutableMapNG readMap(final File file, final Warning warner)
			throws IOException, XMLStreamException, SPFormatException {
		return new MapReaderAdapter().readMap(file, warner);
	}

	/**
	 * Save all maps to the filenames they were loaded from.
	 *
	 * @param source the source of the event that triggered this. May be null if it was
	 *               not a component.
	 */
	private void saveAll(@Nullable final Component source) {
		if (model instanceof IMultiMapModel) {
			final MapReaderAdapter adapter = new MapReaderAdapter();
			for (final Pair<IMutableMapNG, File> pair : ((IMultiMapModel) model)
																.getAllMaps()) {
				try {
					adapter.write(pair.second(), pair.first());
				} catch (final IOException e) {
					ErrorShower.showErrorDialog(source,
							"I/O error writing to file " + pair.second());
					LOGGER.log(Level.SEVERE, "I/O error writing XML", e);
				}
			}
		}
	}

	/**
	 * Handle the 'load secondary map' menu item.
	 *
	 * @param source the component to attach the dialog box to. May be null.
	 */
	private void handleSecondaryLoadMenu(@Nullable final Component source) {
		if ((model instanceof IMultiMapModel) && (chooser
														  .showOpenDialog(source) ==
														  JFileChooser.APPROVE_OPTION)) {
			final File file = chooser.getSelectedFile();
			if (file == null) {
				return;
			}
			try {
				((IMultiMapModel) model).addSubordinateMap(
						readMap(file, Warning.INSTANCE), file);
			} catch (final IOException | SPFormatException | XMLStreamException e) {
				handleError(e, NullCleaner.valueOrDefault(file.getPath(),
						"a null path"), source);
			}
		}
	}
	/**
	 * @param component a component
	 * @return a FindDialog if the driver model is for a map viewer, or null otherwise
	 */
	@Nullable
	private synchronized FindDialog getFindDialog(final Component component) {
		final Window window = getContainingFrame(component);
		if ((model instanceof IViewerModel) && (window instanceof Frame)) {
			if (finder == null) {
				final FindDialog local =
						new FindDialog((Frame) window, (IViewerModel) model);
				finder = local;
				return local;
			} else {
				return NullCleaner.assertNotNull(finder);
			}
		} else {
			return null;
		}
	}

	/**
	 * @return a String representation of the object.
	 */
	@SuppressWarnings("MethodReturnAlwaysConstant")
	@Override
	public String toString() {
		return "IOHandler";
	}
	/**
	 * @param list a listener to add
	 */
	@Override
	public void addPlayerChangeListener(final PlayerChangeListener list) {
		playerChangeListeners.add(list);
	}

	/**
	 * @param list a listener to remove
	 */
	@Override
	public void removePlayerChangeListener(final PlayerChangeListener list) {
		playerChangeListeners.remove(list);
	}
	/**
	 * @param players a collection of players
	 * @return the players as an array
	 */
	private static Player[] playersAsArray(final Iterable<Player> players) {
		if (players instanceof PlayerCollection) {
			return ((PlayerCollection) players).asArray(); // NOPMD
		} else {
			final List<Player> list = StreamSupport.stream(players.spliterator(), false)
					                          .collect(Collectors.toList());
			return NullCleaner
					       .assertNotNull(list.toArray(new Player[list.size()]));
		}
	}
	/**
	 * Should only be called once per object lifetime. Notify all listeners, as if the
	 * current player had changed from null to its current value.
	 */
	public void notifyListeners() {
		for (final PlayerChangeListener list : playerChangeListeners) {
			list.playerChanged(null, currentPlayer);
		}
	}
	/**
	 * @param list the listener to add
	 */
	public void addTreeExpansionListener(final TreeExpansionOrderListener list) {
		treeExpansionListeners.add(list);
	}
	/**
	 * @param list the listener to remove
	 */
	public void removeTreeExpansionListener(final TreeExpansionOrderListener list) {
		treeExpansionListeners.remove(list);
	}
	/**
	 * @param component a component
	 * @return the frame containing it, if any
	 */
	@Nullable
	private Frame getContainingFrame(final Component component) {
		Component temp = component;
		while (temp != null) {
			if (temp instanceof Frame) {
				return (Frame) temp;
			} else if (temp instanceof JPopupMenu) {
				temp = ((JPopupMenu) temp).getInvoker();
			} else {
				temp = temp.getParent();
			}
		}
		return null;
	}
}
