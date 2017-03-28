import ceylon.language.meta {
    type
}
import ceylon.language.meta.model {
    ClassOrInterface
}

import controller.map.formatexceptions {
    MissingPropertyException
}

import java.lang {
    JAppendable=Appendable,
    IllegalArgumentException
}

import javax.xml.namespace {
    QName
}
import javax.xml.stream.events {
    StartElement,
    XMLEvent
}

import model.map {
    HasMutableImage,
    HasKind,
    HasImage
}
import strategicprimer.viewer.model {
    IDRegistrar
}
import strategicprimer.viewer.model.map.fixtures.mobile {
    Centaur,
    IUnit,
    SimpleImmortal,
    SimpleImmortalKind,
    Giant,
    Fairy,
    Dragon,
    Animal,
    MobileFixture
}

import util {
    Warning
}
"A reader for 'mobile fixtures'"
class YAMobileReader(Warning warning, IDRegistrar idRegistrar)
        extends YAAbstractReader<MobileFixture>(warning, idRegistrar) {
    Map<ClassOrInterface<MobileFixture>, String> tagMap = map {
        `Animal`->"animal", `Centaur`->"centaur", `Dragon`->"dragon",
        `Fairy`->"fairy", `Giant`->"giant"
    };
    Set<String> supportedTags = set { *tagMap.items }.union(set {
        *{*`SimpleImmortalKind`.caseValues}
            .map((SimpleImmortalKind kind) => kind.tag)});
    MobileFixture createAnimal(StartElement element) {
        // TODO: support 'traces="false"'
        Boolean tracks = hasParameter(element, "traces");
        Integer idNum;
        if (tracks && !hasParameter(element, "id")) {
            idNum = -1;
        } else {
            idNum = getOrGenerateID(element);
        }
        value talking = Boolean.parse(getParameter(element, "talking", "false"));
        if (is Boolean talking) {
            return Animal(getParameter(element, "kind"), tracks, talking,
                getParameter(element, "status", "wild"), idNum);
        } else {
            // TODO: Is there a better exception for this case?
            throw MissingPropertyException(element, "talking", talking);
        }
    }
    MobileFixture readSimple(String tag, Integer idNum) {
        if (exists kind = SimpleImmortalKind.parse(tag)) {
            return SimpleImmortal(kind, idNum);
        } else {
            throw IllegalArgumentException("No simple immortal matches ``tag``");
        }
    }
    shared actual Boolean isSupportedTag(String tag) =>
            supportedTags.contains(tag.lowercased);
    shared actual MobileFixture read(StartElement element, QName parent, {XMLEvent*} stream) {
        requireTag(element, parent, *supportedTags);
        MobileFixture twoParam(MobileFixture(String, Integer) constr) =>
            constr(getParameter(element, "kind"), getOrGenerateID(element));
        MobileFixture retval;
        switch (type = element.name.localPart.lowercased)
        case ("animal") { retval = createAnimal(element); }
        case ("centaur") { retval = twoParam(Centaur); }
        case ("dragon") { retval = twoParam(Dragon); }
        case ("fairy") { retval = twoParam(Fairy); }
        case ("giant") { retval = twoParam(Giant); }
        else { retval = readSimple(type, getOrGenerateID(element)); }
        spinUntilEnd(element.name, stream);
        if (is HasMutableImage retval) {
            retval.setImage(getParameter(element, "image", ""));
        }
        return retval;
    }
    shared actual void write(JAppendable ostream, MobileFixture obj, Integer indent) {
        if (is IUnit obj) {
            throw IllegalArgumentException("Unit handled elsewhere");
        } else if (is Animal obj) {
            writeTag(ostream, "animal", indent);
            writeProperty(ostream, "kind", obj.kind);
            if (obj.traces) {
                writeProperty(ostream, "traces", "");
            }
            if (obj.talking) {
                writeProperty(ostream, "talking", "true");
            }
            if ("wild" != obj.status) {
                writeProperty(ostream, "status", obj.status);
            }
            if (!obj.traces) {
                writeProperty(ostream, "id", obj.id);
            }
            writeImageXML(ostream, obj);
            closeLeafTag(ostream);
        } else if (is SimpleImmortal obj) {
            writeTag(ostream, obj.kind, indent);
            writeProperty(ostream, "id", obj.id);
            writeImageXML(ostream, obj);
            closeLeafTag(ostream);
        } else if (exists tag = tagMap.get(type(obj))) {
            writeTag(ostream, tag, indent);
            if (is HasKind obj) {
                writeProperty(ostream, "kind", obj.kind);
            }
            writeProperty(ostream, "id", obj.id);
            if (is HasImage obj) {
                writeImageXML(ostream, obj);
            }
            closeLeafTag(ostream);
        } else {
            throw IllegalArgumentException("No tag for ``obj.shortDesc()``");
        }
        // TODO: At least the closeLeafTag() could be moved here
    }
    shared actual Boolean canWrite(Object obj) => obj is MobileFixture && !obj is IUnit;
}