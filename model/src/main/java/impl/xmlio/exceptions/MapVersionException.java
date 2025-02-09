package impl.xmlio.exceptions;

import common.xmlio.SPFormatException;

import javax.xml.stream.events.StartElement;

/**
 * An exception to indicate that a map file specified a map version not supported by the code reading it.
 */
public class MapVersionException extends SPFormatException {
	private static final long serialVersionUID = 1L;
	private static String messageFragment(final int minimum, final int maximum) {
		if (minimum == maximum) {
			return ": must be " + minimum;
		} else {
			return String.format(": must be between %d and %d", minimum, maximum);
		}
	}

	/**
	 * @param context the current tag
	 * @param version the requested map version
	 * @param minimum the lowest version the code supports
	 * @param maximum the highest version the code supports
	 */
	public MapVersionException(final StartElement context, final int version, final int minimum, final int maximum) {
		super(String.format("Unsupported map version %d in tag %s%s", version,
			context.getName().getLocalPart(), messageFragment(minimum, maximum)),
			context.getLocation());
	}

	/**
	 * @param version the requested map version
	 * @param minimum the lowest version the code supports
	 * @param maximum the highest version the code supports
	 */
	private MapVersionException(final int version, final int minimum, final int maximum) {
		super(String.format("Unsupported SP map version %d%s", version,
			messageFragment(minimum, maximum)), -1, -1);
	}

	/**
	 * @param version the requested map version
	 * @param minimum the lowest version the code supports
	 * @param maximum the highest version the code supports
	 */
	public static MapVersionException nonXML(final int version, final int minimum, final int maximum) {
		return new MapVersionException(version, minimum, maximum);
	}
}
