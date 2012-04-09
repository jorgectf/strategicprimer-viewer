package controller.map.simplexml.node;

import model.map.PlayerCollection;
import model.map.fixtures.Mountain;
import util.Warning;
import controller.map.SPFormatException;
/**
 * A Node to produce a Mountain.
 * @author Jonathan Lovelace
 *
 */
public class MountainNode extends AbstractFixtureNode<Mountain> {
	/**
	 * Constructor.
	 */
	public MountainNode() {
		super(Mountain.class);
	}
	/**
	 * @param players ignored
	 * @param warner a Warning instance to use for warnings
	 * @return the Mountain this represents
	 * @throws SPFormatException never
	 */
	@Override
	public Mountain produce(final PlayerCollection players, final Warning warner) throws SPFormatException {
		return new Mountain();
	}
	
	/**
	 * Check that the node is valid. A Mountain is valid if it has no children.
	 * TODO: should it have attributes?
	 * 
	 * @param warner a Warning instance to use for warnings
	 * @throws SPFormatException if the node isn't valid.
	 */
	@Override
	public void checkNode(final Warning warner) throws SPFormatException {
		if (iterator().hasNext()) {
			throw new SPFormatException("Mountain shouldn't have children", getLine());
		}
	}
	/**
	 * @param property the name of a property
	 * @return whether this kind of node can use the property
	 */
	@Override
	public boolean canUse(final String property) {
		return false;
	}
	/**
	 * @return a String representation of the node
	 */
	@Override
	public String toString() {
		return "MountainNode";
	}
}
