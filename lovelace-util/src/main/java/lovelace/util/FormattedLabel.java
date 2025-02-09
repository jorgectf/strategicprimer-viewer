package lovelace.util;

import javax.swing.JLabel;

/**
 * A JLabel that takes a format string in its constructor and later takes format-string arguments to produce its text.
 */
public class FormattedLabel extends JLabel {
	private static final long serialVersionUID = 1;
	private final String formatString;
	/**
	 * @param formatString The format string to use to produce the label's text.
	 * @param defaultArguments The arguments to plug into the format string
	 * to produce the label's initial text.
	 */
	public FormattedLabel(final String formatString, final Object... defaultArguments) {
		super(String.format(formatString, defaultArguments));
		this.formatString = formatString;
	}
	/**
	 * Change the arguments and regenerate the label's text.
	 */
	public void setArguments(final Object... arguments) {
		setText(String.format(formatString, arguments));
	}
}
