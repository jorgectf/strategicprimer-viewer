import javax.xml.namespace {
    QName
}
import javax.xml.stream {
    Location
}
import javax.xml.stream.events {
    StartElement
}

import strategicprimer.model.xmlio {
    SPFormatException
}
"A custom exception for when a tag has a child tag it can't handle."
shared class UnwantedChildException extends SPFormatException {
    "The current tag."
    shared QName tag;
    "The unwanted child."
    shared QName child;
    "For when the unwanted child isn't an unwanted *tag* at all. (The one current use
     is for arbitrary text outside a tile."
    shared new childInTag(
            "The current tag" QName parent,
            "The unwanted child" QName child,
            "Where this occurred" Location location,
            "Why this occurred" Throwable cause)
            extends SPFormatException(
                "Unexpected child ``child.localPart`` in tag ``parent.localPart``",
                location.lineNumber, location.columnNumber, cause) {
        tag = parent;
        this.child = child;
    }
    shared new (
	        "The current tag"
	        QName parent,
	        "The unwanted child"
	        StartElement child,
            "Another exception that caused this one"
            Throwable? cause = null)
            extends SPFormatException(
                "Unexpected child ``child.name.localPart`` in tag ``parent.localPart``",
                child.location.lineNumber, child.location.columnNumber, cause) {
        tag = parent;
        this.child = child.name;
    }
    "Copy-constructor-with-replacement, for cases where the original thrower didn't know
     the parent tag."
    shared new addParent(QName parent, UnwantedChildException except)
            extends SPFormatException(
                "Unexpected child ``except.child.localPart`` in tag ``parent.localPart``",
                except.line, except.column, except.cause) {
        tag = parent;
        child = except.child;
    }
}
