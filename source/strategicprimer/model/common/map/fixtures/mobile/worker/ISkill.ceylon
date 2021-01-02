import lovelace.util.common {
    todo
}
import strategicprimer.model.common.map {
    Subsettable,
    HasName
}

"An interface for Skills."
todo("Split mutators into a separate interface?")
shared interface ISkill satisfies HasName&Subsettable<ISkill> {
    "How many levels the worker has in the skill."
    shared formal Integer level;

    "How many hours of experience the worker has accumulated since the skill level last
     increased."
    shared formal Integer hours;

    "Clone the skill."
    shared formal ISkill copy();

    """A skill is "empty" if the worker has no levels in it and no hours of experience in
       it."""
    shared default Boolean empty => level == 0 && hours == 0;

    "A skill is a subset if it has the same name, equal or lower level, and if equal level
     equal or lower hours."
    shared actual default Boolean isSubset(ISkill obj, Anything(String) report) {
        if (obj.name == name) {
            if (obj.level > level) {
                report("Extra level(s) in ``name``");
                return false;
            } else if (obj.level == level, obj.hours > hours) {
                report("Extra hours in ``name``");
                return false;
            } else {
                return true;
            }
        } else {
            report("Called with non-corresponding skill, ``obj.name`` (this is ``name``");
            return false;
        }
    }
}
