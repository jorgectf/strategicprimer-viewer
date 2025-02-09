package lovelace.util;

import javax.swing.JEditorPane;

import java.awt.Color;

/**
 * A label that can easily be written (appended) to.
 */
public final class StreamingLabel extends JEditorPane {
	private static final long serialVersionUID = 1;
	/**
	 * Possible colors for use by text in a {@link StreamingLabel}.
	 */
	public enum LabelTextColor {
		YELLOW("yellow"), WHITE("white"), RED("red"), GREEN("green"), BLACK("black");
		private final String colorName;
		public String getColorName() {
			return colorName;
		}
		LabelTextColor(final String color) {
			colorName = color;
		}
	}

	private final StringBuilder buffer = new StringBuilder();

	public StreamingLabel() {
		super("text/html", "<html><body bgcolor=\"#000000\"><p>&nbsp;</p></body></html>");
		setEditable(false);
		setBackground(Color.BLACK);
		setOpaque(true);
	}

	/**
	 * Add text to the label.
	 */
	public void append(final String string) {
		buffer.append(string);
		setText("<html><body bgcolor=\"#000000\">" + buffer + "</body></html>");
		repaint();
	}

	/**
	 * Add text to the label, followed by a newline.
	 */
	public void appendLine(final String string) {
		buffer.append(string);
		buffer.append("<br />");
		setText("<html><body bgcolor=\"#000000\">" + buffer + "</body></html>");
		repaint();
	}
}
