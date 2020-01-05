import lovelace.util.common {
    PathWrapper
}

import strategicprimer.drivers.common.cli {
    ICLIHelper
}

import strategicprimer.drivers.common {
    SPOptions,
    DriverUsage,
    ParamCount,
    IDriverUsage,
    IDriverModel,
    DriverFactory,
    ModelDriverFactory,
    ModelDriver,
    SimpleMultiMapModel
}

import strategicprimer.model.common.map {
    IMutableMapNG
}

"""A factory for the hackish driver to fix missing content in the map, namely units with
   "TODO" for their "kind" and aquatic villages with non-aquatic races."""
service(`interface DriverFactory`)
shared class TodoFixerFactory() satisfies ModelDriverFactory {
    shared actual IDriverUsage usage = DriverUsage(false, ["fix-todos"],
        ParamCount.atLeastOne, "Fix TODOs in maps",
        "Fix TODOs in unit kinds and aquatic villages with non-aquatic races", false,
        false);

    shared actual ModelDriver createDriver(ICLIHelper cli, SPOptions options,
            IDriverModel model) => TodoFixerCLI(cli, model);

    shared actual IDriverModel createModel(IMutableMapNG map, PathWrapper? path) =>
            SimpleMultiMapModel(map, path);
}
