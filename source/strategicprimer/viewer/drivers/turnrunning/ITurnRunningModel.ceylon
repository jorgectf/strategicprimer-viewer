import strategicprimer.model.common.map {
    HasExtent,
    HasPopulation,
    Player,
    Point,
    TileFixture
}

import strategicprimer.drivers.exploration.common {
    IExplorationModel
}

import ceylon.decimal {
    Decimal
}

import strategicprimer.drivers.common {
    IAdvancementModel
}

import strategicprimer.model.common.map.fixtures {
    IResourcePile,
    Quantity
}

import strategicprimer.model.common.map.fixtures.mobile {
    IUnit
}

import strategicprimer.model.common.map.fixtures.towns {
    Fortress
}

import lovelace.util.common {
    matchingValue
}

"A model for turn-running apps."
shared interface ITurnRunningModel satisfies IExplorationModel&IAdvancementModel {
    "Add a copy of the given fixture to all submaps at the given location iff no fixture
     with the same ID is already there."
    shared formal void addToSubMaps(Point location, TileFixture fixture, Boolean zero);

    "Reduce the population of a group of plants, animals, etc., and copy the reduced form
     into all subordinate maps."
    shared formal void reducePopulation(Point location, HasPopulation<out TileFixture>&TileFixture fixture,
            Boolean zero, Integer reduction);

    "Reduce the acreage of a fixture, and copy the reduced form into all subordinate maps."
    shared formal void reduceExtent(Point location,
            HasExtent<out HasExtent<out Anything>&TileFixture>&TileFixture fixture,
            Boolean zero, Decimal reduction);

    "Reduce the matching [[resource|IResourcePile]], in a [[unit|IUnit]] or
     [[fortress|strategicprimer.model.common.map.fixtures.towns::Fortress]]
     owned by [[the specified player|owner]], by [[the specified
     amount|amount]]. Returns [[true]] if any (mutable) resource piles matched
     in any of the maps, [[false]] otherwise."
    shared formal Boolean reduceResourceBy(IResourcePile resource, Decimal amount, Player owner);

    "Remove the given [[resource|IResourcePile]] from a [[unit|IUnit]] or
     [[fortress|strategicprimer.model.common.map.fixtures.towns::Fortress]]
     owned by [[the specified player|owner]] in all maps. Returns [[true]] if
     any matched in any of the maps, [[false]] otherwise."
    deprecated("Use [[reduceResourceBy]] when possible instead.")
    shared formal Boolean removeResource(IResourcePile resource, Player owner);

    "Set the given unit's results for the given turn to the given text. Returns
     [[true]] if a matching (and mutable) unit was found in at least one map,
     [[false]] otherwise."
    shared formal Boolean setUnitResults(IUnit unit, Integer turn, String results);

    "Add a resource with the given ID, kind, contents, quantity, and (if
     provided) created date in the given unit or fortress in all maps.
     Returns [[true]] if a matching (and mutable) unit or fortress was found in
     at least one map, [[false]] otherwise."
    shared formal Boolean addResource(IUnit|Fortress container, Integer id, String kind, String contents,
        Quantity quantity, Integer? createdDate = null);

    "Add a non-talking animal population to the given unit in all maps. Returns
     [[true]] if the input makes sense and a matching (and mutable) unit was
     found in at least one map, [[false]] otherwise.

     Note the last two parameters are *reversed* from the
      [[strategicprimer.model.common.map.fixtures.mobile::AnimalImpl]]
      constructor, to better fit the needs of *our* callers."
    shared formal Boolean addAnimal(IUnit container, String kind, String status, Integer id,
        Integer population = 1, Integer born = -1);

    "Find the given player's HQ in the main map. If it can't find a
     [[fortress|Fortress]] with the given name, return *a* fortress of that
     player. If it can't find even one, return [[null]]."
    shared default Fortress? findHQ(Player player, String fortressName = "HQ") {
        variable Fortress? retval = null;
        for (fortress in map.fixtures.items.narrow<Fortress>()
                .filter(matchingValue(player, Fortress.owner))) {
            if (fortress.name == fortressName) { // TODO: Take the name as a parameter, for maximum flexibility
                return fortress;
            } else if (!retval exists) {
                retval = fortress;
            }
        }
        return retval;
    }

    "Transfer [[quantity]] units from [[a resource|from]] to (if not all of it)
     another resource in [[a unit or fortress|to]] in all maps. If this leaves
     any behind in any map, [[id]] will be called exactly once to generate the
     ID number for the resource in the destination in maps where that is the
     case. Returns [[true]] if a matching resource and destination are found
     (and the transfer occurs) in any map, [[false]] otherwise."
    shared formal Boolean transferResource(IResourcePile from, IUnit|Fortress to,
        Decimal quantity, Integer() id);
}
