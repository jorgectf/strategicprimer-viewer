package model.map;

import controller.map.cxml.CompactXMLWriter;
import controller.map.formatexceptions.DeprecatedPropertyException;
import controller.map.formatexceptions.MissingChildException;
import controller.map.formatexceptions.MissingPropertyException;
import controller.map.formatexceptions.SPFormatException;
import controller.map.formatexceptions.UnsupportedTagException;
import controller.map.formatexceptions.UnwantedChildException;
import controller.map.iointerfaces.IMapReader;
import controller.map.iointerfaces.ISPReader;
import controller.map.iointerfaces.TestReaderFactory;
import controller.map.readerng.ReaderAdapter;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.StreamSupport;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import util.FatalWarningException;
import util.NullCleaner;
import util.TypesafeLogger;
import util.Warning;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * An abstract base class for this helper method.
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
 * @author Jonathan Lovelace
 */
@SuppressWarnings({"ElementOnlyUsedFromTestCode", "ClassHasNoToStringMethod"})
public abstract class BaseTestFixtureSerialization { // NOPMD
	/**
	 * The "filename" to pass to the readers.
	 */
	private static final File FAKE_FILENAME = new File("");
	/**
	 * An instance of the previous-generation reader to test against.
	 */
	private final ISPReader oldReader = TestReaderFactory.createOldReader();
	/**
	 * An instance of the current-generation reader to test against.
	 */
	private final ISPReader newReader = TestReaderFactory.createNewReader();
	/**
	 * Logger.
	 */
	private static final Logger LOGGER =
			TypesafeLogger.getLogger(BaseTestFixtureSerialization.class);

	/**
	 * Assert that reading the given XML will produce an UnwantedChildException. If it's
	 * only supposed to be a warning, assert that it'll pass with warnings disabled but
	 * fail with warnings made fatal. This version runs against both the old and the new
	 * reader.
	 *
	 * @param xml         the XML to read
	 * @param desideratum the class it would produce if it weren't erroneous
	 * @param warning     whether this is supposed to be a warning only
	 * @throws SPFormatException  on unexpected SP format error
	 * @throws XMLStreamException on XML format error
	 */
	protected final void assertUnwantedChild(final String xml,
									final Class<?> desideratum, final boolean warning)
			throws XMLStreamException, SPFormatException {
		assertUnwantedChild(oldReader, xml, desideratum, warning);
		assertUnwantedChild(newReader, xml, desideratum, warning);
	}

	/**
	 * Assert that reading the given XML will produce an UnsupportedTagException. If it's
	 * only supposed to be a warning, assert that it'll pass with warnings disabled but
	 * fail with warnings made fatal. This version uses both old and new readers.
	 *
	 * @param xml         the XML to read
	 * @param desideratum the class it would produce if it weren't erroneous
	 * @param tag         the unsupported tag
	 * @param warning     whether this is supposed to be a warning only
	 * @throws SPFormatException  on unexpected SP format error
	 * @throws XMLStreamException on XML format error
	 */
	protected final void assertUnsupportedTag(final String xml,
										final Class<?> desideratum, final String tag,
										final boolean warning)
			throws XMLStreamException, SPFormatException {
		assertUnsupportedTag(oldReader, xml, desideratum, tag, warning);
		assertUnsupportedTag(newReader, xml, desideratum, tag, warning);
	}

	/**
	 * Assert that reading the given XML will produce an UnsupportedTagException. If it's
	 * only supposed to be a warning, assert that it'll pass with warnings disabled but
	 * fail with warnings made fatal.
	 *
	 * @param reader      the reader to do the reading
	 * @param xml         the XML to read
	 * @param desideratum the class it would produce if it weren't erroneous
	 * @param tag         the unsupported tag
	 * @param warning     whether this is supposed to be a warning only
	 * @throws SPFormatException  on unexpected SP format error
	 * @throws XMLStreamException on XML format error
	 */
	private static void assertUnsupportedTag(final ISPReader reader,
											final String xml,
											final Class<?> desideratum,
											final String tag,
											final boolean warning)
			throws XMLStreamException, SPFormatException {
		if (warning) {
			try (StringReader sreader = new StringReader(xml)) {
				reader.readXML(FAKE_FILENAME, sreader, desideratum,
						Warning.Ignore);
			}
			try (StringReader sreader = new StringReader(xml)) {
				reader.readXML(FAKE_FILENAME, sreader,
						desideratum, Warning.Die);
			} catch (final FatalWarningException except) {
				final Throwable cause = except.getCause();
				assertTrue("Unsupported tag",
						cause instanceof UnsupportedTagException);
				assertEquals("The tag we expected", new QName(tag),
						((UnsupportedTagException) cause).getTag());
			}
		} else {
			try (StringReader sreader = new StringReader(xml)) {
				reader.readXML(FAKE_FILENAME, sreader, desideratum,
						Warning.Ignore);
				fail("Expected an UnsupportedTagException");
			} catch (final UnsupportedTagException except) {
				assertEquals("The tag we expected", tag, except.getTag().getLocalPart());
			}
		}
	}

