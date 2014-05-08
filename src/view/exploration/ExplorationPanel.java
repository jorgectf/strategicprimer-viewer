package view.exploration;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.text.Document;

import model.exploration.IExplorationModel;
import model.exploration.IExplorationModel.Direction;
import model.listeners.CompletionListener;
import model.listeners.CompletionSource;
import model.listeners.MovementCostListener;
import model.listeners.SelectionChangeListener;
import model.listeners.SelectionChangeSupport;
import model.map.IMap;
import model.map.IPlayerCollection;
import model.map.ITile;
import model.map.Point;
import model.map.Tile;
import model.map.TileType;

import org.eclipse.jdt.annotation.Nullable;

import util.IsNumeric;
import util.Pair;
import view.map.details.FixtureList;
import view.util.BorderedPanel;
import view.util.BoxPanel;
import view.util.ListenedButton;

/**
 * A panel to let the user explore using a unit.
 *
 * @author Jonathan Lovelace
 */
public class ExplorationPanel extends BorderedPanel implements ActionListener,
		SelectionChangeListener, CompletionSource, MovementCostListener {
	/**
	 * The label showing the current location of the explorer.
	 */
	private final JLabel locLabel = new JLabel(
			"<html><body>Currently exploring (-1, -1); click a tile to explore it. "
					+ "Selected fixtures in its left-hand list will be 'discovered'."
					+ "</body></html>");
	/**
	 * The list of completion listeners listening to us.
	 */
	private final List<CompletionListener> cListeners = new ArrayList<>();

	/**
	 * The text-field containing the running MP total.
	 */
	private final JTextField mpField;
	/**
	 * The collection of proxies for main-map tile-fixture-lists.
	 */
	private final Map<Direction, SelectionChangeSupport> mains = new EnumMap<>(
			Direction.class);
	/**
	 * The collection of proxies for secondary-map tile-fixture lists.
	 */
	private final Map<Direction, SelectionChangeSupport> seconds = new EnumMap<>(
			Direction.class);
	/**
	 * The collection of dual-tile-buttons.
	 */
	private final Map<Direction, DualTileButton> buttons = new EnumMap<>(
			Direction.class);

	/**
	 * The exploration model.
	 */
	private final IExplorationModel model;
	/**
	 * The text for the 'back' button.
	 */
	private static final String BACK_TEXT = "Select a different explorer";

	/**
	 * Constructor.
	 *
	 * @param emodel the exploration model.
	 * @param mpDoc the model underlying the remaining-MP text boxes.
	 */
	public ExplorationPanel(final IExplorationModel emodel, final Document mpDoc) {
		super();
		model = emodel;
		final BoxPanel headerPanel = new BoxPanel(true);
		headerPanel.add(new ListenedButton(BACK_TEXT, this));
		headerPanel.add(locLabel);
		headerPanel.add(new JLabel("Remaining Movement Points: "));
		mpField = new JTextField(mpDoc, null, 5);
		// TODO: store the reference to the document, not the text field, in the
		// class body.
		headerPanel.add(mpField);
		setCenter(new JSplitPane(JSplitPane.VERTICAL_SPLIT, headerPanel,
				setupTilesGUI(new JPanel(new GridLayout(3, 12, 2, 2)))));
	}

	/**
	 * Set up the GUI for the surrounding tiles.
	 *
	 * @param panel the panel to add them all to.
	 * @return it
	 */
	private JPanel setupTilesGUI(final JPanel panel) {
		return setupTilesGUIImpl(panel, Direction.Northwest, Direction.North,
				Direction.Northeast, Direction.West, Direction.Nowhere,
				Direction.East, Direction.Southwest, Direction.South,
				Direction.Southeast);
	}

	/**
	 * Set up the GUI for multiple tiles.
	 *
	 * @param panel the panel to add them all to.
	 * @param directions the directions to create GUIs for
	 * @return the panel
	 */
	private JPanel setupTilesGUIImpl(final JPanel panel,
			final Direction... directions) {
		for (final Direction direction : directions) {
			if (direction != null) {
				addTileGUI(panel, direction);
			}
		}
		return panel;
	}

	/**
	 * Handle a button press.
	 *
	 * @param evt the event to handle.
	 */
	@Override
	public final void actionPerformed(@Nullable final ActionEvent evt) {
		if (evt != null && BACK_TEXT.equalsIgnoreCase(evt.getActionCommand())) {
			for (final CompletionListener list : cListeners) {
				list.stopWaitingOn(true);
			}
		}
	}

	/**
	 * Set up the GUI representation of a tile---a list of its contents in the
	 * main map, a visual representation, and a list of its contents in a
	 * secondary map.
	 *
	 * @param panel the panel to add them to
	 * @param direction which direction from the currently selected tile this
	 *        GUI represents.
	 */
	private void addTileGUI(final JPanel panel, final Direction direction) {
		final SelectionChangeSupport mainPCS = new SelectionChangeSupport();
		final FixtureList mainList = new FixtureList(panel, model.getMap()
				.getPlayers());
		mainPCS.addSelectionChangeListener(mainList);
		panel.add(new JScrollPane(mainList));
		final DualTileButton dtb = new DualTileButton();
		// panel.add(new JScrollPane(dtb));
		panel.add(dtb);
		final ExplorationClickListener ecl = new ExplorationClickListener(
				model, direction, mainList);
		dtb.addActionListener(ecl);
		ecl.addSelectionChangeListener(this);
		ecl.addMovementCostListener(this);
		mainList.getModel().addListDataListener(
				new ExplorationListListener(model, mainList));
		final SelectionChangeSupport secPCS = new SelectionChangeSupport();
		final Iterator<Pair<IMap, File>> subMaps = model.getSubordinateMaps()
				.iterator();
		// ESCA-JAVA0177:
		final IPlayerCollection players;
		if (subMaps.hasNext()) {
			players = subMaps.next().first().getPlayers();
		} else {
			players = model.getMap().getPlayers();
		}
		final FixtureList secList = new FixtureList(panel, players);
		secPCS.addSelectionChangeListener(secList);
		panel.add(new JScrollPane(secList));
		mains.put(direction, mainPCS);
		buttons.put(direction, dtb);
		seconds.put(direction, secPCS);
	}

	/**
	 * Account for a movement cost.
	 *
	 * @param cost how much the movement cost
	 */
	@Override
	public void deduct(final int cost) {
		final String mpText = mpField.getText().trim();
		if (mpText != null && IsNumeric.isNumeric(mpText)) {
			int mpoints = Integer.parseInt(mpText);
			mpoints -= cost;
			mpField.setText(Integer.toString(mpoints));
		}
	}

	/**
	 * @param old the previously selected location
	 * @param newPoint the newly selected location
	 */
	@Override
	public void selectedPointChanged(@Nullable final Point old,
			final Point newPoint) {
		final Point selPoint = model.getSelectedUnitLocation();
		for (final Direction dir : Direction.values()) {
			if (dir == null) {
				continue;
			}
			final Point point = model.getDestination(selPoint, dir);
			final ITile tileOne = model.getMap().getTile(point);
			final Iterator<Pair<IMap, File>> subs = model
					.getSubordinateMaps().iterator();
			// ESCA-JAVA0177:
			final ITile tileTwo; // NOPMD
			if (subs.hasNext()) {
				tileTwo = subs.next().first().getTile(point);
			} else {
				tileTwo = new Tile(TileType.NotVisible); // NOPMD
			}
			mains.get(dir).fireChanges(null, null, null, tileOne);
			seconds.get(dir).fireChanges(null, null, null, tileTwo);
			buttons.get(dir).setTiles(tileOne, tileTwo);
			buttons.get(dir).repaint();
		}
		locLabel.setText("<html><body>Currently exploring "
				+ model.getSelectedUnitLocation()
				+ "; click a tile to explore it. "
				+ "Selected fixtures in its left-hand list "
				+ "will be 'discovered'.</body></html>");
	}

	/**
	 * @param old the previously selected tile
	 * @param newTile the newly selected tile
	 */
	@Override
	public void selectedTileChanged(@Nullable final ITile old, final ITile newTile) {
		// Everything is handled in selectedPointChanged().
	}

	/**
	 * @param list a listener to add
	 */
	@Override
	public final void addCompletionListener(final CompletionListener list) {
		cListeners.add(list);
	}

	/**
	 * @param list a listener to remove
	 */
	@Override
	public final void removeCompletionListener(final CompletionListener list) {
		cListeners.remove(list);
	}
}
