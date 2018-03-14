"Various converters for the [Strategic
 Primer](https://shinecycle.wordpress.com/archives/strategic-primer) assistive programs
 suite."
license("GPL-3")
native("jvm")
module strategicprimer.drivers.converters "0.4.9016" {
    import ceylon.collection "1.3.3";
    shared import strategicprimer.model "0.4.9016";
    import ceylon.test "1.3.3";
    import lovelace.util.common "0.1.0";
    import lovelace.util.jvm "0.1.0";
    import strategicprimer.drivers.common "0.4.9016";
    import ceylon.logging "1.3.3";
    import ceylon.regex "1.3.3";
    import ceylon.file "1.3.3";
    shared import strategicprimer.drivers.exploration.old "0.4.9016";
    import ceylon.random "1.3.3";
}
