package model.map.fixtures.towns;


/**
 * An abandoned, ruined, or burned-out city.
 *
 * @author Jonathan Lovelace
 *
 */
public final class CityEvent extends AbstractTownEvent {
	/**
	 * Version UID for serialization.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * Constructor.
	 *
	 * @param tStatus The status of the city
	 * @param tSize The size of the city
	 * @param discdc The DC to discover it.
	 * @param tName the name of the town, fortress, or city
	 * @param idNum the ID number.
	 */
	public CityEvent(final TownStatus tStatus, final TownSize tSize,
			final int discdc, final String tName, final int idNum) {
		super(TownKind.City, tStatus, tSize, tName);
		dc = discdc;
		id = idNum;
	}

	/**
	 * The DC to discover the city. TODO: Should perhaps be mutable.
	 */
	private final int dc; // NOPMD

	/**
	 *
	 * @return the DC to discover the event.
	 */
	@Override
	public int getDC() {
		return dc;
	}

	/**
	 * ID number.
	 */
	private final int id; // NOPMD

	/**
	 * @return a UID for the fixture.
	 */
	@Override
	public int getID() {
		return id;
	}
}
