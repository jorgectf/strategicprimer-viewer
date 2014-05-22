package view.worker;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeNode;

import model.map.HasImage;
import model.map.fixtures.mobile.IUnit;
import model.map.fixtures.mobile.Worker;
import model.map.fixtures.mobile.worker.IJob;
import model.workermgmt.WorkerTreeModelAlt.KindNode;
import model.workermgmt.WorkerTreeModelAlt.UnitNode;

import org.eclipse.jdt.annotation.Nullable;

import util.ImageLoader;
import util.NullCleaner;
import util.TypesafeLogger;

/**
 * A cell renderer for the worker management tree.
 *
 * @author Jonathan Lovelace
 */
public class UnitMemberCellRenderer implements TreeCellRenderer {
	/**
	 * Logger.
	 */
	private static final Logger LOGGER = TypesafeLogger
			.getLogger(UnitMemberCellRenderer.class);
	/**
	 * the default fixture icon.
	 */
	private final Icon defaultFixtIcon = createDefaultFixtureIcon();

	/**
	 * Default renderer, for cases we don't know how to handle.
	 */
	private static final DefaultTreeCellRenderer DFLT =
			new DefaultTreeCellRenderer();
	/**
	 * The default background color when selected.
	 */
	private static final Color DEF_BKGD_SELECTED = NullCleaner
			.assertNotNull(DFLT.getBackgroundSelectionColor());
	/**
	 * The default background when not selected.
	 */
	private static final Color DEF_BKGD_NON_SEL = NullCleaner
			.assertNotNull(DFLT.getBackgroundNonSelectionColor());
	/**
	 * Whether we warn on certain ominous conditions.
	 */
	private final boolean warn;
	/**
	 * @param check whether to visually warn on certain ominous conditions
	 */
	public UnitMemberCellRenderer(final boolean check) {
		warn = check;
	}
	/**
	 * @param tree the tree being rendered
	 * @param value the object in the tree that's being rendered
	 * @param selected whether it's selected
	 * @param expanded whether it's an expanded node
	 * @param leaf whether it's a leaf node
	 * @param row its row in the tree
	 * @param hasFocus whether the tree has the focus
	 * @return a component representing the cell
	 */
	// ESCA-JAVA0138: We have to have this many params to override the
	// superclass method.
	@Override
	public Component getTreeCellRendererComponent(@Nullable final JTree tree,
			@Nullable final Object value, final boolean selected,
			final boolean expanded, final boolean leaf, final int row,
			final boolean hasFocus) {
		assert tree != null && value != null;
		final Component component =
				NullCleaner.assertNotNull(DFLT.getTreeCellRendererComponent(
						tree, value, selected, expanded, leaf, row, hasFocus));
		((DefaultTreeCellRenderer) component)
				.setBackgroundSelectionColor(DEF_BKGD_SELECTED);
		((DefaultTreeCellRenderer) component)
				.setBackgroundNonSelectionColor(DEF_BKGD_NON_SEL);
		final Object internal = getNodeValue(value);
		if (internal instanceof HasImage) {
			((JLabel) component).setIcon(getIcon((HasImage) internal));
		}
		if (internal instanceof Worker) {
			final Worker worker = (Worker) internal;
			// Assume at least a K in size.
			final StringBuilder builder = new StringBuilder(1024)
					.append("<html><p>");
			builder.append(worker.getName());
			if (!"human".equals(worker.getRace())) {
				builder.append(", a ").append(worker.getRace());
			}
			builder.append(jobCSL(worker));
			builder.append("</p></html>");
			((JLabel) component).setText(builder.toString());
		} else if (internal instanceof IUnit) {
			final IUnit unit = (IUnit) internal;
			((JLabel) component).setText(unit.getName());
			final String orders = unit.getOrders().toLowerCase();
			if (warn && orders.contains("fixme") && unit.iterator().hasNext()) {
				((DefaultTreeCellRenderer) component)
						.setBackgroundSelectionColor(Color.PINK);
				((DefaultTreeCellRenderer) component)
						.setBackgroundNonSelectionColor(Color.PINK);
			} else if (warn && orders.contains("todo")
					&& unit.iterator().hasNext()) {
				((DefaultTreeCellRenderer) component)
						.setBackgroundSelectionColor(Color.YELLOW);
				((DefaultTreeCellRenderer) component)
						.setBackgroundNonSelectionColor(Color.YELLOW);
			}
		} else if (warn && value instanceof KindNode) {
			boolean shouldWarn = false;
			boolean shouldErr = false;
			for (final TreeNode node : (KindNode) value) {
				if (node instanceof UnitNode) {
					final IUnit unit = (IUnit) NullCleaner
							.assertNotNull(getNodeValue(node));
					final String orders = unit.getOrders().toLowerCase();
					if (orders.contains("fixme") && unit.iterator().hasNext()) {
						shouldErr = true;
						break;
					} else if (orders.contains("todo")
							&& unit.iterator().hasNext()) {
						shouldWarn = true;
					}
				}
			}

			if (shouldErr) {
				((DefaultTreeCellRenderer) component)
						.setBackgroundSelectionColor(Color.PINK);
				((DefaultTreeCellRenderer) component)
						.setBackgroundNonSelectionColor(Color.PINK);
			} else if (shouldWarn) {
				((DefaultTreeCellRenderer) component)
						.setBackgroundSelectionColor(Color.YELLOW);
				((DefaultTreeCellRenderer) component)
						.setBackgroundNonSelectionColor(Color.YELLOW);
			}
		}
		return component;
	}

