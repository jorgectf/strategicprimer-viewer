package drivers.resourceadding;

import java.util.stream.StreamSupport;
import java.util.stream.Collectors;
import lovelace.util.LovelaceLogger;
import org.jetbrains.annotations.Nullable;

import common.map.fixtures.towns.IMutableFortress;

import drivers.common.SimpleMultiMapModel;
import drivers.common.IDriverModel;

import common.map.fixtures.FortressMember;
import common.map.fixtures.IMutableResourcePile;
import common.map.fixtures.IResourcePile;
import common.map.fixtures.Quantity;
import common.map.fixtures.ResourcePileImpl;

import common.map.Player;
import common.map.IMutableMapNG;

import java.math.BigDecimal;

/**
 * A driver model for resource-entering drivers.
 */
/* package */ class ResourceManagementDriverModel extends SimpleMultiMapModel {
	// TODO: Make these private and expose them as fromMap() and
	// fromDriverModel() static factory methods, as they were (in effect)
	// in Ceylon?
	public ResourceManagementDriverModel(final IMutableMapNG map) {
		super(map);
	}

	public ResourceManagementDriverModel(final IDriverModel driverModel) {
		super(driverModel);
	}

	/**
	 * All the players in all the maps.
	 */
	public Iterable<Player> getPlayers() {
		return streamAllMaps().flatMap(m -> StreamSupport.stream(m.getPlayers().spliterator(), false))
			.collect(Collectors.toSet());
	}

	/**
	 * Add a resource to a player's HQ.
	 *
	 * TODO: Use the "HQ or, failing that, any other fortress" algorithm used by other driver models.
	 */
	public void addResource(final FortressMember resource, final Player player) {
		for (final IMutableMapNG map : getRestrictedAllMaps()) {
			final Player mapPlayer = map.getCurrentPlayer();
			// Operate on maps where the current player is independent OR matches
			// ("independent" also including negative ID)
			if (mapPlayer.isIndependent() || mapPlayer.getPlayerId() < 0 ||
					mapPlayer.getPlayerId() == player.getPlayerId()) {
				final IMutableFortress fortress = map.streamAllFixtures()
						.filter(IMutableFortress.class::isInstance)
						.map(IMutableFortress.class::cast)
						.filter(f -> "HQ".equals(f.getName()))
						.filter(f -> mapPlayer.getPlayerId() ==
							f.owner().getPlayerId())
						.findAny().orElse(null);
				if (fortress == null) {
					LovelaceLogger.warning("Didn't find HQ for %s", mapPlayer);
				} else {
					fortress.addMember(resource);
					map.setModified(true);
				}
			} else {
				LovelaceLogger.debug("Skipping map because current player isn't independent and doesn't match");
			}
		}
	}

	public IResourcePile addResourcePile(final Player player, final int id, final String kind, final String resource,
	                                     final BigDecimal quantity, final String units, final @Nullable Integer created) {
		final IMutableResourcePile pile = new ResourcePileImpl(id, kind, resource,
			new Quantity(quantity, units));
		if (created != null) {
			pile.setCreated(created);
		}
		addResource(pile, player);
		return pile;
	}

	/**
	 * Get the current player. If none is current, returns null.
	 */
	public @Nullable Player getCurrentPlayer() {
		return StreamSupport.stream(getPlayers().spliterator(), false)
			.filter(Player::isCurrent).findAny().orElse(null);
	}
}
