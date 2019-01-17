import java.io {
    JReader=Reader,
    IOException,
    FileNotFoundException
}
import java.nio.file {
    JPaths=Paths,
    JFiles=Files,
    NoSuchFileException
}

import javax.xml.namespace {
    QName
}
import javax.xml.stream {
    XMLStreamException
}
import javax.xml.stream.events {
    XMLEvent,
    StartElement
}

import lovelace.util.common {
    IteratorWrapper,
    PathWrapper,
    MissingFileException,
    MalformedXMLException
}
import lovelace.util.jvm {
    TypesafeXMLEventReader
}

import strategicprimer.model.common.idreg {
    IDRegistrar,
    IDFactory
}
import strategicprimer.model.common.map {
    IMutableMapNG
}
import strategicprimer.model.impl.xmlio {
    IMapReader,
    ISPReader
}
import strategicprimer.model.common.xmlio {
    SPFormatException,
    Warning
}
import strategicprimer.model.impl.xmlio.io_impl {
    IncludingIterator
}

"Sixth-generation SP XML reader."
shared object yaXMLReader satisfies IMapReader&ISPReader {
    "Read an object from XML."
    throws(`class MalformedXMLException`, "if the XML isn't well-formed")
    throws(`class SPFormatException`, "on SP XML format error")
    throws(`class AssertionError`,
        "if a reader produces a different type than requested")
    shared actual Element readXML<Element>(
            "The file we're reading from" PathWrapper file,
            "The stream to read from" JReader istream,
            "The Warning instance to use for warnings" Warning warner)
            given Element satisfies Object {
        try {
            Iterator<XMLEvent> reader = TypesafeXMLEventReader(istream);
            {XMLEvent*} eventReader = IteratorWrapper(IncludingIterator(file, reader,
                warner));
            IDRegistrar idFactory = IDFactory();
            if (exists event = eventReader.narrow<StartElement>().first) {
                assert (is Element retval = YAReaderAdapter(warner, idFactory)
                    .parse(event, QName("root"), eventReader));
                return retval;
            }
        } catch (XMLStreamException except) {
            throw MalformedXMLException(except);
        }
        throw MalformedXMLException.notWrapping(
            "XML stream didn't contain a start element");
    }

    "Read a map from a stream."
    throws(`class MalformedXMLException`, "on malformed XML")
    throws(`class SPFormatException`, "on SP format problems")
    shared actual IMutableMapNG readMapFromStream(
            "The file we're reading from" PathWrapper file,
            "The stream to read from" JReader istream,
            "The Warning instance to use for warnings" Warning warner) =>
                readXML<IMutableMapNG>(file, istream, warner);

    "Read a map from XML."
    throws(`class IOException`, "on I/O error")
    throws(`class MalformedXMLException`, "on malformed XML")
    throws(`class SPFormatException`, "on SP format problems")
    shared actual IMutableMapNG readMap("The file to read from" PathWrapper file,
            "The Warning instance to use for warnings" Warning warner) {
        try (istream = JFiles.newBufferedReader(JPaths.get(file.string))) {
            return readMapFromStream(file, istream, warner);
        } catch (FileNotFoundException|NoSuchFileException except) {
            throw MissingFileException(file, except);
        }
    }
}
