import ceylon.collection {
    MutableList,
    ArrayList,
    MutableMap,
    HashMap
}
import ceylon.language.meta {
    classDeclaration
}
import ceylon.math.decimal {
    decimalNumber,
    Decimal
}
import ceylon.math.whole {
    Whole
}

import java.lang {
    JNumber=Number,
    JInteger=Integer,
    JLong=Long,
    JFloat=Float,
    JDouble=Double,
    IllegalStateException
}
import java.math {
    BigDecimal,
    BigInteger
}

import strategicprimer.model.map {
    IFixture,
    IMutableMapNG,
    TileFixture
}
import strategicprimer.model.map.fixtures {
    ResourcePile,
    Quantity
}
import strategicprimer.model.map.fixtures.mobile {
    IUnit,
    Animal
}
import strategicprimer.model.map.fixtures.resources {
    CacheFixture
}
import strategicprimer.model.map.fixtures.towns {
    Fortress
}
import strategicprimer.drivers.common.cli {
    ICLIHelper
}
import strategicprimer.drivers.common {
    SPOptions,
    DriverUsage,
    IMultiMapModel,
    SimpleCLIDriver,
    ParamCount,
    IDriverUsage,
    IDriverModel,
    DriverFailedException
}
import java.io {
    IOException
}

""""Remove" (at first we just report) duplicate fixtures (i.e. hills, forests, of the same
    kind, oases, etc.---we use [[TileFixture.equalsIgnoringID]]) from every tile in a
    map."""
