"Common functionality shared between the exploration CLI and the exploration GUI."
// TODO: Write user-introductory documentation for this module
license("GPL-3")
native("jvm")
module strategicprimer.drivers.exploration.common "0.4.9019" {
    value ceylonVersion = "1.3.3";
    value spVersion = "0.4.9019";
    shared import strategicprimer.drivers.common spVersion;
    import strategicprimer.model.common spVersion;
    import ceylon.collection ceylonVersion;
    import ceylon.numeric ceylonVersion;
    import ceylon.test ceylonVersion;
    import ceylon.random ceylonVersion;
    import ceylon.logging ceylonVersion;
}
