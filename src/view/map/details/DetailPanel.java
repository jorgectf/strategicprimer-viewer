package view.map.details;

import java.awt.Dimension;
import java.util.EnumMap;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import model.viewer.Tile;
import model.viewer.TileType;

/**
 * A panel to show the details of a tile. FIXME: If the map includes the name of
 * a player, it should show that player's name in addition to his or her number.
 * 
 * @author Jonathan Lovelace
 */
public class DetailPanel extends JPanel {
	/**
	 * Maximum height of this panel, in pixels.
	 */
	public static final int DETAIL_PAN_MAX_HT = 175;
	/**
	 * Preferred width of this panel, in pixels.
	 */
	public static final int DETAIL_PANEL_HT = 125;
	/**
	 * Minimum width of this panel, in pixels.
	 */
	public static final int DETAIL_PAN_MIN_HT = 50;
	/**
	 * Panel to show the tile's coordinates and terrain type.
	 */
	private final TileDetailPanel typePanel = new TileDetailPanel();
	/**
	 * Panel to show chits for the items on the tile and the details of a selected chit.
	 */
	private final ChitAndDetailPanel chitPanel;
	/**
	 * Panel to show and edit exploration results.
	 */
	private final ResultsPanel resultsPanel = new ResultsPanel(DETAIL_PAN_MIN_HT,
			DETAIL_PANEL_HT, DETAIL_PAN_MAX_HT);

	/**
	 * Constructor.
	 */
	public DetailPanel() {
		super();
		setMaximumSize(new Dimension(Integer.MAX_VALUE, DETAIL_PAN_MAX_HT));
		setMinimumSize(new Dimension(Integer.MAX_VALUE, DETAIL_PAN_MIN_HT));
		setPreferredSize(new Dimension(Integer.MAX_VALUE, DETAIL_PANEL_HT));
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		typePanel.updateText(new Tile(-1, -1, TileType.NotVisible));
		add(typePanel);
		chitPanel = new ChitAndDetailPanel(DETAIL_PAN_MAX_HT, DETAIL_PAN_MIN_HT, DETAIL_PANEL_HT);
		add(chitPanel);

		add(resultsPanel);
		add(new KeyPanel());
	}

	/**
	 * The tile we refer to.
	 */
	private Tile tile;
	
	/**
	 * @param newTile
	 *            the tile we should now refer to.
	 */
	public void setTile(final Tile newTile) {
		if (newTile.equals(tile)) {
			return;
		} else {
			tile = newTile;
		}
		typePanel.updateText(tile);
		chitPanel.updateChits(tile);
		resultsPanel.setTile(tile);
		repaint();
	}

	/**
	 * Descriptions of the types.
	 */
	private static final Map<TileType, String> DESCRIPTIONS = new EnumMap<TileType, String>(
			TileType.class);
	static {
		DESCRIPTIONS.put(TileType.BorealForest, "<html><p>Boreal Forest</p></html>");
		DESCRIPTIONS.put(TileType.Desert, "<html><p>Desert</p></html>");
		DESCRIPTIONS.put(TileType.Jungle, "<html><p>Jungle</p></html>");
		DESCRIPTIONS.put(TileType.Mountain, "<html><p>Mountains</p></html>");
		DESCRIPTIONS.put(TileType.NotVisible, "<html><p>Unknown</p></html>");
		DESCRIPTIONS.put(TileType.Ocean, "<html><p>Ocean</p></html>");
		DESCRIPTIONS.put(TileType.Plains, "<html><p>Plains</p></html>");
		DESCRIPTIONS.put(TileType.TemperateForest, "<html><p>Temperate Forest</p></html>");
		DESCRIPTIONS.put(TileType.Tundra, "<html><p>Tundra</p></html>");
	}

	/**
	 * @param type
	 *            a terrain type
	 * @return a String representation of that terrain type
	 */
	static String terrainText(final TileType type) { // NOPMD
		if (DESCRIPTIONS.containsKey(type)) {
			return DESCRIPTIONS.get(type);
		} // else
		throw new IllegalArgumentException("Unknown terrain type");
	}
	
	/**
	 * Run an encounter. This remains here despite the logic having moved to
	 * ResultsPanel until we find a better way.
	 */
	public void runEncounter() {
		resultsPanel.runEncounter();
	}
}
