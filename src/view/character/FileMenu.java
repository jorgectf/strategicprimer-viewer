package view.character;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import view.util.IsAdmin;
import controller.character.CharacterReader;
import controller.character.CharacterWriter;

/**
 * File menu for the character management program.
 * 
 * @author kingjon
 * 
 */
public class FileMenu extends JMenu implements ActionListener {
	/**
	 * Version UID for serialization.
	 */
	private static final long serialVersionUID = 8844526617362435258L;

	/**
	 * Logger.
	 */
	private static final Logger LOGGER = Logger.getLogger(FileMenu.class
			.getName());

	/**
	 * Constructor
	 * 
	 * @param _window
	 *            the window we're attached to
	 */
	public FileMenu(final CharacterFrame _window) {
		super("File");
		window = _window;
		addMenuItem("Open");
		addMenuItem("Save As");
		addSeparator();
		addMenuItem("Close");
	}

	/**
	 * The window we're attached to.
	 */
	private final CharacterFrame window;

	/**
	 * The file-chooser dialog.
	 */
	private static final JFileChooser FILE_CHOOSER = new JFileChooser();

	/**
	 * Handle menu selections.
	 * 
	 * @param evt
	 *            the event to handle
	 */
	@Override
	public void actionPerformed(final ActionEvent evt) {
		if ("Save As".equals(evt.getActionCommand())) {
			if (FILE_CHOOSER.showSaveDialog(window) == JFileChooser.APPROVE_OPTION) {
				try {
					new CharacterWriter(FILE_CHOOSER.getSelectedFile()
							.getPath()).write(window.getCharacter(),
							!IsAdmin.IS_ADMIN);
				} catch (IOException e) {
					LOGGER.log(Level.SEVERE, "I/O error while saving the map",
							e);
				}
			}
		} else if ("Open".equals(evt.getActionCommand())) {
			if (FILE_CHOOSER.showOpenDialog(window) == JFileChooser.APPROVE_OPTION) {
				try {
					new CharacterFrame(new CharacterReader(FILE_CHOOSER
							.getSelectedFile().getPath()).getCharacter())
							.setVisible(true);
				} catch (final FileNotFoundException except) {
					LOGGER.log(Level.WARNING,
							"File not found while opening the map", except);
				} catch (final IOException except) {
					LOGGER.log(Level.SEVERE, "I/O error while opening the map",
							except);
				}
			}
		} else if ("Close".equals(evt.getActionCommand())) {
			window.setVisible(false);
			window.dispose();
		} 
	}

	/**
	 * Add a menu item and set this as its ActionListener.
	 * 
	 * @param text
	 *            the text of the menu item
	 */
	private void addMenuItem(final String text) {
		final JMenuItem item = new JMenuItem(text);
		item.addActionListener(this);
		add(item);
	}

}
