package model.workermgmt;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import javax.swing.DefaultListModel;

import model.map.fixtures.mobile.worker.Job;
import model.map.fixtures.mobile.worker.Skill;
import util.PropertyChangeSource;

/**
 * A list model for a list of the skills associated with a Job.
 * @author Jonathan Lovelace
 */
public class SkillListModel extends DefaultListModel<Skill> implements
		PropertyChangeListener, PropertyChangeSource {
	/**
	 * The current Job.
	 */
	private Job job = null;
	/**
	 * Constructor.
	 * @param sources property-change sources to listen to.
	 */
	public SkillListModel(final PropertyChangeSource... sources) {
		if (sources.length == 0) {
			throw new IllegalStateException("No sources given");
		}
		for (final PropertyChangeSource source : sources) {
			source.addPropertyChangeListener(this);
		}
	}
	/**
	 * Handle a property change.
	 * @param evt the event to handle.
	 */
	@Override
	public void propertyChange(final PropertyChangeEvent evt) {
		if ("job".equalsIgnoreCase(evt.getPropertyName())) {
			if (evt.getNewValue() == null || evt.getNewValue() instanceof Job) {
				handleNewJob((Job) evt.getNewValue());
			}
		} else if ("add".equalsIgnoreCase(evt.getPropertyName()) && job != null) {
			final Skill skill = new Skill(evt.getNewValue().toString(), 0, 0);
			job.addSkill(skill);
			addElement(skill);
			pcs.firePropertyChange("finished", null, skill);
		} else if ("level".equalsIgnoreCase(evt.getPropertyName())) {
			fireContentsChanged(evt.getSource(), 0, getSize());
		}
	}
	/**
	 * Handle the "job" property changing.
	 * @param newValue the new value
	 */
	private void handleNewJob(final Job newValue) {
		if (newValue == null) {
			if (job != null) {
				job = newValue;
				clear();
			}
		} else {
			if (job == null || !job.equals(newValue)) {
				clear();
				job = newValue;
				for (Skill skill : job) {
					addElement(skill);
				}
				pcs.firePropertyChange("finished", null, isEmpty() ? Integer.valueOf(-1) : Integer.valueOf(0));
			}
		}
	}
	/**
	 * Our delegate for property-change handling.
	 */
	private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
	/**
	 * @param list a listener to listen to us
	 */
	@Override
	public void addPropertyChangeListener(final PropertyChangeListener list) {
		pcs.addPropertyChangeListener(list);
	}
	/**
	 * @param list a listener to stop listenng to us
	 */
	@Override
	public void removePropertyChangeListener(final PropertyChangeListener list) {
		pcs.removePropertyChangeListener(list);
	}
}
