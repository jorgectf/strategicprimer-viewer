package model.listeners;

/**
 * An interface for objects that can indicate the selected location and tile
 * changed.
 *
 * @author Jonathan Lovelace
 *
 */
public interface SelectionChangeSource {
	/**
	 * @param list a listener to add
	 */
	void addSelectionChangeListener(SelectionChangeListener list);

	/**
	 * @param list a listener to remove
	 */
	void removeSelectionChangeListener(SelectionChangeListener list);
}
