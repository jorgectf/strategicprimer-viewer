import strategicprimer.viewer.xmlio {
    SPWriter
}
import java.io {
    IOException
}
import java.lang {
    JAppendable=Appendable
}
import java.nio.file {
    JPath=Path,
    JFiles=Files
}

import strategicprimer.viewer.model.map {
    IMapNG
}
"Sixth generation SP XML writer."
shared object yaXMLWriter satisfies SPWriter {
    "Write an object to a stream."
    throws(`class IOException`, "on I/O error")
    shared actual void writeSPObject("The stream to write to" JPath|JAppendable arg,
            "The object to write" Object obj) {
        if (is JAppendable ostream = arg) {
            YAReaderAdapter().write(ostream, obj, 0);
        } else if (is JPath file = arg) {
            try (writer = JFiles.newBufferedWriter(file)) {
                writeSPObject(writer, obj);
            }
        }
    }
    "Write a map to a file or stream."
    throws(`class IOException`, "on I/O error")
    shared actual void write("The file to write to." JPath|JAppendable arg,
            "The map to write." IMapNG map) => writeSPObject(arg, map);
}