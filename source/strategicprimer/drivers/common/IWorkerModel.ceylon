import strategicprimer.model.common.map {
    HasKind,
    HasMutableName,
    HasMutableOwner,
    Player
}

import strategicprimer.model.common.map.fixtures {
    UnitMember
}

import strategicprimer.model.common.map.fixtures.mobile {
    IUnit
}
import strategicprimer.drivers.common {
    IMultiMapModel
}
import strategicprimer.model.common.map.fixtures.towns {
    Fortress
}

"A driver model for the worker management app and the advancement app, aiming
 to provide a hierarchical view of the units in the map and their members,
 regardless of their location."
shared interface IWorkerModel satisfies IMultiMapModel {
    "All the players in all the maps."
    shared formal {Player*} players;

    "The units in the map belonging to the given player."
    shared formal {IUnit*} getUnits(
            "The player whose units we want"
            Player player,
            """Which "kind" to restrict to, if any"""
            String? kind = null);

    """The "kinds" of units that the given player has."""
    shared formal {String*} getUnitKinds(
            "The player whose unit kinds we want"
            Player player);

    "Add a unit in its owner's HQ."
    shared formal void addUnit("The unit to add" IUnit unit);

    "Get a unit by ID number."
    shared formal IUnit? getUnitByID(Player owner, Integer id);

    "The player that the UI seems to be concerned with."
    shared formal variable Player currentPlayer;

    "The fortresses belonging to the current player."
    // TODO: Return their positions with them?
    shared formal {Fortress*} getFortresses(
            "The player whose fortresses we want" Player player);

    """Remove the given unit from the map. It must be empty, and may be
       required to be owned by the current player. The operation will also fail
       if "matching" units differ in name or kind from the provided unit.
       Returns [[true]] if the preconditions were met and the unit was removed,
       and [[false]] otherwise."""
    shared formal Boolean removeUnit(IUnit unit);

    "Move a unit-member from one unit to another."
    shared formal void moveMember(UnitMember member, IUnit old, IUnit newOwner);

    "Dismiss a unit member from a unit and from the player's service."
    shared formal void dismissUnitMember(UnitMember member);

    "The unit members that have been dismissed during this session."
    shared formal {UnitMember*} dismissed;

    "Add a new member to a unit."
    shared formal void addUnitMember(
            "The unit that should own the member"
            IUnit unit,
            "The member to add to the unit"
            UnitMember member);

    "Change something's name. Returns [[true]] if we were able to find it and
     changed its name, [[false]] on failure."
    shared formal Boolean renameItem(HasMutableName item, String newName);

    "Change something's kind. Returns [[true]] if we were able to find it and
     changed its kind, [[false]] on failure."
    shared formal Boolean changeKind(HasKind item, String newKind);

    "Add a unit member to the unit that contains the given member in each map.
     Returns [[true]] if any of the maps had a unit containing the existing
     sibling, to which the new member was added, [[false]] otherwise."
    shared formal Boolean addSibling(
        "The member that is already in the tree."
        UnitMember base,
        "The member to add as its sibling."
        UnitMember sibling);

    "Change the owner of the given item in all maps. Returns [[true]] if this
     succeeded in any map, [[false]] otherwise."
    shared formal Boolean changeOwner(HasMutableOwner item, Player newOwner);
}