	/**
	 * Assert that reading the given XML will produce an UnwantedChildException. If it's
	 * only supposed to be a warning, assert that it'll pass with warnings disabled but
	 * fail with warnings made fatal.
	 *
	 * @param reader      the reader to do the reading
	 * @param xml         the XML to read
	 * @param desideratum the class it would produce if it weren't erroneous
	 * @param warning     whether this is supposed to be a warning only
	 * @throws SPFormatException  on unexpected SP format error
	 * @throws XMLStreamException on XML format error
	 */
	private static void assertUnwantedChild(final ISPReader reader,
											final String xml, final Class<?> desideratum,
											final boolean warning)
			throws XMLStreamException, SPFormatException {
		if (warning) {
			try (StringReader sreader = new StringReader(xml)) {
				reader.readXML(FAKE_FILENAME, sreader, desideratum,
						Warning.Ignore);
			}
			try (StringReader sreader = new StringReader(xml)) {
				reader.readXML(FAKE_FILENAME, sreader, desideratum,
						Warning.Die);
				fail("We were expecting an UnwantedChildException");
			} catch (final FatalWarningException except) {
				assertTrue("Unwanted child",
						except.getCause() instanceof UnwantedChildException);
			}
		} else {
			try (StringReader sreader = new StringReader(xml)) {
				reader.readXML(FAKE_FILENAME, sreader, desideratum,
						Warning.Ignore);
				fail("We were expecting an UnwantedChildException");
			} catch (final UnwantedChildException except) {
				assertNotNull("Dummy check", except);
			}
		}
	}

	/**
	 * Assert that reading the given XML will give a MissingPropertyException. If it's
	 * only supposed to be a warning, assert that it'll pass with warnings disabled but
	 * object with them made fatal. This version tests both old and new readers.
	 *
	 * @param xml         the XML to read
	 * @param desideratum the class it would produce if it weren't erroneous
	 * @param property    the missing property
	 * @param warning     whether this is supposed to be only a warning
	 * @throws SPFormatException  on unexpected SP format error
	 * @throws XMLStreamException on XML format error
	 */
	protected final void assertMissingProperty(final String xml,
										final Class<?> desideratum,
										final String property,
										final boolean warning)
			throws XMLStreamException, SPFormatException {
		assertMissingProperty(oldReader, xml, desideratum, property, warning);
		assertMissingProperty(newReader, xml, desideratum, property, warning);
	}

	/**
	 * Assert that reading the given XML will give a MissingPropertyException. If it's
	 * only supposed to be a warning, assert that it'll pass with warnings disabled but
	 * object with them made fatal.
	 *
	 * @param reader      the reader to do the reading
	 * @param xml         the XML to read
	 * @param desideratum the class it would produce if it weren't erroneous
	 * @param property    the missing property
	 * @param warning     whether this is supposed to be only a warning
	 * @throws SPFormatException  on unexpected SP format error
	 * @throws XMLStreamException on XML format error
	 */
	private static void assertMissingProperty(final ISPReader reader,
											final String xml,
											final Class<?> desideratum,
											final String property,
											final boolean warning)
			throws XMLStreamException, SPFormatException {
		if (warning) {
			try (StringReader sreader = new StringReader(xml)) {
				reader.readXML(FAKE_FILENAME, sreader, desideratum,
						Warning.Ignore);
			}
			try (StringReader sreader = new StringReader(xml)) {
				reader.readXML(FAKE_FILENAME, sreader, desideratum,
						Warning.Die);
				fail("We were expecting a MissingParameterException");
			} catch (final FatalWarningException except) {
				final Throwable cause = except.getCause();
				assertTrue("Missing property",
						cause instanceof MissingPropertyException);
				assertEquals(
						"The missing property should be the one we're expecting",
						property, ((MissingPropertyException) cause).getParam());
			}
		} else {
			try (StringReader sreader = new StringReader(xml)) {
				reader.readXML(FAKE_FILENAME, sreader, desideratum,
						Warning.Ignore);
			} catch (final MissingPropertyException except) {
				assertEquals(
						"Missing property should be the one we're expecting",
						property, except.getParam());
			}
		}
	}

