package controller.map.cxml;

import controller.map.formatexceptions.DeprecatedPropertyException;
import controller.map.formatexceptions.MissingPropertyException;
import controller.map.formatexceptions.SPFormatException;
import controller.map.formatexceptions.SPMalformedInputException;
import controller.map.formatexceptions.UnwantedChildException;
import controller.map.iointerfaces.ISPReader;
import controller.map.misc.IDFactory;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import model.map.HasImage;
import model.map.HasPortrait;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import util.EqualsAny;
import util.NullCleaner;
import util.Warning;

import static java.lang.String.format;
import static util.NullCleaner.assertNotNull;

/**
 * A superclass to provide helper methods.
 *
 * This is part of the Strategic Primer assistive programs suite developed by Jonathan
 * Lovelace.
 *
 * Copyright (C) 2012-2015 Jonathan Lovelace
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of version 3 of the GNU General Public License as published by the Free Software
 * Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this
 * program. If not, see
 * <a href="http://www.gnu.org/licenses/">http://www.gnu.org/licenses/</a>.
 *
 * @param <T> a type parameter, since we now "implement" the interface
 * @author Jonathan Lovelace
 * @deprecated CompactXML is deprecated in favor of FluidXML
 */
@Deprecated
public abstract class AbstractCompactReader<@NonNull T>
		implements CompactReader<@NonNull T> {
	/**
	 * Do not instantiate directly.
	 */
	protected AbstractCompactReader() {
		// Nothing to do
	}

	/**
	 * Require that an element be one of the specified tags.
	 *
	 * @param element the element to check
	 * @param tags    the tags we accept here
	 */
	protected static void requireTag(final StartElement element,
									final String... tags) throws SPFormatException {
		if (!EqualsAny.equalsAny(
				assertNotNull(element.getName().getNamespaceURI()),
				ISPReader.NAMESPACE, XMLConstants.NULL_NS_URI)) {
			// TODO: Pass in parent tag? Somehow?
			throw new UnwantedChildException(new QName("unknown"), element,
													new IllegalArgumentException
															("Unrecognized namespace"));
		}
		final String localName = assertNotNull(element.getName().getLocalPart());
		final int line = element.getLocation().getLineNumber();
		if (!EqualsAny.equalsAny(localName, tags)) {
			// While we'd like tests to exercise this, we're always careful
			// to only call readers when we know they support the tag ...
			throw new UnwantedChildException(new QName("unknown"), element,
					new IllegalArgumentException(Stream.concat(Stream.of(format(
						"Unexpected tag %s on line %d, expected one of the following: ",
						localName, Integer.valueOf(line))), Stream.of(tags))
													.collect(Collectors.joining(", "))
			));
		}
	}

	/**
	 * Get a parameter from the XML.
	 *
	 * @param element the current tag
	 * @param param   the parameter to get
	 * @return the value for that parameter
	 * @throws SPFormatException if the tag doesn't have that parameter.
	 */
	protected static String getParameter(final StartElement element,
										final String param) throws SPFormatException {
		final Attribute attr = getAttributeByName(element, param);
		if (attr == null) {
			throw new MissingPropertyException(element, param);
		} else {
			final String value = attr.getValue();
			if (value == null) {
				throw new MissingPropertyException(element, param);
			} else {
				return value;
			}
		}
	}

	/**
	 * Get a parameter from the XML.
	 *
	 * @param element      the current tag
	 * @param param        the parameter to get
	 * @param defaultValue the value to return if the tag doesn't have that parameter
	 * @return the value for that parameter
	 */
	protected static String getParameter(final StartElement element,
										final String param, final String defaultValue) {
		final Attribute attr = getAttributeByName(element, param);
		if (attr == null) {
			return defaultValue;
		} else {
			return NullCleaner.valueOrDefault(attr.getValue(), defaultValue);
		}
	}

	/**
	 * Require a non-empty parameter.
	 *
	 * @param element   the current tag
	 * @param param     the parameter to require
	 * @param mandatory whether this is a requirement, or merely a recommendation.
	 * @param warner    the Warning instance to use for the warning.
	 * @throws SPFormatException if mandatory and missing
	 */
	protected static void requireNonEmptyParameter(final StartElement element,
													final String param,
													final boolean mandatory,
													final Warning warner)
			throws SPFormatException {
		if (getParameter(element, param, "").isEmpty()) {
			final SPFormatException except = new MissingPropertyException(element,
																				param);
			if (mandatory) {
				throw except;
			} else {
				warner.warn(except);
			}
		}
	}

	/**
	 * Move along the stream until we hit an end element matching the start-element we're
	 * parsing, but object to any start elements.
	 *
	 * @param tag    the tag we're currently parsing
	 * @param reader the XML stream we're reading from
	 * @throws SPFormatException on unwanted child
	 */
	protected static void spinUntilEnd(final QName tag,
										final Iterable<XMLEvent> reader)
			throws SPFormatException {
		for (final XMLEvent event : reader) {
			if (event.isStartElement() && EqualsAny.equalsAny(
					assertNotNull(
							event.asStartElement().getName().getNamespaceURI()),
					ISPReader.NAMESPACE, XMLConstants.NULL_NS_URI)) {
				throw new UnwantedChildException(tag,
						assertNotNull(event.asStartElement()));
			} else if (event.isEndElement()
								&& tag.equals(event.asEndElement().getName())) {
				break;
			}
		}
	}

	/**
	 * If the specified tag has an ID as a property, return it; otherwise warn about its
	 * absence and generate one.
	 *
	 * @param element   the tag we're working with
	 * @param warner    the Warning instance to send the warning on if the tag doesn't
	 *                  specify an ID
	 * @param idFactory the factory to register an existing ID with or get a new one from
	 * @return the ID the tag has if it has one, or otherwise a generated one.
	 * @throws SPFormatException on SP format problems reading the attribute
	 */
	protected static int getOrGenerateID(final StartElement element,
										final Warning warner, final IDFactory idFactory)
			throws SPFormatException {
		if (hasParameter(element, "id")) {
			try {
				return idFactory.register(warner, NumberFormat.getIntegerInstance()
												.parse(getParameter(element, "id"))
												.intValue());
			} catch (final NumberFormatException | ParseException except) {
				throw new MissingPropertyException(element, "id", except);
			}
		} else {
			warner.warn(new MissingPropertyException(element, "id"));
			return idFactory.createID();
		}
	}
	/**
	 * @param element the current tag
	 * @param param the parameter we want
	 * @return it if it's present in either the default namespace or our namespace, or
	 * null if not present
	 */
	@Nullable
	protected static Attribute getAttributeByName(final StartElement element,
													final String param) {
		final Attribute retval =
				element.getAttributeByName(new QName(ISPReader.NAMESPACE, param));
		if (retval == null) {
			return element.getAttributeByName(new QName(param));
		} else {
			return retval;
		}
	}
	/**
	 * @param element the current tag
	 * @param param   the parameter we want
	 * @return whether the tag has that parameter
	 */
	protected static boolean hasParameter(final StartElement element,
											final String param) {
		return getAttributeByName(element, param) != null;
	}

	/**
	 * @param element    the current tag
	 * @param preferred  the preferred name of the parameter
	 * @param deprecated the deprecated name of the parameter
	 * @param warner     the warning instance to use
	 * @return the value of the parameter, gotten from the preferred form if it has it,
	 * and from the deprecated form if the preferred form isn't there but it is.
	 * @throws SPFormatException if the element doesn't have that attribute
	 */
	protected static String getParamWithDeprecatedForm(final StartElement element,
														final String preferred,
														final String deprecated,
														final Warning warner)
			throws SPFormatException {
		final Attribute prefProp = getAttributeByName(element, preferred);
		final Attribute deprecatedProp = getAttributeByName(element, deprecated);
		final MissingPropertyException exception =
				new MissingPropertyException(element, preferred);
		if ((prefProp == null) && (deprecatedProp == null)) {
			throw exception;
		} else if (prefProp == null) {
			//noinspection ConstantConditions
			assert deprecatedProp != null;
			warner.warn(new DeprecatedPropertyException(element, deprecated, preferred));
			final String value = deprecatedProp.getValue();
			if (value == null) {
				throw exception;
			}
			return value;
		} else {
			final String prefValue = prefProp.getValue();
			if (prefValue == null) {
				if (deprecatedProp == null) {
					throw exception;
				} else {
					final String deprecatedValue = deprecatedProp.getValue();
					if (deprecatedValue == null) {
						throw exception;
					} else {
						return deprecatedValue;
					}
				}
			} else {
				return prefValue;
			}
		}
	}

	/**
	 * @param ostream the stream to write the tabs to
	 * @param tabs a non-negative integer: how many tabs to add to the stream
	 * @throws IOException on I/O error writing to ostream
	 */
	protected static void indent(final Appendable ostream, final int tabs)
			throws IOException {
		for (int i = 0; i < tabs; i++) {
			ostream.append('\t');
		}
	}

	/**
	 * @param obj an object being written out that might have a custom image
	 * @return the XML for the image if it does, or the empty string if not
	 */
	@SuppressWarnings("TypeMayBeWeakened")
	protected static String imageXML(final HasImage obj) {
		final String image = obj.getImage();
		if (image.isEmpty() || image.equals(obj.getDefaultImage())) {
			return "";
		} else {
			return " image=\"" + image + '"';
		}
	}
	/**
	 * @param obj an object being written out that might have a custom portrait
	 * @return the XML for the portrait if it does, or the empty string if not
	 */
	@SuppressWarnings("TypeMayBeWeakened")
	protected static String portraitXML(final HasPortrait obj) {
		final String portrait = obj.getPortrait();
		if (portrait.isEmpty()) {
			return "";
		} else {
			// TODO: Test that portraits get written
			return " portrait=\"" + portrait + '"';
		}
	}
	/**
	 * A parser for numeric data.
	 */
	private static final NumberFormat NUM_PARSER =
			assertNotNull(NumberFormat
																				.getIntegerInstance());


	/**
	 * Parse an integer.
	 *
	 * @param str  the text to parse
	 * @param location the current location in the document
	 * @return the result of parsing the text
	 * @throws SPFormatException if the string is non-numeric or otherwise malformed
	 */
	private static int parseInt(final String str, final Location location)
			throws SPFormatException {
		try {
			return NUM_PARSER.parse(str).intValue();
		} catch (final ParseException e) {
			throw new SPMalformedInputException(location, e);
		}
	}
	/**
	 * Parse an integer parameter.
	 *
	 * @param tag       the tag to get the parameter from
	 * @param parameter the name of the parameter
	 * @return the result of parsing the text
	 * @throws SPFormatException if the tag doesn't have that parameter or if its
	 * value is non-numeric or otherwise malformed
	 */
	protected static int getIntegerParameter(final StartElement tag,
												final String parameter)
			throws SPFormatException {
		return parseInt(getParameter(tag, parameter),
				assertNotNull(tag.getLocation()));
	}

	/**
	 * Parse an integer parameter.
	 *
	 * @param tag          the tag to get the parameter from
	 * @param parameter    the name of the parameter
	 * @param defaultValue the default value to return if the parameter is missing
	 * @return the result of parsing the text
	 * @throws SPFormatException if the parameter's value is non-numeric or otherwise
	 *                           malformed
	 */
	protected static int getIntegerParameter(final StartElement tag,
											final String parameter,
											final int defaultValue)
			throws SPFormatException {
		final Attribute attr = getAttributeByName(tag, parameter);
		if (attr == null) {
			return defaultValue;
		}
		final String val = attr.getValue();
		if ((val == null) || val.isEmpty()) {
			return defaultValue;
		} else {
			return parseInt(val, assertNotNull(tag.getLocation()));
		}
	}
	/**
	 * Write the necessary number of tab characters and a tag. Does not write the right
	 * bracket to close the tag.
	 * @param ostream the stream to write to
	 * @param tag the tag to write
	 * @param indent the indentation level
	 * @throws IOException on I/O error writing to stream
	 */
	@SuppressWarnings("TypeMayBeWeakened")
	protected static void writeTag(final Appendable ostream, final String tag,
								final int indent) throws IOException {
		indent(ostream, indent);
		ostream.append('<');
		ostream.append(tag);
		if (indent == 0) {
			ostream.append(" xmlns=\"");
			ostream.append(ISPReader.NAMESPACE);
			ostream.append("\"");
		}
	}
}
