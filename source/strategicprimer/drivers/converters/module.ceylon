"Various converters for the [Strategic
 Primer](https://strategicprimer.wordpress.com) assistive programs
 suite."
license("GPL-3")
native("jvm")
module strategicprimer.drivers.converters "0.4.9016" {
    value ceylonVersion = "1.3.3";
    value lovelaceUtilsVersion = "0.1.0";
    value spVersion = "0.4.9016";
    import ceylon.collection ceylonVersion;
    shared import strategicprimer.model spVersion;
    import ceylon.test ceylonVersion;
    import lovelace.util.common lovelaceUtilsVersion;
    import lovelace.util.jvm lovelaceUtilsVersion;
    import strategicprimer.drivers.common spVersion;
    import ceylon.logging ceylonVersion;
    import ceylon.regex ceylonVersion;
    import ceylon.file ceylonVersion;
    shared import strategicprimer.drivers.exploration.old spVersion;
    import ceylon.random ceylonVersion;
}
