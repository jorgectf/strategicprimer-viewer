package controller.map.simplexml.node;

import model.map.PlayerCollection;
import model.map.fixtures.Dragon;
import util.Warning;
import controller.map.SPFormatException;

/**
 * A Node to represent a dragon.
 * @author Jonathan Lovelace
 *
 */
public class DragonNode extends AbstractFixtureNode<Dragon> {
	/**
	 * Constructor.
	 */
	public DragonNode() {
		super(Dragon.class);
	}
	/**
	 * @param players ignored
	 * @param warner a Warning instance to use for warnings
	 * @return the dragon this represents
	 * @throws SPFormatException if missing a required attribute
	 */
	@Override
	public Dragon produce(final PlayerCollection players, final Warning warner) throws SPFormatException {
		return new Dragon(getProperty("kind"));
	}
	/**
	 * Check the node for invalid data. A Dragon is valid if it has no children and has a "kind" property.
	 * @param warner a Warning instance to use for warnings
	 * @throws SPFormatException if the node contains invalid data
	 */
	@Override
	public void checkNode(final Warning warner) throws SPFormatException {
		if (iterator().hasNext()) {
			throw new SPFormatException("Dragon shouldn't have children", getLine());
		} else if (!hasProperty("kind")) {
			throw new SPFormatException("Dragon must have \"kind\" property", getLine());
		}
	}
	/**
	 * @param property the name of a property
	 * @return whether this kind of node can use the property
	 */
	@Override
	public boolean canUse(final String property) {
		return "kind".equals(property);
	}
	/**
	 * @return a String representation of the node.
	 */
	@Override
	public String toString() {
		return "DragonNode";
	}
}
