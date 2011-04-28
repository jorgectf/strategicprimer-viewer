package view.map;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.logging.Logger;

/**
 * A class to keep track of which tile is selected.
 * 
 * @author Jonathan Lovelace
 */
//ESCA-JAVA0137:
public class SelectionListener implements MouseListener {
	/**
	 * The currently-selected tile.
	 */
	private GUITile selection;
	/**
	 * Logger.
	 */
	private static final Logger LOGGER = Logger
			.getLogger(SelectionListener.class.getName());
	/**
	 * The menu to change a tile's type.
	 */
	private static final TerrainChangingMenu MENU = new TerrainChangingMenu();
	/**
	 * The detail panel.
	 */
	private final DetailPanel detailPanel;
	/**
	 * Constructor.
	 * @param details the panel that'll show the details of the selected tile
	 */
	public SelectionListener(final DetailPanel details) {
		if (details == null) {
			throw new IllegalArgumentException("DetailPanel was null");
		}
		detailPanel = details;
	}
	/**
	 * Handle mouse clicks.
	 * 
	 * @param event
	 *            the event to handle
	 */
	@Override
	public void mouseClicked(final MouseEvent event) {
		if (event.getComponent() instanceof GUITile) {
			if (selection != null) {
				selection.setSelected(false);
			}
			selection = (GUITile) event.getComponent();
			selection.setSelected(true);
			detailPanel.setTile(selection.getTile());
			LOGGER.fine("Click");
		}
	}

	/**
	 * Ignored
	 * 
	 * @param event
	 *            ignored
	 */
	@Override
	public void mouseEntered(final MouseEvent event) {
		// Do nothing
	}

	/**
	 * Ignored
	 * 
	 * @param event
	 *            ignored
	 */
	@Override
	public void mouseExited(final MouseEvent event) {
		// Do nothing
	}

	/**
	 * Ignored
	 * 
	 * @param event
	 *            the event to handle
	 */
	@Override
	public void mousePressed(final MouseEvent event) {
		if (event.isPopupTrigger()) {
			MENU.setTile((GUITile) event.getComponent());
			MENU.show(event.getComponent(), event.getX(), event.getY());
		}
	}

	/**
	 * Ignored
	 * 
	 * @param event
	 *            the event to handle
	 */
	@Override
	public void mouseReleased(final MouseEvent event) {
		if (event.isPopupTrigger()) {
			MENU.setTile((GUITile) event.getComponent());
			MENU.show(event.getComponent(), event.getX(), event.getY());
		}
	}

}
