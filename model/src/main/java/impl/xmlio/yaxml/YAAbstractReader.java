package impl.xmlio.yaxml;

import org.jetbrains.annotations.Nullable;

import org.javatuples.Pair;

import java.text.NumberFormat;
import java.text.ParseException;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.Attribute;

import common.idreg.IDRegistrar;
import common.map.Point;
import common.map.HasImage;
import static impl.xmlio.ISPReader.SP_NAMESPACE;
import common.xmlio.Warning;
import common.xmlio.SPFormatException;
import impl.xmlio.exceptions.UnwantedChildException;
import impl.xmlio.exceptions.MissingPropertyException;
import impl.xmlio.exceptions.DeprecatedPropertyException;
import impl.xmlio.exceptions.UnsupportedPropertyException;
import impl.xmlio.exceptions.UnsupportedTagException;
import lovelace.util.IteratorWrapper;
import java.util.regex.Pattern;
import static impl.xmlio.ISPReader.SP_NAMESPACE;

import java.util.List;
import java.util.Collections;
import lovelace.util.ThrowingConsumer;
import java.util.Arrays;
import java.util.stream.Stream;
import java.util.stream.Collectors;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Set;
import static impl.xmlio.ISPReader.FUTURE_TAGS;
import java.util.stream.StreamSupport;

/**
 * A superclass for YAXML reader classes to provide helper methods.
 */
abstract class YAAbstractReader<Item, Value> implements YAReader<Item, Value> {
	/**
	 * A parser for numeric data, so integers can contain commas.
	 */
	protected static final NumberFormat NUM_PARSER = NumberFormat.getIntegerInstance();

	/**
	 * Patterns to match XML metacharacters, and their quoted forms.
	 */
	protected static final List<Pair<Pattern, String>> QUOTING =
		Collections.unmodifiableList(Arrays.asList(
			Pair.with(Pattern.compile("&"), "&amp;"),
			Pair.with(Pattern.compile("<"), "&lt;"),
			Pair.with(Pattern.compile(">"), "&gt;")));

	protected static final Pair<Pattern, String> QUOTE_DOUBLE_QUOTE =
			Pair.with(Pattern.compile("\""), "&quot;");

	protected static final Pair<Pattern, String> QUOTE_SINGLE_QUOTE =
		Pair.with(Pattern.compile("'"), "&apos;");

	/**
	 * Whether the given tag is in a namespace we support.
	 */
	protected static boolean isSupportedNamespace(QName tag) {
		return SP_NAMESPACE.equals(tag.getNamespaceURI()) ||
			XMLConstants.NULL_NS_URI.equals(tag.getNamespaceURI());
	}

	/**
	 * Require that an element be one of the specified tags.
	 */
	protected static void requireTag(StartElement element, QName parent, String... tags)
			throws SPFormatException {
		if (!isSupportedNamespace(element.getName())) {
			throw UnwantedChildException.unexpectedNamespace(parent, element);
		}
		String elementTag = element.getName().getLocalPart();
		if (Stream.of(tags).noneMatch(elementTag::equalsIgnoreCase)) {
			// While we'd like tests to exercise this, we're always careful to only call
			// readers when we know they support the tag ...
			throw UnwantedChildException.listingExpectedTags(parent, element, tags);
		}
	}

	/**
	 * Create a {@link QName} for the given tag in our namespace.
	 */
	private static QName qname(String tag) {
		return new QName(SP_NAMESPACE, tag);
	}

	/**
	 * Get an attribute by name from the given tag, if it's there.
	 */
	@Nullable
	private static Attribute getAttributeByName(StartElement element, String parameter) {
		Attribute retval = element.getAttributeByName(qname(parameter));
		if (retval == null) {
			return element.getAttributeByName(new QName(parameter));
		} else {
			return retval;
		}
	}

	/**
	 * Read a parameter (aka attribute aka property) from an XML tag.
	 */
	protected static String getParameter(StartElement element, String param) throws SPFormatException {
		Attribute attr = getAttributeByName(element, param);
		if (attr != null) {
			String retval = attr.getValue();
			if (retval != null) {
				return retval;
			}
		}
		throw new MissingPropertyException(element, param);
	}

	/**
	 * Read a parameter (aka attribute aka property) from an XML tag.
	 */
	protected static String getParameter(StartElement element, String param, String defaultValue) {
		Attribute attr = getAttributeByName(element, param);
		if (attr != null) {
			String retval = attr.getValue();
			if (retval != null) {
				return retval;
			}
		}
		return defaultValue;
	}

