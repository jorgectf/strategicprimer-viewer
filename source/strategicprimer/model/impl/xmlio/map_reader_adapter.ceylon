import ceylon.file {
    Path,
    parsePath
}
import ceylon.logging {
    Logger,
    logger
}
import ceylon.test {
    test
}

import java.io {
    JReader=Reader
}

import lovelace.util.common {
    todo
}

import strategicprimer.model.impl.dbio {
    spDatabaseWriter,
    spDatabaseReader
}
import strategicprimer.model.common.map {
    IMutableMapNG,
    IMapNG
}
import strategicprimer.model.impl.xmlio.fluidxml {
    SPFluidReader
}
import strategicprimer.model.impl.xmlio.yaxml {
    yaXMLWriter
}
import strategicprimer.model.common.xmlio {
    Warning,
    warningLevels
}

"A logger."
Logger log = logger(`module strategicprimer.model.impl`);
shared object mapIOHelper {
    IMapReader reader = SPFluidReader();
    SPWriter writer = yaXMLWriter;
    SPWriter dbWriter = spDatabaseWriter;
    IMapReader dbReader = spDatabaseReader;
    "Turn a series of Strings into a series of equvalent Paths."
    shared {Path+} namesToFiles(String+ names) => names.map(parsePath);
    "Read a map from a file or a stream.."
    todo("Port to use ceylon.io or ceylon.buffer")
    shared IMutableMapNG readMap(Path|JReader file,
            Warning warner = warningLevels.warn) {
        log.trace("In mapIOHelper.readMap");
        if (is Path file) {
            if (file.string.endsWith(".db")) {
                log.trace("Reading from ``file`` as an SQLite database");
                return dbReader.readMap(file, warner);
            } else {
                log.trace("Reading from ``file``");
                return reader.readMap(file, warner);
            }
        } else {
            log.trace("Reading from a Reader");
            return reader.readMapFromStream(parsePath(""), file, warner);
        }
    }
    "Write a map to file."
    shared void writeMap(Path file, IMapNG map) {
        if (file.string.endsWith(".db") || file.string.empty) {
            log.trace("Writing to ``file`` as an SQLite database");
            dbWriter.write(file, map);
        } else {
            log.trace("Writing to ``file``");
            writer.write(file, map);
        }
    }
    test
    shared void testNamesToFiles() {
        {Path+} expected = [ parsePath("two"), parsePath("three"), parsePath("four")];
        "[[namesToFiles]] should return all names."
        assert (corresponding(namesToFiles("two", "three", "four"), expected));
    }
}