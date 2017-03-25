import ceylon.collection {
    MutableList,
    ArrayList
}

import java.nio.file {
    JPath=Path
}

import lovelace.util.common {
    todo
}

import strategicprimer.viewer.model.map {
    IMutableMapNG,
    IMapNG
}
"A superclass for implementations of interfaces inheriting from [[IMultiMapModel]]."
shared class SimpleMultiMapModel extends SimpleDriverModel satisfies IMultiMapModel {
    "The collection of subordinate maps."
    MutableList<[IMutableMapNG, JPath?]> subordinateMapsList =
            ArrayList<[IMutableMapNG, JPath?]>();
    shared new (IMutableMapNG map, JPath? file)
            extends SimpleDriverModel(map, file) { }
    shared new copyConstructor(IDriverModel model)
            extends SimpleMultiMapModel(model.map, model.mapFile) {
        if (is IMultiMapModel model) {
            for (pair in model.subordinateMaps) {
                subordinateMapsList.add(pair);
            }
        }
    }
    shared actual void addSubordinateMap(IMutableMapNG map, JPath? file) =>
            subordinateMapsList.add([map, file]);
    todo(/*FIXME*/"Test this; I fixed the clearly-wrong implementation, but this might
                   cause [[ConcurrentModificationException]]")
    shared actual void removeSubordinateMap(IMapNG map) {
        subordinateMapsList.removeWhere(([localMap, file]) => localMap == map);
    }
    shared actual {[IMutableMapNG, JPath?]*} subordinateMaps =>
            {*subordinateMapsList};
    shared actual {[IMutableMapNG, JPath?]*} allMaps =>
            subordinateMaps.follow([map, mapFile]);
}
