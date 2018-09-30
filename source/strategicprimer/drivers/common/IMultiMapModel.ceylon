import strategicprimer.model.common.map {
    IMutableMapNG
}

import lovelace.util.common {
    PathWrapper
}

"""A driver-model for drivers that have a main map (like every driver) and any number of
   "subordinate" maps."""
shared interface IMultiMapModel satisfies IDriverModel {
    "Add a subordinate map."
    shared formal void addSubordinateMap(
            "The map to add"
            IMutableMapNG map,
            "The file it was loaded from"
            PathWrapper? file,
            "Whether it has been modified since being loaded or last saved"
            Boolean modified = false);
    "Subordinate maps with their filenames (and the flag of whether the map has been
     modified since loaded or last saved), as [[Entries|Entry]]"
    shared formal {<IMutableMapNG->[PathWrapper?, Boolean]>*} subordinateMaps;
    "All maps with their filenames (and the flag of whether the map has been
     modified since loaded or last saved), including the main map and the subordinate
     maps, as [[Entries|Entry]]"
    shared default {<IMutableMapNG->[PathWrapper?, Boolean]>*} allMaps =>
            subordinateMaps.follow(map->[mapFile, mapModified]);
    "Set the 'modified' flag for the given map."
    shared formal void setModifiedFlag(IMutableMapNG map, Boolean modified);
}
