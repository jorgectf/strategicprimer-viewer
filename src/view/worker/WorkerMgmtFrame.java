package view.worker;

import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.text.View;

import model.map.Player;
import model.map.fixtures.UnitMember;
import model.map.fixtures.mobile.Unit;
import model.map.fixtures.mobile.worker.Job;
import model.map.fixtures.mobile.worker.Skill;
import model.viewer.MapModel;
import model.workermgmt.SkillListModel;
import util.PropertyChangeSource;
import view.util.AddRemovePanel;
/**
 * A GUI to let a user manage workers.
 * @author Jonathan Lovelace
 *
 */
public class WorkerMgmtFrame extends JFrame implements ItemListener,
		PropertyChangeListener, PropertyChangeSource {
	/**
	 * The map model containing the data we're working from.
	 */
	private final MapModel model;
	/**
	 * A drop-down list listing the players in the map.
	 */
	private final JComboBox<Player> players = new JComboBox<Player>();
	/**
	 * A non-drop-down list of the skills associated with that job.
	 */
	private final JList<Skill> skills = new JList<Skill>();
	/**
	 * Constructor.
	 * @param source the model containing the data to work from
	 */
	public WorkerMgmtFrame(final MapModel source) {
		model = source;
		model.addPropertyChangeListener(this);
		setLayout(new BoxLayout(getContentPane(), BoxLayout.LINE_AXIS));

		final JPanel panelOne = new JPanel();
		panelOne.setLayout(new BoxLayout(panelOne, BoxLayout.PAGE_AXIS));
		players.addItemListener(this);
		final JLabel playerLabel = new JLabel(htmlize("Current Player:"));
		panelOne.add(playerLabel);
		panelOne.add(players);
		final JLabel unitLabel = new JLabel(htmlize("Player's Units:"));
		panelOne.add(unitLabel);
		final JList<Unit> units = new UnitList(source, this, source, this);
		panelOne.add(units);
		add(panelOne);

		final JPanel panelTwo = new JPanel();
		panelTwo.setLayout(new BoxLayout(panelTwo, BoxLayout.PAGE_AXIS));
		final JLabel memberLabel = new JLabel(htmlize("Selected Unit's Members:"));
		panelTwo.add(memberLabel);
		final JList<UnitMember> members = new UnitMemberList(this);
		panelTwo.add(members);
		final StatsLabel statsLabel = new StatsLabel(this);
		panelTwo.add(statsLabel);
		add(panelTwo);

		final JPanel panelThree = new JPanel();
		panelThree.setLayout(new BoxLayout(panelThree, BoxLayout.PAGE_AXIS));
		final AddRemovePanel jarp = new AddRemovePanel(false);
		final JList<Job> jobs = new JobsList(this, this, jarp);
		final JLabel jobsLabel = new JLabel(htmlize("Worker's Jobs:"));
		panelThree.add(jobsLabel);
		panelThree.add(jobs);
		panelThree.add(jarp);
		final AddRemovePanel sarp = new AddRemovePanel(false);
		skills.setModel(new SkillListModel(this, sarp));
		skills.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
//		skills.addListSelectionListener(this);
		final JLabel skillsLabel = new JLabel(htmlize("Skills in selected Job:"));
		panelThree.add(skillsLabel);
		panelThree.add(skills);
		panelThree.add(sarp);
		add(panelThree);

		addPropertyChangeListener(this);
		firePropertyChange("map", null, null);
		removePropertyChangeListener(this);

		setMinimumSize(new Dimension(640, 480));
		final List<JComponent> lists = new ArrayList<JComponent>();
		lists.add(panelOne);
		lists.add(panelTwo);
		lists.add(panelThree);
		lists.add(players);
		lists.add(jobs);
		lists.add(skills);
		lists.add(units);
		lists.add(members);
		lists.add(playerLabel);
		lists.add(unitLabel);
		lists.add(memberLabel);
		lists.add(jobsLabel);
		lists.add(skillsLabel);
		lists.add(statsLabel);
		getContentPane().addComponentListener(new ComponentAdapter() {
			/**
			 * Adjust the size of the sub-panels when this is resized.
			 * @param evt the event being handled
			 */
			@Override
			public void componentResized(final ComponentEvent evt) {
				final int width = getWidth() / 3;
				final int minHeight = 20; // NOPMD
				final int maxHeight = getHeight();
				for (JComponent list : lists) {
					if (list instanceof JComboBox) {
						list.setMaximumSize(new Dimension(width, minHeight));
						list.setPreferredSize(new Dimension(width, minHeight));
					} else if (list instanceof JList) {
						list.setMaximumSize(new Dimension(width, maxHeight));
						list.setMinimumSize(new Dimension(width, minHeight));
					} else if (list instanceof JLabel) {
						final Dimension dim = getComponentPreferredSize(list, width);
						list.setMinimumSize(dim);
						list.setPreferredSize(dim);
						list.setMaximumSize(dim);
					} else if (list instanceof JPanel) {
						list.setMaximumSize(new Dimension(width, maxHeight));
						list.setPreferredSize(new Dimension(width, maxHeight));
						list.setMinimumSize(new Dimension(width, maxHeight));
					}
				}
			}
		});
	}
	/**
	 * Handle a property change.
	 * @param evt the property-change event to handle
	 */
	@Override
	public void propertyChange(final PropertyChangeEvent evt) {
		if ("map".equals(evt.getPropertyName())) {
			players.removeAllItems();
			for (Player player : model.getMainMap().getPlayers()) {
				players.addItem(player);
			}
		} else if (!equals(evt.getSource())) {
			for (PropertyChangeListener listener : getPropertyChangeListeners(evt.getPropertyName())) {
				listener.propertyChange(evt);
			}
		}
	}
	/**
	 * @param evt an event indicating an item's changed in one of the combo-boxes we listen to
	 */
	@Override
	public void itemStateChanged(final ItemEvent evt) {
		if (players.equals(evt.getSource())) {
			firePropertyChange("player", null, players.getSelectedItem());
		}
	}
	/**
	 * Turn a string into left-aligned HTML.
	 * @param string a string
	 * @return it wrapped in HTML code that should make it left-aligned.
	 */
	private static String htmlize(final String string) {
		return "<html><p align=\"left\">" + string + "</p></html>";
	}
	/**
	 * Get a label's size given a fixed width.
	 * Adapted from http://blog.nobel-joergensen.com/2009/01/18/changing-preferred-size-of-a-html-jlabel/
	 * @param component the component we're laying out
	 * @param width the width we're working within
	 * @return the "ideal" dimensions for the component
	 */
	public static Dimension getComponentPreferredSize(
			final JComponent component, final int width) {
	final View view = (View) component
				.getClientProperty(javax.swing.plaf.basic.BasicHTML.propertyKey);
		view.setSize(width, 0);
		final int wid = (int) Math.ceil(view.getPreferredSpan(View.X_AXIS));
		final int height = (int) Math.ceil(view.getPreferredSpan(View.Y_AXIS));
		return new Dimension(wid, height);
	}
}