	/**
	 * Whether the given XML event is an end element matching the given tag.
	 */
	protected static boolean isMatchingEnd(QName tag, XMLEvent event) {
		return event instanceof EndElement && tag.equals(((EndElement) event).getName());
	}

	/**
	 * Whether the given tag has the given parameter.
	 */
	protected static boolean hasParameter(StartElement element, String param) {
		return getAttributeByName(element, param) != null;
	}

	/**
	 * Append the given number of tabs to the stream.
	 */
	protected static void indent(ThrowingConsumer<String, IOException> ostream, int tabs) throws IOException {
		ostream.accept(String.join("", Collections.nCopies(tabs, "\t")));
	}

	/**
	 * Replace XML meta-characters in a string with their equivalents.
	 */
	protected static String simpleQuote(String text) {
		String retval = text;
		for (Pair<Pattern, String> pair : QUOTING) {
			retval = pair.getValue0().matcher(retval).replaceAll(pair.getValue1());
		}
		return retval;
	}

	/**
	 * Replace XML meta-characters in a string with their equivalents.
	 *
	 * @param delimeter The character that will mark the end of the string
	 * as far as XML is concerned.  If a single or double quote, that
	 * character will be encoded every time it occurs in the string; if a
	 * greater-than sign or an equal sign, both types of quotes will be; if
	 * a less-than sign, neither will be.
	 */
	protected static String simpleQuote(String text, char delimiter) {
		String retval = simpleQuote(text);
		if (delimiter == '"' || delimiter == '>' || delimiter == '=') {
			retval = QUOTE_DOUBLE_QUOTE.getValue0().matcher(retval)
				.replaceAll(QUOTE_DOUBLE_QUOTE.getValue1());
		}
		if (delimiter == '\'' || delimiter == '>' || delimiter == '=') {
			retval = QUOTE_SINGLE_QUOTE.getValue0().matcher(retval)
				.replaceAll(QUOTE_SINGLE_QUOTE.getValue1());
		}
		return retval;
	}

	/**
	 * Write a property to XML.
	 */
	protected static void writeProperty(ThrowingConsumer<String, IOException> ostream, String name, String val)
			throws IOException {
		ostream.accept(String.format(" %s=\"%s\"", simpleQuote(name, '='), simpleQuote(val, '"')));
	}

	/**
	 * Write a property to XML.
	 */
	protected static void writeProperty(ThrowingConsumer<String, IOException> ostream, String name, int val)
			throws IOException {
		writeProperty(ostream, name, Integer.toString(val));
	}

	/**
	 * Write a property to XML only if its value is nonempty.
	 */
	protected static void writeNonemptyProperty(ThrowingConsumer<String, IOException> ostream, String name, String val)
			throws IOException {
		if (!val.isEmpty()) {
			writeProperty(ostream, name, val);
		}
	}

	/**
	 * Write the image property to XML if the object's image is nonempty
	 * and differs from the default.
	 */
	protected static void writeImageXML(ThrowingConsumer<String, IOException> ostream, HasImage obj)
			throws IOException {
		String image = obj.getImage();
		if (!image.equals(obj.getDefaultImage())) {
			writeNonemptyProperty(ostream, "image", image);
		}
	}

	/**
	 * Parse an integer. We use {@link NumberFormat} rather than {@link
	 * Integer#parseInt} because we want to allow commas in the input.
	 *
	 * TODO: Inline this into the caller or pass in information that lets
	 * us throw a more meaningful exception, so we can get rid of
	 * SPMalformedInputException
	 *
	 * @throws ParseException on non-numeric input
	 * @param location The current location in the document
	 */
	protected static int parseInt(String string, Location location) throws ParseException {
		return NUM_PARSER.parse(string).intValue();
	}

	/**
	 * Read a parameter from XML whose value must be an integer.
	 */
	protected static int getIntegerParameter(StartElement element, String parameter)
			throws SPFormatException {
		Attribute attr = getAttributeByName(element, parameter);
		if (attr != null) {
			String retval = attr.getValue();
			if (retval != null) {
				try {
					return parseInt(retval, element.getLocation());
				} catch (ParseException except) {
					throw new MissingPropertyException(element, parameter, except);
				}
			}
		}
		throw new MissingPropertyException(element, parameter);
	}