	/**
	 * Assert that reading the given XML will give a DeprecatedPropertyException. If it's
	 * only supposed to be a warning, assert that it'll pass with warnings disabled but
	 * object with them made fatal. This version tests both old and new readers.
	 *
	 * @param xml         the XML to read
	 * @param desideratum the class it would produce if it weren't erroneous
	 * @param deprecated  the deprecated property
	 * @param warning     whether this is supposed to be only a warning
	 * @throws SPFormatException  on unexpected SP format error
	 * @throws XMLStreamException on XML format error
	 */
	protected final void assertDeprecatedProperty(final String xml,
											final Class<?> desideratum,
											final String deprecated,
											final boolean warning)
			throws XMLStreamException, SPFormatException {
		assertDeprecatedProperty(oldReader, xml, desideratum, deprecated,
				warning);
		assertDeprecatedProperty(newReader, xml, desideratum, deprecated,
				warning);
	}

	/**
	 * Assert that reading the given XML will give a DeprecatedPropertyException. If it's
	 * only supposed to be a warning, assert that it'll pass with warnings disabled but
	 * object with them made fatal.
	 *
	 * @param reader      the reader to do the reading
	 * @param xml         the XML to read
	 * @param desideratum the class it would produce if it weren't erroneous
	 * @param deprecated  the deprecated property
	 * @param warning     whether this is supposed to be only a warning
	 * @throws SPFormatException  on unexpected SP format error
	 * @throws XMLStreamException on XML format error
	 */
	private static void assertDeprecatedProperty(final ISPReader reader,
												final String xml,
												final Class<?> desideratum,
												final String deprecated,
												final boolean warning)
			throws XMLStreamException, SPFormatException {
		if (warning) {
			try (StringReader sreader = new StringReader(xml)) {
				reader.readXML(FAKE_FILENAME, sreader, desideratum,
						Warning.Ignore);
			}
			try (StringReader sreader = new StringReader(xml)) {
				reader.readXML(FAKE_FILENAME, sreader, desideratum,
						Warning.Die);
				fail("We were expecting a MissingParameterException");
			} catch (final FatalWarningException except) {
				final Throwable cause = except.getCause();
				assertTrue("Missing property",
						cause instanceof DeprecatedPropertyException);
				assertEquals(
						"The missing property should be the one we're expecting",
						deprecated,
						((DeprecatedPropertyException) cause).getOld());
			}
		} else {
			try (StringReader sreader = new StringReader(xml)) {
				reader.readXML(FAKE_FILENAME, sreader, desideratum,
						Warning.Ignore);
			} catch (final DeprecatedPropertyException except) {
				assertEquals(
						"Missing property should be the one we're expecting",
						deprecated, except.getOld());
			}
		}
	}

	/**
	 * Assert that the serialized form of the given object will deserialize without error
	 * using both old and new readers.
	 *
	 * @param message the message to use
	 * @param obj     the object to serialize
	 * @throws SPFormatException  on SP XML problem
	 * @throws XMLStreamException on XML reading problem
	 * @throws IOException        on I/O error creating serialized form
	 */
	protected final void assertSerialization(final String message, final Object obj)
			throws XMLStreamException, SPFormatException, IOException {
		assertSerialization(message, obj, Warning.Die);
	}

	/**
	 * Assert that the serialized form of the given object will deserialize without error
	 * using both the old and new readers.
	 *
	 * @param message the message to use
	 * @param obj     the object to serialize
	 * @param warning the warning instance to use
	 * @throws SPFormatException  on SP XML problem
	 * @throws XMLStreamException on XML reading problem
	 * @throws IOException        on I/O error creating serialized form
	 */
	protected final void assertSerialization(final String message, final Object obj,
											final Warning warning)
			throws XMLStreamException, SPFormatException, IOException {
		assertSerialization(message, oldReader, obj, warning);
		assertSerialization(message, newReader, obj, warning);
	}

