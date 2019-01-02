"Common classes used by both the worker-management and advancement apps from the
 [Strategic Primer](https://strategicprimer.wordpress.com) assistive programs suite, and
 possibly even CLI versions Any class extending a Swing class should go elsewhere, at
 least until [the Ceylon tooling bug](https://github.com/eclipse/ceylon/issues/6986) is
 fixed."
// TODO: Write user-introductory documentation of the module.
license("GPL-3")
native("jvm")
module strategicprimer.drivers.worker.common "0.4.9017" {
    value ceylonVersion = "1.3.3";
    value javaVersion = "8";
    value spVersion = "0.4.9017";
    shared import java.base javaVersion;
    import ceylon.collection ceylonVersion;
    shared import strategicprimer.drivers.common spVersion;
    shared import strategicprimer.model.common spVersion;
    import ceylon.test ceylonVersion;
    shared import java.desktop javaVersion;
    import ceylon.logging ceylonVersion;
    import ceylon.random ceylonVersion;
}
