package utility;

import drivers.common.DriverFailedException;
import common.xmlio.SPFormatException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.Files;

import java.io.StringReader;

import common.map.IMapNG;

import common.xmlio.Warning;

import impl.xmlio.IMapReader;
import impl.xmlio.TestReaderFactory;
import impl.xmlio.SPWriter;

import drivers.common.UtilityDriver;
import drivers.common.SPOptions;
import drivers.common.EmptyOptions;

import drivers.common.cli.ICLIHelper;
import javax.xml.stream.XMLStreamException;

/**
 * A driver for comparing map readers.
 */
public class ReaderComparator implements UtilityDriver {
	private static String readAll(final Path path) throws IOException {
		return String.join(System.lineSeparator(), Files.readAllLines(path, StandardCharsets.UTF_8));
	}

	@Override
	public SPOptions getOptions() {
		return EmptyOptions.EMPTY_OPTIONS;
	}

	private final ICLIHelper cli;
	public ReaderComparator(final ICLIHelper cli) {
		this.cli = cli;
	}

	/**
	 * Compare the two readers' performance on the given files.
	 */
	@Override
	public void startDriver(final String... args) throws DriverFailedException {
		final IMapReader readerOne = TestReaderFactory.getOldMapReader();
		final IMapReader readerTwo = TestReaderFactory.getNewMapReader();
		final SPWriter writerOne = TestReaderFactory.getOldWriter();
		final SPWriter writerTwo = TestReaderFactory.getNewWriter();
		final Warning warner = Warning.IGNORE;
		for (final String arg : args) {
			cli.println(arg + ":");
			final Path path = Paths.get(arg);
			final String contents;
			try {
				contents = readAll(path);
			} catch (final IOException except) {
				throw new DriverFailedException(except, "I/O error reading file");
			}
			final long readStartOne = System.nanoTime();
			final IMapNG mapOne;
			try {
				mapOne = readerOne.readMapFromStream(path,
					new StringReader(contents), warner);
			} catch (final SPFormatException except) {
				throw new DriverFailedException(except,
					"Fatal SP format error reported by first reader");
			} catch (final XMLStreamException except) {
				throw new DriverFailedException(except,
					"Malformed XML reported by first reader");
			} catch (final IOException except) {
				throw new DriverFailedException(except,
					"I/O error thrown by first reader");
			}
			final long readEndOne = System.nanoTime();
			cli.println("Old reader took " + (readEndOne - readStartOne));
			final long readStartTwo = System.nanoTime();
			final IMapNG mapTwo;
			try {
				mapTwo = readerTwo.readMapFromStream(path,
					new StringReader(contents), warner);
			} catch (final SPFormatException except) {
				throw new DriverFailedException(except,
					"Fatal SP format error reported by second reader");
			} catch (final XMLStreamException except) {
				throw new DriverFailedException(except,
					"Malformed XML reported by second reader");
			} catch (final IOException except) {
				throw new DriverFailedException(except,
					"I/O error thrown by second reader");
			}
			final long readEndTwo = System.nanoTime();
			cli.println("New reader took " + (readEndTwo - readStartTwo));
			if (mapOne.equals(mapTwo)) {
				cli.println("Readers produce identical results");
			} else {
				cli.println("Readers differ on " + arg);
			}
			final StringBuilder outOne = new StringBuilder();
			final long writeStartOne = System.nanoTime();
			try {
				writerOne.write(outOne::append, mapOne);
			} catch (final XMLStreamException except) {
				throw new DriverFailedException(except,
					"First writer produced malformed XML");
			} catch (final IOException except) {
				throw new DriverFailedException(except,
					"I/O error reported by first writer");
			}
			final long writeEndOne = System.nanoTime();
			cli.println("Old writer took " + (writeEndOne - writeStartOne));
			final StringBuilder outTwo = new StringBuilder();
			final long writeStartTwo = System.nanoTime();
			try {
				writerTwo.write(outTwo::append, mapTwo);
			} catch (final XMLStreamException except) {
				throw new DriverFailedException(except,
					"Second writer produced malformed XML");
			} catch (final IOException except) {
				throw new DriverFailedException(except,
					"I/O error reported by second writer");
			}
			final long writeEndTwo = System.nanoTime();
			cli.println("New writer took " + (writeEndTwo - writeStartTwo));
			if (outOne.toString().equals(outTwo.toString())) {
				cli.println("Writers produce identical results");
			} else if (outOne.toString().strip().equals(outTwo.toString().strip())) {
				// TODO: try with a global replacement of all whitespace with single spaces
				cli.println("Writers produce identical results except for whitespace");
			} else {
				cli.println("Writers differ on " + arg);
			}
		}
	}
}
