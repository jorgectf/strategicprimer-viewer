// TODO: Write user-introductory documentation for this module
native("jvm") // TODO: remove this
module strategicprimer.mining "0.4.9019" {
    value ceylonVersion = "1.3.3";
    value spVersion = "0.4.9019";
    value lovelaceUtilsVersion = "0.1.1";
    import lovelace.util.common lovelaceUtilsVersion;
    native("jvm")
    shared import ceylon.file ceylonVersion;
    native("jvm")
    shared import strategicprimer.drivers.common spVersion;
    import ceylon.collection ceylonVersion;
}