	/**
	 * Read a parameter from XML whose value must be an integer.
	 */
	protected static int getIntegerParameter(StartElement element, String parameter, int defaultValue)
			throws SPFormatException {
		Attribute attr = getAttributeByName(element, parameter);
		if (attr != null) {
			String retval = attr.getValue();
			if (retval != null) {
				try {
					return parseInt(retval, element.getLocation());
				} catch (ParseException except) {
					throw new MissingPropertyException(element, parameter, except);
				}
			}
		}
		return defaultValue;
	}

	/**
	 * Read a parameter from XML whose value can be an Integer or a Decimal.
	 */
	protected static Number getNumericParameter(StartElement element, String parameter)
			throws SPFormatException {
		if (hasParameter(element, parameter)) {
			String paramString = getParameter(element, parameter);
			if (paramString.contains(".")) {
				try {
					return new BigDecimal(paramString);
				} catch (NumberFormatException except) {
					throw new MissingPropertyException(element, parameter, except);
				}
			} else {
				try {
					return Integer.parseInt(paramString);
				} catch (NumberFormatException except) {
					throw new MissingPropertyException(element, parameter, except);
				}
			}
		} else {
			throw new MissingPropertyException(element, parameter);
		}
	}

	/**
	 * Read a parameter from XML whose value can be an Integer or a Decimal.
	 */
	protected static Number getNumericParameter(StartElement element, String parameter,
			Number defaultValue) throws SPFormatException {
		if (hasParameter(element, parameter)) {
			String paramString = getParameter(element, parameter);
			if (paramString.contains(".")) {
				try {
					return new BigDecimal(paramString);
				} catch (NumberFormatException except) {
					throw new MissingPropertyException(element, parameter, except);
				}
			} else {
				try {
					return Integer.parseInt(paramString);
				} catch (NumberFormatException except) {
					throw new MissingPropertyException(element, parameter, except);
				}
			}
		} else {
			return defaultValue;
		}
	}

	/**
	 * Write the necessary number of tab characters and a tag. Does not
	 * write the right-bracket to close the tag. If `tabs` is 0, emit a
	 * namespace declaration as well.
	 */
	protected static void writeTag(ThrowingConsumer<String, IOException> ostream, String tag, int tabs)
			throws IOException {
		indent(ostream, tabs);
		ostream.accept(String.format("<%s", simpleQuote(tag, '>')));
		if (tabs == 0) {
			ostream.accept(String.format(" xmlns=\"%s\"", SP_NAMESPACE));
		}
	}

	/**
	 * Close a tag with a right-bracket and add a newline.
	 */
	protected static void finishParentTag(ThrowingConsumer<String, IOException> ostream) throws IOException {
		ostream.accept(">" + System.lineSeparator());
	}

	/**
	 * Close a 'leaf' tag and add a newline.
	 */
	protected static void closeLeafTag(ThrowingConsumer<String, IOException> ostream) throws IOException {
		ostream.accept(" />" + System.lineSeparator());
	}

	/**
	 * Write a closing tag to the stream, optionally indented, and followed by a newline.
	 */
	protected static void closeTag(ThrowingConsumer<String, IOException> ostream, int tabs, String tag)
			throws IOException {
		if (tabs > 0) {
			indent(ostream, tabs);
		}
		ostream.accept(String.format("</%s>%n", simpleQuote(tag, '>')));
	}

	/**
	 * Parse a Point from a tag's properties.
	 */
	protected static Point parsePoint(StartElement element) throws SPFormatException {
		return new Point(getIntegerParameter(element, "row"),
			getIntegerParameter(element, "column"));
	}

	/**
	 * The Warning instance to use.
	 */
	private Warning warner;
	/**
	 * The factory for ID numbers
	 */
	private IDRegistrar idf;
	
	protected YAAbstractReader(Warning warning, IDRegistrar idRegistrar) {
		warner = warning;
		idf = idRegistrar;
	}

	/**
	 * Warn about a not-yet-(fully-)supported tag.
	 */
	protected void warnFutureTag(StartElement tag) {
		warner.handle(UnsupportedTagException.future(tag));
	}

	/**
	 * Advance the stream until we hit an end element matching the given
	 * name, but object to any start elements.
	 */
	protected void spinUntilEnd(QName tag, Iterable<XMLEvent> reader, String... futureTags)
			throws SPFormatException {
		for (XMLEvent event : reader) {
			if (event instanceof StartElement &&
					isSupportedNamespace(((StartElement) event).getName())) {
				if (FUTURE_TAGS.stream().anyMatch(((StartElement) event).getName()
						.getLocalPart()::equalsIgnoreCase)) {
					warner.handle(new UnwantedChildException(tag,
						(StartElement) event));
				} else {
					throw new UnwantedChildException(tag, (StartElement) event);
				}
			} else if (isMatchingEnd(tag, event)) {
				break;
			}
		}
	}