	/**
	 * Assert that the serialized form of the given object will deserialize without error
	 * using both the reflection and non-reflection methods.
	 *
	 * @param message the message to use
	 * @param reader  the reader to parse the serialized form
	 * @param obj     the object to serialize
	 * @param warner  the warning instance to use
	 * @throws SPFormatException  on SP XML problem
	 * @throws XMLStreamException on XML reading problem
	 * @throws IOException        on I/O error creating serialized form
	 */
	private static void assertSerialization(final String message, final ISPReader reader,
											final Object obj, final Warning warner)
			throws XMLStreamException, SPFormatException, IOException {
		try (StringReader sreader = new StringReader(createSerializedForm(obj, true))) {
			assertEquals(message, obj,
					reader.readXML(FAKE_FILENAME, sreader, obj.getClass(), warner));
		}
		try (StringReader sreader = new StringReader(createSerializedForm(obj, true))) {
			assertEquals(message, obj,
					reader.readXML(FAKE_FILENAME, sreader, obj.getClass(), warner));
		}

		try (StringReader sreader = new StringReader(createSerializedForm(obj, false))) {
			assertEquals(message, obj,
					reader.readXML(FAKE_FILENAME, sreader, obj.getClass(), warner));
		}
		try (StringReader sreader = new StringReader(createSerializedForm(obj, false))) {
			assertEquals(message, obj,
					reader.readXML(FAKE_FILENAME, sreader, obj.getClass(), warner));
		}
	}

	/**
	 * Assert that a deprecated idiom deserializes properly if warnings are ignored, but
	 * is warned about.
	 *
	 * @param message  the message to pass to JUnit
	 * @param expected the object we expect the deserialized form to equal
	 * @param xml      the serialized form
	 * @param property the deprecated property
	 * @throws SPFormatException  on SP format error
	 * @throws XMLStreamException on XML format error
	 */
	protected final void assertDeprecatedDeserialization(final String message,
														final Object expected,
														final String xml,
														final String property)
			throws XMLStreamException, SPFormatException {
		try (StringReader sreader = new StringReader(xml)) {
			assertEquals(message, expected, oldReader.readXML(FAKE_FILENAME, sreader,
					expected.getClass(), Warning.Ignore));
		}
		try (StringReader sreader = new StringReader(xml)) {
			assertEquals(message, expected, oldReader.readXML(FAKE_FILENAME, sreader,
					expected.getClass(), Warning.Ignore));
		}
		try (StringReader sreader = new StringReader(xml)) {
			assertEquals(message, expected, newReader.readXML(FAKE_FILENAME, sreader,
					expected.getClass(), Warning.Ignore));
		}
		try (StringReader sreader = new StringReader(xml)) {
			assertEquals(message, expected, newReader.readXML(FAKE_FILENAME, sreader,
					expected.getClass(), Warning.Ignore));
		}
		assertDeprecatedProperty(xml, expected.getClass(), property, true);
	}

	/**
	 * Assert that a serialized form with a recommended but not required property missing
	 * deserializes properly if warnings are ignored, but is warned about.
	 *
	 * @param message  the message to pass to JUnit
	 * @param expected the object we expect the deserialized form to equal
	 * @param xml      the serialized form
	 * @param property the missing property
	 * @throws SPFormatException  on SP format error
	 * @throws XMLStreamException on XML format error
	 */
	protected final void assertMissingPropertyDeserialization(final String message,
															final Object expected,
															final String xml,
															final String property)
			throws XMLStreamException, SPFormatException {
		try (StringReader sreader = new StringReader(xml)) {
			assertEquals(message, expected, oldReader.readXML(FAKE_FILENAME, sreader,
					expected.getClass(), Warning.Ignore));
		}
		try (StringReader sreader = new StringReader(xml)) {
			assertEquals(message, expected, oldReader.readXML(FAKE_FILENAME, sreader,
					expected.getClass(), Warning.Ignore));
		}
		try (StringReader sreader = new StringReader(xml)) {
			assertEquals(message, expected, newReader.readXML(FAKE_FILENAME, sreader,
					expected.getClass(), Warning.Ignore));
		}
		try (StringReader sreader = new StringReader(xml)) {
			assertEquals(message, expected, newReader.readXML(FAKE_FILENAME, sreader,
					expected.getClass(), Warning.Ignore));
		}
		assertMissingProperty(xml, expected.getClass(), property, true);
	}

