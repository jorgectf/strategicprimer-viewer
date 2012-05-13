package view.map.details;

import javax.swing.JPanel;

import model.map.TileFixture;
import model.map.events.IEvent;
import model.map.events.MineralEvent;
import model.map.events.StoneEvent;
import model.map.fixtures.Animal;
import model.map.fixtures.CacheFixture;
import model.map.fixtures.Forest;
import model.map.fixtures.Fortress;
import model.map.fixtures.Ground;
import model.map.fixtures.Grove;
import model.map.fixtures.Hill;
import model.map.fixtures.Meadow;
import model.map.fixtures.Mine;
import model.map.fixtures.Mountain;
import model.map.fixtures.Oasis;
import model.map.fixtures.RiverFixture;
import model.map.fixtures.Sandbar;
import model.map.fixtures.Shrub;
import model.map.fixtures.TextFixture;
import model.map.fixtures.Unit;
import model.map.fixtures.Village;
import util.EqualsAny;
import view.map.main.SelectionListener;

/**
 * A panel for displaying chits.
 * 
 * @author Jonathan Lovelace
 * 
 */
public class ChitPanel extends JPanel {
	/**
	 * Constructor.
	 * 
	 * @param list
	 *            the selection listener.
	 */
	public ChitPanel(final SelectionListener list) {
		super();
		listener = list;
	}

	/**
	 * The selection listener.
	 */
	private final SelectionListener listener;

	/**
	 * Clear all chits off the panel, and clear the selection.
	 */
	public void clear() {
		listener.clearSelection();
		removeAll();
	}

	/**
	 * Add a chit for a TileFixture. TODO: Should a CacheFixture actually have
	 * its own Chit with a tool-tip advising that the cache's contents were most
	 * likely removed by the explorer?
	 * 
	 * @param fix
	 *            the Fixture to add a chit for
	 */
	@SuppressWarnings("unchecked")
	public void add(final TileFixture fix) {
		if (fix instanceof Fortress) {
			add(new FortChit((Fortress) fix, listener));
		} else if (fix instanceof Unit) {
			add(new UnitChit((Unit) fix, listener));
		} else if (EqualsAny.equalsAny(fix.getClass(), Mine.class, Grove.class,
				Oasis.class, Shrub.class, MineralEvent.class, StoneEvent.class,
				Mountain.class, Forest.class, Animal.class, Meadow.class,
				Hill.class, Village.class, CacheFixture.class, Sandbar.class, TextFixture.class)) {
			add(new SimpleChit(fix, listener));
		} else if (fix instanceof IEvent) {
			add(new EventChit((IEvent) fix, listener));
		} else if (fix instanceof RiverFixture) {
			add(new RiverChit((RiverFixture) fix, listener));
		} else if (fix instanceof Ground) {
			add(new GroundChit((Ground) fix, listener));
		} else {
			throw new IllegalStateException("We're missing a case here: " + fix.getClass().getSimpleName());
		}
	}
}