void removeDuplicateFixtures(IMutableMapNG map, ICLIHelper cli) {
    Boolean approveRemoval(TileFixture fixture, TileFixture matching) {
        return cli.inputBooleanInSeries(
            "Remove '``fixture.shortDescription``', of class '``classDeclaration(fixture)
                .name``', ID #``fixture.id``, which matches '``matching
                .shortDescription``', of class '``classDeclaration(matching).name
                ``', ID #``matching.id``?", "duplicate");
    }
    for (location in map.locations) {
        MutableList<TileFixture> fixtures = ArrayList<TileFixture>();
        MutableList<TileFixture> toRemove = ArrayList<TileFixture>();
//        for (fixture in map.fixtures[location]) { // TODO: syntax sugar once compiler bug fixed
        for (fixture in map.fixtures.get(location)) {
            if (is IUnit fixture, fixture.kind.contains("TODO")) {
                continue;
            } else if (is CacheFixture fixture) {
                continue;
            }
            if (exists matching = fixtures.find((fixture.equalsIgnoringID)),
                    approveRemoval(fixture, matching)) {
                toRemove.add(fixture);
            } else {
                fixtures.add(fixture);
                if (is {IFixture*} fixture) {
                    coalesceResources(fixture, cli);
                }
            }
        }
        for (fixture in toRemove) {
            map.removeFixture(location, fixture);
        }
    }
}
"Offer to combine like resources in a unit or fortress."
void coalesceResources({IFixture*} stream, ICLIHelper cli) {
    MutableMap<[String, String, String, Integer], MutableList<ResourcePile>> resources =
            HashMap<[String, String, String, Integer], MutableList<ResourcePile>>();
    MutableMap<[String, String, Integer], MutableList<Animal>> animals =
            HashMap<[String, String, Integer], MutableList<Animal>>();
    for (fixture in stream) {
        if (is {IFixture*} fixture) {
            coalesceResources(fixture, cli);
        } else if (is ResourcePile fixture) {
            [String, String, String, Integer] key = [fixture.kind, fixture.contents,
                fixture.quantity.units, fixture.created];
            MutableList<ResourcePile> list;
            if (exists temp = resources[key]) {
                list = temp;
            } else {
                list = ArrayList<ResourcePile>();
//                resources[key] = list; // TODO: report backend-error bug
                resources.put(key, list);
            }
            list.add(fixture);
        } else if (is Animal fixture) {
            if (fixture.traces || fixture.talking) {
                continue;
            }
            [String, String, Integer] key = [fixture.kind, fixture.status, fixture.born];
            MutableList<Animal> list;
            if (exists temp = animals[key]) {
                list = temp;
            } else {
                list = ArrayList<Animal>();
//                animals[key] = list; TODO: report backend-error bug
                animals.put(key, list);
            }
            list.add(fixture);
        }
    }
    if (!stream is IUnit|Fortress) {
        // We can't add items to or remove them from any other iterable
        return;
    }
    for (list in resources.items) {
        if (list.size <= 1) {
            continue;
        }
        cli.println("The following items could be combined:");
        for (item in list) {
            cli.println(item.string);
        }
        if (cli.inputBooleanInSeries("Combine them? ")) {
            ResourcePile combined = combineResources(list);
            if (is IUnit stream) {
                for (item in list) {
                    stream.removeMember(item);
                }
                stream.addMember(combined);
            } else if (is Fortress stream) {
                for (item in list) {
                    stream.removeMember(item);
                }
                stream.addMember(combined);
            }
        }
    }
    for (list in animals.items) {
        if (list.size <= 1) {
            continue;
        }
        cli.println("The following animals could be grouped into one population:");
        for (item in list) {
            cli.println(item.string);
        }
        if (cli.inputBooleanInSeries("Group these animals together? ")) {
            Animal combined = combineAnimals(list);
            if (is IUnit stream) {
                for (item in list) {
                    stream.removeMember(item);
                }
                stream.addMember(combined);
            }
            // Can't add animals to any other kind of stream.
        }
    }
}
"Combine like Animals into a single Animal population. We assume that all animals have the
 same kind, domestication status, and turn of birth."
Animal combineAnimals({Animal*} list) {
    assert (exists top = list.first);
    return Animal(top.kind, false, false, top.status, top.id, top.born,
        list.map(Animal.population).fold(0)(plus));
}
"Combine like resources into a single resource pile. We assume that all resources have
 the same kind, contents, units, and created date."
ResourcePile combineResources({ResourcePile*} list) {
    assert (exists top = list.first);
    ResourcePile combined = ResourcePile(top.id, top.kind,
        top.contents, Quantity(list
            .map(ResourcePile.quantity).map(Quantity.number)
            .map((num) {
                if (is Decimal num) {
                    return num;
                } else if (is Integer|Float|Whole num) {
                    return decimalNumber(num);
                } else {
                    throw IllegalStateException("Can't get here");
                }
            }).fold(decimalNumber(0))(
                    (Decimal partial, Decimal element) => partial.plus(element)),
            top.quantity.units));
    combined.created = top.created;
    return combined;
}
"Convert a Number of any known subclass to a BigDecimal."
BigDecimal toBigDecimal(JNumber number) {
    switch (number)
    case (is BigDecimal) { return number; }
    case (is BigInteger) { return BigDecimal(number); }
    case (is JInteger|JLong) { return BigDecimal.valueOf(number.longValue()); }
    case (is JFloat|JDouble) { return BigDecimal.valueOf(number.doubleValue()); }
    else { return BigDecimal(number.string); }
}
"""A driver to remove duplicate hills, forests, etc. from the map (to reduce the size it
   takes up on disk and the memory and CPU it takes to deal with it)."""
shared object duplicateFixtureRemoverCLI satisfies SimpleCLIDriver {
    shared actual IDriverUsage usage = DriverUsage {
        graphical = false;
        shortOption = "-u";
        longOption = "--duplicates";
        paramsWanted = ParamCount.one;
        shortDescription = "Remove duplicate fixtures";
        longDescription = "Remove duplicate fixtures (identical except ID# and on the
                           same tile) from a map.";
        supportedOptionsTemp = [ "--current-turn=NN" ];
    };
    "Run the driver"
    shared actual void startDriverOnModel(ICLIHelper cli, SPOptions options,
            IDriverModel model) {
        try {
            if (is IMultiMapModel model) {
                for (pair in model.allMaps) {
                    removeDuplicateFixtures(pair.first, cli);
                }
            } else {
                removeDuplicateFixtures(model.map, cli);
            }
        } catch (IOException except) {
            throw DriverFailedException(except, "I/O error interacting with user");
        }
    }
}
