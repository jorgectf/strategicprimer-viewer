import java.nio.file {
    JPath=Path
}

import strategicprimer.model.map {
    IMutableMapNG,
    MapDimensions
}
"An interface for driver-model objects that hold a mutable map. Interfaces deriving from
 this one will give the methods each driver needs."
shared interface IDriverModel satisfies MapChangeSource&VersionChangeSource {
    "Set the (main) map and its filename."
    shared formal void setMap("The new map" IMutableMapNG newMap,
            "The file from which it was loaded, if known" JPath? origin);
    "The (main) map."
    shared formal IMutableMapNG map;
    "Its dimensions."
    shared default MapDimensions mapDimensions => map.dimensions;
    "The filename from which the map was loaded or to which it should be written."
    shared formal JPath? mapFile;
}
