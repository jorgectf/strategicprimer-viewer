package view.map.main;

import java.awt.event.MouseEvent;
import java.util.logging.Logger;

import model.viewer.PointFactory;
import model.viewer.Tile;
import model.viewer.TileType;

/**
 * A class to keep track of which tile is selected.
 * 
 * @author Jonathan Lovelace
 */
// ESCA-JAVA0137:
public class TileSelectionListener extends SelectionListener {
	/**
	 * Logger.
	 */
	private static final Logger LOGGER = Logger
			.getLogger(TileSelectionListener.class.getName());
	/**
	 * The menu to change a tile's type.
	 */
	private static final TerrainChangingMenu MENU = new TerrainChangingMenu();
	/**
	 * The detail panel.
	 */
	private final DetailPanel detailPanel;
	/**
	 * The main viewer ... the only object that can copy a tile from the main
	 * map to the alternate.
	 */
	private final MapPanel viewer;

	/**
	 * Constructor.
	 * 
	 * @param view
	 *            the main panel. Needed to copy a tile from the main map to a
	 *            sub-map.
	 * @param details
	 *            the panel that'll show the details of the selected tile
	 */
	public TileSelectionListener(final MapPanel view, final DetailPanel details) {
		super();
		if (details == null) {
			throw new IllegalArgumentException("DetailPanel was null");
		} else if (view == null) {
			throw new IllegalArgumentException("ViewerFrame was null");
		}
		detailPanel = details;
		viewer = view;
	}

	/**
	 * Handle mouse clicks.
	 * 
	 * @param event
	 *            the event to handle
	 */
	@Override
	public void mouseClicked(final MouseEvent event) {
		super.mouseClicked(event);
		if (selection() instanceof GUITile) {
			detailPanel.setTile(((GUITile) selection()).getTile());
			if (event.getClickCount() == 2) {
				detailPanel.runEncounter();
				viewer.copyTile(((GUITile) selection()).getTile());
			}
			selection().requestFocusInWindow();
			LOGGER.fine("Click");
		} else {
			detailPanel.setTile(new Tile(-1, -1, TileType.NotVisible));
		}
	}
	/**
	 * Move the cursor up one.
	 */
	public void up() { // NOPMD
		if (selection() instanceof GUITile) {
			final GUITile newTile = viewer.getTile(PointFactory.point(
					((GUITile) selection()).getTile().getRow() - 1,
					((GUITile) selection()).getTile().getCol())); 
			setSelection(newTile);
			detailPanel.setTile(newTile.getTile());
		}
	}
	/**
	 * Move the cursor left one.
	 */
	public void left() {
		if (selection() instanceof GUITile) {
			final GUITile newTile = viewer.getTile(PointFactory.point(
					((GUITile) selection()).getTile().getRow(),
					((GUITile) selection()).getTile().getCol() - 1)); 
			setSelection(newTile);
			detailPanel.setTile(newTile.getTile());
		}
	}
	/**
	 * Move the cursor down one.
	 */
	public void down() { // NOPMD
		if (selection() instanceof GUITile) {
			final GUITile newTile = viewer.getTile(PointFactory.point(
					((GUITile) selection()).getTile().getRow() + 1,
					((GUITile) selection()).getTile().getCol())); 
			setSelection(newTile);
			detailPanel.setTile(newTile.getTile());
		}
	}
	/**
	 * Move the cursor right one.
	 */
	public void right() {
		if (selection() instanceof GUITile) {
			final GUITile newTile = viewer.getTile(PointFactory.point(
					((GUITile) selection()).getTile().getRow(),
					((GUITile) selection()).getTile().getCol() + 1)); 
			setSelection(newTile);
			detailPanel.setTile(newTile.getTile());
		}
	}
	/**
	 * Ignored.
	 * 
	 * @param event
	 *            the event to handle
	 */
	@Override
	public void mousePressed(final MouseEvent event) {
		super.mousePressed(event);
		if (event.isPopupTrigger()) {
			MENU.setTile((GUITile) event.getComponent());
			MENU.show(event.getComponent(), event.getX(), event.getY());
		}
	}

	/**
	 * Ignored.
	 * 
	 * @param event
	 *            the event to handle
	 */
	@Override
	public void mouseReleased(final MouseEvent event) {
		super.mouseReleased(event);
		if (event.isPopupTrigger()) {
			MENU.setTile((GUITile) event.getComponent());
			MENU.show(event.getComponent(), event.getX(), event.getY());
		}
	}

	/**
	 * Clear the selection.
	 */
	@Override
	public void clearSelection() {
		super.clearSelection();
		detailPanel.setTile(new Tile(-1, -1, TileType.NotVisible));
	}
}