	/**
	 * Assert that a "forward idiom"---an idiom that we do not yet produce, but want to
	 * accept---will be deserialized properly by both readers, both with and without
	 * reflection.
	 *
	 * @param message  the message to pass to JUnit
	 * @param expected the object we expect the deserialized form to equal
	 * @param xml      the serialized form
	 * @throws SPFormatException  on SP format error
	 * @throws XMLStreamException on XML format error
	 */
	protected final void assertForwardDeserialization(final String message,
													final Object expected, final String xml)
			throws XMLStreamException, SPFormatException {
		try (StringReader sreader = new StringReader(xml)) {
			assertEquals(message, expected, oldReader.readXML(FAKE_FILENAME, sreader,
					expected.getClass(), Warning.Die));
		}
		try (StringReader sreader = new StringReader(xml)) {
			assertEquals(message, expected, oldReader.readXML(FAKE_FILENAME, sreader,
					expected.getClass(), Warning.Die));
		}
		try (StringReader sreader = new StringReader(xml)) {
			assertEquals(message, expected, newReader.readXML(FAKE_FILENAME, sreader,
					expected.getClass(), Warning.Die));
		}
		try (StringReader sreader = new StringReader(xml)) {
			assertEquals(message, expected, newReader.readXML(FAKE_FILENAME, sreader,
					expected.getClass(), Warning.Die));
		}
	}

	/**
	 * Assert that two deserialzed forms are equivalent, using both readers, both with
	 * and
	 * without reflection.
	 *
	 * @param <T>          the type they'll deserialize to.
	 * @param message      the message to pass to JUnit
	 * @param firstForm    the first form
	 * @param secondForm   the second form
	 * @param type         the type
	 * @param warningLevel the warning level to set.
	 * @throws SPFormatException  on SP format error
	 * @throws XMLStreamException on XML format error
	 */
	protected final <T> void assertEquivalentForms(final String message,
											final String firstForm,
											final String secondForm,
											final Class<T> type,
											final Warning warningLevel)
			throws SPFormatException, XMLStreamException {
		assertEquals(message, oldReader.readXML(FAKE_FILENAME,
				new StringReader(firstForm), type, warningLevel),
				oldReader.readXML(FAKE_FILENAME, new StringReader(secondForm), type,
						warningLevel));
		assertEquals(message, oldReader.readXML(FAKE_FILENAME,
				new StringReader(firstForm), type, warningLevel),
				oldReader.readXML(FAKE_FILENAME, new StringReader(secondForm), type,
						warningLevel));
		assertEquals(message, newReader.readXML(FAKE_FILENAME,
				new StringReader(firstForm), type, warningLevel),
				newReader.readXML(FAKE_FILENAME, new StringReader(secondForm), type,
						warningLevel));
		assertEquals(message, newReader.readXML(FAKE_FILENAME,
				new StringReader(firstForm), type, warningLevel),
				newReader.readXML(FAKE_FILENAME, new StringReader(secondForm), type,
						warningLevel));
	}

	/**
	 * @param obj        an object
	 * @param deprecated whether to use the deprecated XML-serialization idiom
	 * @return its serialized form
	 * @throws IOException on I/O error creating it
	 */
	@SuppressWarnings("deprecation")
	protected static String createSerializedForm(final Object obj,
												final boolean deprecated)
			throws IOException {
		final StringWriter writer = new StringWriter();
		if (deprecated) {
			ReaderAdapter.ADAPTER.write(obj).write(
					writer, 0);
		} else {
			CompactXMLWriter.writeSPObject(writer, obj);
		}
		return NullCleaner.assertNotNull(writer.toString());
	}

	/**
	 * Assert that reading the given XML will give a MissingChildException. If it's only
	 * supposed to be a warning, assert that it'll pass with warnings disabled but object
	 * with them made fatal. This version tests both old and new readers.
	 *
	 * @param xml         the XML to read
	 * @param desideratum the class it would produce if it weren't erroneous
	 * @throws SPFormatException  on unexpected SP format error
	 * @throws XMLStreamException on XML format error
	 */
	protected final void assertMissingChild(final String xml,
									final Class<?> desideratum)
			throws XMLStreamException, SPFormatException {
		assertMissingChild(oldReader, xml, desideratum);
		assertMissingChild(newReader, xml, desideratum);
	}

