package view.exploration;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;

import model.exploration.IExplorationModel;
import model.exploration.IExplorationModel.Direction;
import model.map.IMap;
import model.map.Tile;
import model.map.TileFixture;
import model.map.fixtures.mobile.SimpleMovement.TraversalImpossibleException;
import util.Pair;
import util.PropertyChangeSource;
import view.map.details.FixtureList;

/**
 * The listener for clicks on tile buttons indicating movement.
 * @author Jonathan Lovelace
 *
 */
public final class ExplorationClickListener implements ActionListener, PropertyChangeSource {
	/**
	 * The exploration model.
	 */
	private final IExplorationModel model;
	/**
	 * The direction this button is from the currently selected tile.
	 */
	private final Direction direction;
	/**
	 * The list of fixtures on this tile in the main map.
	 */
	private final FixtureList list;
	/**
	 * Constructor.
	 * @param emodel the exploration model
	 * @param direct what direction this button is from the center.
	 * @param mainList the list of fixtures on this tile in the main map.
	 */
	public ExplorationClickListener(final IExplorationModel emodel,
			final Direction direct, final FixtureList mainList) {
		model = emodel;
		direction = direct;
		list = mainList;
	}
	/**
	 * @param evt the event to handle.
	 *
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(final ActionEvent evt) {
		try {
			final List<TileFixture> fixtures = list
					.getSelectedValuesList();
			model.move(direction);
			for (final Pair<IMap, String> pair : model
					.getSubordinateMaps()) {
				final IMap map = pair.first();
				final Tile tile = map.getTile(model
						.getSelectedUnitLocation());
				for (final TileFixture fix : fixtures) {
					tile.addFixture(fix);
				}
			}
		} catch (TraversalImpossibleException except) {
			pcs.firePropertyChange("point", null,
					model.getSelectedUnitLocation());
			pcs.firePropertyChange("cost",
					Integer.valueOf(0), Integer.valueOf(1));
		}
	}
	/**
	 * A helper to handle notifying listeners of property changes.
	 */
	private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
	/**
	 * @param listener a new listener to listen to us
	 */
	@Override
	public void addPropertyChangeListener(final PropertyChangeListener listener) {
		pcs.addPropertyChangeListener(listener);
	}
	/**
	 * @param listener a former listener that wants to stop listening
	 */
	@Override
	public void removePropertyChangeListener(final PropertyChangeListener listener) {
		pcs.removePropertyChangeListener(listener);
	}
}