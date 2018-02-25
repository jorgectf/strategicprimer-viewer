import ceylon.math.decimal {
    Decimal
}
import ceylon.math.whole {
    Whole
}

import java.lang {
    IllegalArgumentException
}

import strategicprimer.model.map {
    Subsettable
}

import lovelace.util.common {
    comparingOn,
	Comparator
}
shared alias SPNumber=>Integer|Float|Decimal|Whole;
shared object numberComparator satisfies Comparator<SPNumber> {
	"Convert an arbitrary Number to a Float."
	shared Float floatValue(Number<out Anything> number) {
		switch (number)
		case (is Integer) { return number.float; }
		case (is Float) { return number; }
		case (is Decimal) { return number.float; }
		case (is Whole) { return number.float; }
		else { throw IllegalArgumentException("Unknown Number type"); }
	}
	shared actual Comparison compare(/*Number<out Anything>*/SPNumber one,
		/*Number<out Anything>*/SPNumber two) {
		if (is Integer one, is Integer two) {
			return one <=> two;
		} else if (is Float one, is Float two) {
			return one <=> two;
		} else if (is Decimal one, is Decimal two) {
			return one <=> two;
		} else if (is Whole one, is Whole two) {
			return one <=> two;
		} else {
			Float oneValue = floatValue(one);
			Float twoValue = floatValue(two);
			return oneValue <=> twoValue;
		}
	}
}
"A number paired with its units. This class is immutable."
shared class Quantity
        satisfies Subsettable<Quantity>&Comparable<Quantity> {
    "The numeric quantity."
    shared /*Number<out Object>*/SPNumber number;
    "The units in which that number is measured."
    shared String units;
	shared new (SPNumber number, String units) {
		this.number = number;
		this.units = units;
	}
	"That quantity as a Float"
	shared Float floatNumber => numberComparator.floatValue(number);
    shared actual String string => "``number`` ``units``";
    "A Quantity is a subset iff it has the same units and either the same or a lesser
     quantity."
    shared actual Boolean isSubset(Quantity obj, Anything(String) report) {
        if (units == obj.units) {
            if (numberComparator.compare(number, obj.number) == smaller) {
                report("Has greater quantity than we do");
                return false;
            } else {
                return true;
            }
        } else {
            report("Units differ");
            return false;
        }
    }
    shared actual Boolean equals(Object obj) {
        if (is Quantity obj) {
            return units == obj.units && number == obj.number;
        } else {
            return false;
        }
    }
    shared actual Integer hash => units.hash.or(number.hash);
    shared actual Comparison compare(Quantity quantity) =>
            comparing(comparingOn(Quantity.units, (String x, String y) => x <=> y),
                comparingOn(Quantity.number, numberComparator.compare))(this, quantity);
}