	/**
	 * Assert that reading the given XML will give a MissingChildException. If it's only
	 * supposed to be a warning, assert that it'll pass with warnings disabled but object
	 * with them made fatal.
	 *
	 * @param reader      the reader to do the reading
	 * @param xml         the XML to read
	 * @param desideratum the class it would produce if it weren't erroneous
	 * @throws SPFormatException  on unexpected SP format error
	 * @throws XMLStreamException on XML format error
	 */
	private static void assertMissingChild(final ISPReader reader,
										final String xml, final Class<?> desideratum)
			throws XMLStreamException, SPFormatException {
		try {
			reader.readXML(FAKE_FILENAME, new StringReader(xml),
					desideratum, Warning.Ignore);
			fail("We were expecting a MissingChildException");
		} catch (final MissingChildException except) {
			LOGGER.log(Level.FINEST, "Got the expected MissingChildException",
					except);
		}
	}

	/**
	 * Assert that a map is properly deserialized (by the main map-deserialization
	 * methods) into a view.
	 *
	 * @param message  the message to use in JUnit calls
	 * @param expected the object to test against
	 * @param xml      the XML to deserialize it into.
	 * @throws SPFormatException  if map format too old or new for a reader, or on other
	 *                            SP format error
	 * @throws XMLStreamException on XML format error
	 */
	protected final void assertMapDeserialization(final String message,
											final IMapNG expected, final String xml)
			throws XMLStreamException, SPFormatException {
		assertEquals(message, expected, ((IMapReader) oldReader).readMap(
				FAKE_FILENAME, new StringReader(xml), Warning.Die));
		assertEquals(message, expected, ((IMapReader) newReader).readMap(
				FAKE_FILENAME, new StringReader(xml), Warning.Die));
	}

	/**
	 * Determine the size of an iterable. Note that its iterator will have been advanced
	 * to the end.
	 *
	 * @param iter an iterable
	 * @param <T>  the type of thing it contains
	 * @return the number of items in the iterable
	 */
	protected static <T> long iteratorSize(final Iterable<T> iter) {
		return StreamSupport.stream(iter.spliterator(), false).count();
	}

	/**
	 * Assert that the given object, if serialized and deserialized, will have its image
	 * property preserved. We modify its image property, but set it back to the original
	 * value before exiting the method.
	 *
	 * @param message the message to use for assertions
	 * @param obj     the object to serialize
	 * @throws SPFormatException  on SP XML problem
	 * @throws XMLStreamException on XML reading problem
	 * @throws IOException        on I/O error creating serialized form
	 */
	protected final void assertImageSerialization(final String message,
												final HasMutableImage obj)
			throws XMLStreamException, SPFormatException, IOException {
		final String origImage = obj.getImage();
		obj.setImage("imageForSerialization");
		assertImageSerialization(message, obj, oldReader);
		assertImageSerialization(message, obj, newReader);
		obj.setImage(origImage);
	}

	/**
	 * Assert that the given object, if serialized and deserialized, will have its image
	 * property preserved. We modify its image property, but set it back to the original
	 * value before exiting the method.
	 *
	 * @param message the message to use for assertions
	 * @param obj     the object to serialize
	 * @param reader  the reader to use
	 * @throws SPFormatException  on SP XML problem
	 * @throws XMLStreamException on XML reading problem
	 * @throws IOException        on I/O error creating serialized form
	 */
	private static void assertImageSerialization(final String message, final HasImage obj,
												final ISPReader reader)
			throws XMLStreamException, SPFormatException, IOException {
		assertEquals(
				message,
				obj.getImage(),
				reader.readXML(FAKE_FILENAME,
						new StringReader(createSerializedForm(obj, true)),
						obj.getClass(), Warning.Ignore).getImage());
		assertEquals(
				message,
				obj.getImage(),
				reader.readXML(FAKE_FILENAME,
						new StringReader(createSerializedForm(obj, false)),
						obj.getClass(), Warning.Ignore).getImage());
	}
}
