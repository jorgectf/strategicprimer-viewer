package view.worker;

import java.beans.PropertyChangeListener;

import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import model.map.fixtures.mobile.worker.Job;
import model.workermgmt.JobsListModel;
import util.PropertyChangeSource;
/**
 * A visual list of a worker's Jobs. We also handle listening for selection changes.
 * @author Jonathan Lovelace
 *
 */
public class JobsList extends JList<Job> implements PropertyChangeSource,
		ListSelectionListener {
	/**
	 * Constructor.
	 * @param listener something to listen to us
	 * @param sources what our model should listen to
	 */
	public JobsList(final PropertyChangeListener listener,
			final PropertyChangeSource... sources) {
		setModel(new JobsListModel(sources));
		addPropertyChangeListener(listener);
		addListSelectionListener(this);
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	}
	/**
	 * @param evt an event indicating the selection changed.
	 */
	@Override
	public void valueChanged(final ListSelectionEvent evt) {
		firePropertyChange("job", null, getSelectedValue());
	}
}
