package common.map.fixtures.towns;

import common.map.HasKind;
import org.jetbrains.annotations.Nullable;

import common.map.TileFixture;
import common.map.HasPortrait;
import common.map.HasName;
import common.map.HasOwner;

/**
 * An interface for towns and similar fixtures.
 */
public interface ITownFixture extends TileFixture, HasName, HasOwner, HasPortrait, HasKind {
	/**
	 * The status of the town.
	 */
	TownStatus getStatus();

	/**
	 * The size of the town.
	 */
	TownSize getTownSize();

	/**
	 * A description of what kind of "town" this is.
	 */
	@Override
	String getKind();

	/**
	 * A summary of the town's contents.
	 */
	@Nullable
	CommunityStats getPopulation();
}
