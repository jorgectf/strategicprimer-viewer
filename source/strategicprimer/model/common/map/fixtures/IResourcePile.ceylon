import lovelace.util.common {
    todo
}

import strategicprimer.model.common.map {
    IFixture,
    HasImage,
    HasKind
}
import strategicprimer.model.common.map.fixtures {
    UnitMember,
    FortressMember,
    Quantity
}

"A quantity of some kind of resource."
todo("More members?")
shared interface IResourcePile
        satisfies UnitMember&FortressMember&HasKind&HasImage&Identifiable {
    "What specific kind of thing is in the resource pile."
    shared formal String contents;

    "How much of that thing is in the pile, including units."
    shared formal Quantity quantity;

    "The turn on which the resource was created."
    shared formal Integer created;

    "If we ignore ID, a fixture is equal iff it is an IResourcePile with the
     same kind and contents, of the same age, with equal quantity."
    shared actual Boolean equalsIgnoringID(IFixture fixture) {
        if (is IResourcePile fixture) {
            return fixture.kind == kind && fixture.contents == contents &&
                fixture.quantity == quantity && fixture.created == created;
        } else {
            return false;
        }
    }

    "A fixture is a subset iff it is an IResourcePile of the same kind,
     contents, and age, with the same ID, and its quantity is a subset of ours."
    shared actual Boolean isSubset(IFixture obj, Anything(String) report) {
        if (obj.id == id) {
            if (is IResourcePile obj) {
                variable Boolean retval = true;
                Anything(String) localReport = compose(report,
                    "In Resource Pile, ID #``id``: ".plus);
                if (kind != obj.kind) {
                    localReport("Kinds differ");
                    retval = false;
                }
                if (contents != obj.contents) {
                    localReport("Contents differ");
                    retval = false;
                }
                if (!quantity.isSubset(obj.quantity, localReport)) {
                    retval = false;
                }
                if (created != obj.created, obj.created != -1) {
                    localReport("Age differs");
                    retval = false;
                }
                return retval;
            } else {
                report("Different fixture types given for ID #``id``");
                return false;
            }
        } else {
            report("IDs differ");
            return false;
        }
    }

    shared actual Integer hash => id;

    shared actual Boolean equals(Object obj) {
        if (is IResourcePile obj) {
            return id == obj.id && equalsIgnoringID(obj);
        } else {
            return false;
        }
    }

    shared actual String string {
        if (quantity.units.empty) {
            return "A pile of ``quantity`` ``contents`` (``kind``)``(created < 0) then
                "" else " from turn ``created``"``";
        } else {
            return "A pile of ``quantity`` of ``contents`` (``kind``)``(created < 0) then
                "" else " from turn ``created``"``";
        }
    }

    "Clone the object."
    shared actual formal IResourcePile copy(Boolean zero);
}