	/**
	 * Read a parameter from XML whose value must be a boolean.
	 *
	 * Note that unlike in Ceylon, we can't use {@link
	 * Boolean#parseBoolean} because it doesn't object to non-boolean
	 * input, just returns false for everything but "true".
	 */
	protected boolean getBooleanParameter(StartElement element, String parameter)
			throws SPFormatException {
		Attribute attr = getAttributeByName(element, parameter);
		if (attr != null) {
			String val = attr.getValue();
			if ("true".equalsIgnoreCase(val)) {
				return true;
			} else if ("false".equalsIgnoreCase(val)) {
				return false;
			} else {
				throw new MissingPropertyException(element, parameter,
					new IllegalArgumentException("Boolean can only be true or false"));
			}
		}
		throw new MissingPropertyException(element, parameter);
	}

	/**
	 * Read a parameter from XML whose value must be a boolean.
	 */
	protected boolean getBooleanParameter(StartElement element, String parameter, boolean defaultValue)
			throws SPFormatException {
		Attribute attr = getAttributeByName(element, parameter);
		if (attr != null) {
			String val = attr.getValue();
			if (val != null && !val.isEmpty()) {
				if ("true".equalsIgnoreCase(val)) {
					return true;
				} else if ("false".equalsIgnoreCase(val)) {
					return false;
				} else {
					warner.handle(new MissingPropertyException(element, parameter,
						new IllegalArgumentException(
							"Boolean can only be true or false")));
				}
			}
		}
		return defaultValue;
	}

	/**
	 * Require that a parameter be present and non-empty.
	 *
	 * TODO: Try to avoid Boolean parameters
	 */
	protected void requireNonEmptyParameter(StartElement element, String parameter,
			boolean mandatory) throws SPFormatException {
		if (getParameter(element, parameter, "").isEmpty()) {
			SPFormatException except = new MissingPropertyException(element, parameter);
			if (mandatory) {
				throw except;
			} else {
				warner.handle(except);
			}
		}
	}

	/**
	 * Register the specified ID number, noting that it came from the
	 * specified location, and return it.
	 */
	protected int registerID(Integer id, Location location) {
		return idf.register(id, warner, location);
	}

	/**
	 * If the specified tag has an ID as a property, return it; otherwise,
	 * warn about its absence and generate one.
	 */
	protected int getOrGenerateID(StartElement element) throws SPFormatException {
		if (hasParameter(element, "id")) {
			return registerID(getIntegerParameter(element, "id"), element.getLocation());
		} else {
			warner.handle(new MissingPropertyException(element, "id"));
			return idf.createID();
		}
	}

	/**
	 * Get a parameter from an element in its preferred form, if present,
	 * or in a deprecated form, in which case fire a warning.
	 */
	protected String getParamWithDeprecatedForm(StartElement element, String preferred,
			String deprecated) throws SPFormatException {
		Attribute preferredProperty = getAttributeByName(element, preferred);
		if (preferredProperty != null) {
			String retval = preferredProperty.getValue();
			if (retval != null) {
				return retval;
			}
		}
		Attribute deprecatedProperty = getAttributeByName(element, deprecated);
		if (deprecatedProperty != null) {
			String retval = deprecatedProperty.getValue();
			if (retval != null) {
				warner.handle(new DeprecatedPropertyException(element, deprecated,
					preferred));
				return retval;
			}
		}
		throw new MissingPropertyException(element, preferred);
	}

	/**
	 * Warn if any unsupported attribute is on this tag.
	 */
	protected void expectAttributes(StartElement element, String... attributes) {
		Set<String> local = Stream.of(attributes).map(String::toLowerCase)
			.collect(Collectors.toSet());
		for (String attribute : StreamSupport.stream(
				new IteratorWrapper<Attribute>(
					element.getAttributes()).spliterator(), true)
				.map(Attribute::getName).filter(YAAbstractReader::isSupportedNamespace)
				.map(QName::getLocalPart).map(String::toLowerCase)
				.collect(Collectors.toList())) {
			if (!local.contains(attribute)) {
				warner.handle(new UnsupportedPropertyException(element, attribute));
			}
		}
	}
}
