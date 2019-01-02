"Functions to generate HTML and tabular (CSV) reports of the contents of [Strategic
 Primer](https://strategicprimer.wordpress.com) maps."
// TODO: Add user-introductory documentation, like that of the SDK
license("GPL-3")
native("jvm")
module strategicprimer.report "0.4.9017" {
    value ceylonVersion = "1.3.3";
    value javaVersion = "8";
    value lovelaceUtilsVersion = "0.1.0";
    import lovelace.util.jvm lovelaceUtilsVersion;
    import ceylon.regex ceylonVersion;
    import java.base javaVersion;
    import ceylon.interop.java ceylonVersion;
    shared import java.desktop javaVersion;
    import ceylon.collection ceylonVersion;
    import ceylon.logging ceylonVersion;
    import ceylon.numeric ceylonVersion;
    shared import strategicprimer.model.common "0.4.9017";
    shared import lovelace.util.common lovelaceUtilsVersion;
    import ceylon.test ceylonVersion;
    import ceylon.random ceylonVersion;
    import com.vasileff.ceylon.structures "1.1.3";
    import ceylon.decimal ceylonVersion;
}
