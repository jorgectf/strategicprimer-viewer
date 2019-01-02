"Model objects, and their XML I/O, for the [Strategic
 Primer](https://strategicprimer.wordpress.com) assitive programs suite. Some of the
 converter apps, being tightly bound to XML I/O and to implementation details of the
 model, also have code in this module."
license("GPL-3")
// TODO: Write user-introductory documentation for this module
// TODO: Make only the I/O parts "native"; blocked by eclipse/ceylon#7336
native("jvm")
module strategicprimer.model.impl "0.4.9017" {
    value ceylonVersion = "1.3.3";
    value javaVersion = "8";
    value lovelaceUtilsVersion = "0.1.0";
    value spVersion = "0.4.9017";
    shared import strategicprimer.model.common spVersion;
    shared import java.base javaVersion;
    shared import javax.xml javaVersion;
    import ceylon.test ceylonVersion;
    shared import ceylon.collection ceylonVersion;
    shared import lovelace.util.common lovelaceUtilsVersion;
    import ceylon.logging ceylonVersion;
    shared import ceylon.numeric ceylonVersion;
    import lovelace.util.jvm lovelaceUtilsVersion;
    import ceylon.regex ceylonVersion;
    shared import ceylon.random ceylonVersion;
    shared import ceylon.file ceylonVersion;
    shared import com.vasileff.ceylon.structures "1.1.3";
    import ceylon.decimal ceylonVersion;
    import ceylon.whole ceylonVersion;
    import ceylon.dbc ceylonVersion;
    import maven:"org.xerial:sqlite-jdbc" "3.23.1";
}
