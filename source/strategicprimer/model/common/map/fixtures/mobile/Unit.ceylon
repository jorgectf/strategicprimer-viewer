import ceylon.collection {
    MutableMap,
    naturalOrderTreeMap,
    SortedMap,
    MutableSet
}

import lovelace.util.common {
    todo,
    ArraySet
}
import strategicprimer.model.common.map {
    IFixture,
    HasMutableName,
    HasMutableKind,
    HasMutableImage,
    HasPortrait,
    TileFixture,
    HasMutableOwner,
    Player
}
import strategicprimer.model.common.map.fixtures {
    UnitMember
}
import strategicprimer.model.common.map.fixtures.mobile {
    ProxyFor
}
import ceylon.logging {
    Logger,
    logger
}

"Logger."
Logger log = logger(`module strategicprimer.model.common`);

"A unit in the map."
todo("FIXME: we need more members: something about stats; what else?")
shared class Unit(owner, kind, name, id) satisfies IUnit&HasMutableKind&
        HasMutableName&HasMutableImage&HasMutableOwner&HasPortrait {
    "The unit's orders. This is serialized to and from XML, but does not affect equality
     or hashing, and is not printed in [[string]]."
    SortedMap<Integer, String>&MutableMap<Integer, String> orders =
            naturalOrderTreeMap<Integer, String>([]);

    "The unit's results. This is serialized to and from XML, but does not affect equality
     or hashing, and is not printed in [[string]]."
    SortedMap<Integer, String>&MutableMap<Integer, String> results =
            naturalOrderTreeMap<Integer, String>([]);

    "The members of the unit."
    MutableSet<UnitMember> members = ArraySet<UnitMember>();

    "The ID number."
    shared actual Integer id;

    "The filename of an image to use as an icon for this instance."
    shared actual variable String image = "";

    "The player that owns the unit."
    shared actual variable Player owner;

    """What kind of unit this is. For player-owned units this is usually their "category"
       (e.g. "agriculture"); for independent units it's more descriptive."""
    shared actual variable String kind;

    """The name of this unit. For independent this is often something like "party from the
       village"."""
    shared actual variable String name;

    "The filename of an image to use as a portrait for the unit."
    shared actual variable String portrait = "";

    "The unit's orders for all turns."
    todo("Wrap somehow so callers can't cast to `MutableMap`.")
    shared actual SortedMap<Integer, String> allOrders => orders;

    "The unit's results for all turns."
    todo("Wrap somehow so callers can't cast to `MutableMap`.")
    shared actual SortedMap<Integer, String> allResults => results;

    "Clone the unit."
    todo("There should be some way to convey the unit's *size* without the *details* of
          its contents. Or maybe we should give the contents but not *their* details?")
    shared actual Unit copy(Boolean zero) {
        Unit retval = Unit(owner, kind, name, id);
        if (!zero) {
            retval.orders.putAll(orders);
            retval.results.putAll(results);
            for (member in members) {
                retval.addMember(member.copy(false));
            }
        }
        retval.image = image;
        return retval;
    }

    "Add a member."
    shared actual void addMember(UnitMember member) {
        if (is ProxyFor<out Anything> member) {
            log.error("ProxyWorker added to Unit",
                AssertionError("ProxyWorker added to Unit"));
        }
        members.add(member);
    }

    "Remove a member."
    shared actual void removeMember(UnitMember member) => members.remove(member);

    "An iterator over the unit's members."
    shared actual Iterator<UnitMember> iterator() => members.iterator();

    "An object is equal iff it is a IUnit owned by the same player, with the same kind,
     ID, and name, and with equal members."
    shared actual Boolean equals(Object obj) {
        if (is IUnit obj) {
            return obj.owner.playerId == owner.playerId && obj.kind == kind &&
                obj.name == name && obj.id == id &&
                members.containsEvery(obj) && obj.containsEvery(members);
        } else {
            return false;
        }
    }

    shared actual Integer hash => id;

    shared actual String string {
        if (owner.independent) {
            return "Independent unit of type ``kind``, named ``name``";
        } else {
            return "Unit of type ``kind``, belonging to ``owner``, named ``name``";
        }
    }

    shared actual String verbose =>
            "``string``, consisting of:
             ``operatingSystem.newline.join(members)``";

    "An icon to represent units by default."
    by("[purzen](https://openclipart.org/detail/28731/sword-and-shield-icon)")
    shared actual String defaultImage = "unit.png";

    "If we ignore ID, a fixture is equal iff it is an IUnit owned by the same
     player with the same kind and name and neither has any members that are
     not equal-ignoring-ID to any member of the other."
    shared actual Boolean equalsIgnoringID(IFixture fixture) {
        if (is IUnit fixture, fixture.owner.playerId == owner.playerId,
                fixture.kind == kind, fixture.name == name) {
            for (member in this) {
                if (!fixture.any(member.equalsIgnoringID)) {
                    return false;
                }
            }
            for (member in fixture) {
                if (!any(member.equalsIgnoringID)) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    "Set orders for a turn."
    shared actual void setOrders(Integer turn, String newOrders) =>
            orders[turn] = newOrders;

    "Get orders for a turn."
    shared actual String getOrders(Integer turn) {
        if (exists retval = orders[turn]) {
            return retval;
        } else if (turn < 0, exists retval = orders[-1]) {
            return retval;
        } else {
            return "";
        }
    }

    "Set results for a turn."
    shared actual void setResults(Integer turn, String newResults) =>
            results[turn] = newResults;

    "Get results for a turn."
    shared actual String getResults(Integer turn) {
        if (exists retval = results[turn]) {
            return retval;
        } else if (turn < 0, exists retval = results[-1]) {
            return retval;
        } else {
            return "";
        }
    }

    "A short description of the fixture, giving its kind and owner but not its name."
    shared actual String shortDescription {
        if (owner.current) {
            return "a(n) ``kind`` unit belonging to you";
        } else if (owner.independent) {
            return "``name``, an independent unit";
        } else {
            return "a(n) ``kind`` unit belonging to ``owner.name``";
        }
    }

    "The required Perception check result for an explorer to notice the unit."
    shared actual Integer dc =>
            Integer.min(members.narrow<TileFixture>()
                .map(TileFixture.dc).follow(25 - members.size));
}
