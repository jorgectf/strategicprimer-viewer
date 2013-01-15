package model.workermgmt;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.DefaultListModel;

import model.map.fixtures.mobile.worker.Job;
import model.map.fixtures.mobile.worker.Skill;
import util.PropertyChangeSource;

/**
 * A list model for a list of the skills associated with a Job.
 * @author Jonathan Lovelace
 */
public class SkillListModel extends DefaultListModel<Skill> implements
		PropertyChangeListener {
	/**
	 * The current Job.
	 */
	private Job job = null;
	/**
	 * Constructor.
	 * @param sources property-change sources to listen to.
	 */
	public SkillListModel(final PropertyChangeSource... sources) {
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
			if (evt.getNewValue() instanceof Job) {
				if (job == null || !job.equals(evt.getNewValue())) {
					clear();
					job = (Job) evt.getNewValue();
					for (Skill skill : job) {
						addElement(skill);
					}
				}
			} else if (job != null && evt.getNewValue() == null) {
				job = (Job) evt.getNewValue();
				clear();
			}
		}
	}
}