	/**
	 * @param value a node of the tree
	 * @return it, unless it's a DefaultMutableTreeNode, in which case return
	 *         the associated user object
	 */
	@Nullable private static Object getNodeValue(final Object value) {
		if (value instanceof DefaultMutableTreeNode) {
			return ((DefaultMutableTreeNode) value).getUserObject(); // NOPMD
		} else {
			return value;
		}
	}

	/**
	 * @param iter something containing Jobs
	 * @return a comma-separated list of them, in parentheses, prepended by a
	 *         space, if there are any.
	 */
	private static String jobCSL(final Iterable<IJob> iter) {
		if (iter.iterator().hasNext()) {
			final StringBuilder builder = new StringBuilder(100);
			builder.append(" (");
			boolean first = true;
			for (final IJob job : iter) {
				if (first) {
					first = false;
				} else {
					builder.append(", ");
				}
				builder.append(job.getName()).append(' ').append(job.getLevel());
			}
			builder.append(')');
			return NullCleaner.assertNotNull(builder.toString()); // NOPMD
		} else {
			return "";
		}
	}
	/**
	 * @param obj a HasImage object
	 * @return an icon representing it
	 */
	private Icon getIcon(final HasImage obj) {
		String image = obj.getImage();
		if (image.isEmpty()) {
			image = obj.getDefaultImage();
		}
		// FIXME: If getImage() references a file that's not there, try the
		// default image for that kind of fixture.
		try {
			return ImageLoader.getLoader().loadIcon(image); // NOPMD
		} catch (final FileNotFoundException e) {
			LOGGER.log(Level.SEVERE, "image file images/" + image
					+ " not found");
			LOGGER.log(Level.FINEST, "with stack trace", e);
			return defaultFixtIcon; // NOPMD
		} catch (final IOException e) {
			LOGGER.log(Level.SEVERE, "I/O error reading image", e);
			return defaultFixtIcon;
		}
	}

	/**
	 * @return the default icon for fixtures.
	 */
	private static Icon createDefaultFixtureIcon() {
		/**
		 * The margin we allow around the chit itself in the default image.
		 */
		final double margin = 0.15; // NOPMD
		final int imageSize = 24; // NOPMD
		final BufferedImage temp = new BufferedImage(imageSize, imageSize,
				BufferedImage.TYPE_INT_ARGB);
		final Graphics2D pen = temp.createGraphics();
		final Color saveColor = pen.getColor();
		pen.setColor(Color.RED);
		pen.fillRoundRect((int) Math.round(imageSize * margin) + 1,
				(int) Math.round(imageSize * margin) + 1,
				(int) Math.round(imageSize * (1.0 - margin * 2.0)),
				(int) Math.round(imageSize * (1.0 - margin * 2.0)),
				(int) Math.round(imageSize * (margin / 2.0)),
				(int) Math.round(imageSize * (margin / 2.0)));
		pen.setColor(saveColor);
		pen.fillRoundRect(
				((int) Math.round(imageSize / 2.0 - imageSize * margin)) + 1,
				((int) Math.round(imageSize / 2.0 - imageSize * margin)) + 1,
				(int) Math.round(imageSize * margin * 2.0),
				(int) Math.round(imageSize * margin * 2.0),
				(int) Math.round(imageSize * margin / 2.0),
				(int) Math.round(imageSize * margin / 2.0));
		return new ImageIcon(temp);

	}
	/**
	 * @return a String representation of the object
	 */
	@Override
	public String toString() {
		return "UnitMemberCellRenderer";
	}
}
