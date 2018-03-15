import ceylon.collection {
    MutableList,
    ArrayList,
    MutableMap,
    HashMap
}
import ceylon.language.meta {
    classDeclaration,
	type
}
import ceylon.math.decimal {
    decimalNumber,
    Decimal
}
import ceylon.math.whole {
    Whole
}

import java.lang {
    IllegalStateException
}
import strategicprimer.model.map {
    IFixture,
    IMutableMapNG,
    TileFixture,
	Point,
	HasPopulation,
	HasExtent
}
import strategicprimer.model.map.fixtures {
    ResourcePile,
    Quantity,
    Implement
}
import strategicprimer.model.map.fixtures.mobile {
    IUnit,
    Animal
}
import strategicprimer.model.map.fixtures.resources {
    CacheFixture,
	Grove,
	Meadow,
	Shrub,
	FieldStatus
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
import lovelace.util.common {
	NonNullCorrespondence
}
import ceylon.language.meta.model {
	ClassOrInterface
}
import strategicprimer.model.map.fixtures.terrain {
	Forest
}
"""A driver to remove duplicate hills, forests, etc. from the map (to reduce the size it
   takes up on disk and the memory and CPU it takes to deal with it)."""
shared object duplicateFixtureRemoverCLI satisfies SimpleCLIDriver {
    shared actual IDriverUsage usage = DriverUsage {
        graphical = false;
        invocations = ["-u", "--duplicates"];
        paramsWanted = ParamCount.one;
        shortDescription = "Remove duplicate fixtures";
        longDescription = "Remove duplicate fixtures (identical except ID# and on the
                           same tile) from a map.";
        supportedOptionsTemp = [ "--current-turn=NN" ];
    };
    void ifApplicable<Desired, Provided>(Anything(Desired) func)(Provided item) {
        if (is Desired item) {
            func(item);
        }
    }
    """"Remove" (at first we just report) duplicate fixtures (i.e. hills, forests, of the same
       kind, oases, etc.---we use [[TileFixture.equalsIgnoringID]]) from every tile in a
       map."""
    void removeDuplicateFixtures(IMutableMapNG map, ICLIHelper cli) {
        Boolean approveRemoval(Point location, TileFixture fixture, TileFixture matching) {
            String fCls = classDeclaration(fixture).name;
            String mCls = classDeclaration(matching).name;
            return cli.inputBooleanInSeries(
                "At ``location``: Remove '``fixture.shortDescription``', of class '``fCls``', ID #``
		            fixture.id``, which matches '``matching.shortDescription``', of class '``mCls
		            ``', ID #``matching.id``?", "duplicate``fCls````mCls``");
        }
        for (location in map.locations) {
            MutableList<TileFixture> fixtures = ArrayList<TileFixture>();
            MutableList<TileFixture> toRemove = ArrayList<TileFixture>();
            String context = "At ``location``: ";
            //        for (fixture in map.fixtures[location]) { // TODO: syntax sugar once compiler bug fixed
            for (fixture in map.fixtures.get(location)) {
                if (is IUnit fixture, fixture.kind.contains("TODO")) {
                    continue;
                } else if (is CacheFixture fixture) {
                    continue;
                } else if (is HasPopulation fixture, fixture.population.positive) {
                    continue;
                } else if (is HasExtent fixture, fixture.acres.positive) {
                    continue;
                }
                if (exists matching = fixtures.find(fixture.equalsIgnoringID),
                    approveRemoval(location, fixture, matching)) {
                    toRemove.add(fixture);
                } else {
                    fixtures.add(fixture);
                    if (is IUnit fixture) {
                        coalesceResources(context, fixture, cli, ifApplicable(fixture.addMember),
                            ifApplicable(fixture.removeMember));
                    } else if (is Fortress fixture) {
                        coalesceResources(context, fixture, cli, ifApplicable(fixture.addMember),
	                        ifApplicable(fixture.removeMember));
                    }
                }
            }
            for (fixture in toRemove) {
                map.removeFixture(location, fixture);
            }
            coalesceResources(context, map.fixtures.get(location), cli,
                ifApplicable<TileFixture, IFixture>((fix) => map.addFixture(location, fix)),
                ifApplicable<TileFixture, IFixture>((fix) => map.removeFixture(location, fix)));
        }
    }
    class CoalescedHolder<Type,Key>(Key(Type) extractor, shared Type({Type+}) combiner)
            satisfies NonNullCorrespondence<Type, MutableList<Type>>&{List<Type>*}
            given Type satisfies IFixture given Key satisfies Object {
        MutableMap<Key, MutableList<Type>> map = HashMap<Key, MutableList<Type>>();
        shared actual Boolean defines(Type key) => true;
        shared variable String plural = "unknown";
        shared actual MutableList<Type> get(Type item) {
            Key key = extractor(item);
            plural = item.plural;
            if (exists retval = map[key]) {
                return retval;
            } else {
                MutableList<Type> retval = ArrayList<Type>();
                map[key] = retval;
                return retval;
            }
        }
        shared actual Iterator<List<Type>> iterator() => map.items.iterator();
        shared void addIfType(Anything item) {
            if (is Type item) {
                get(item).add(item);
            }
        }
        shared Type combineRaw({IFixture+} list) {
            assert (is {Type+} list);
            return combiner(list);
        }
    }
    "Offer to combine like resources in a unit or fortress."
    void coalesceResources(String context, {IFixture*} stream, ICLIHelper cli, Anything(IFixture) add,
	        Anything(IFixture) remove) {
        Map<ClassOrInterface<IFixture>, CoalescedHolder<out IFixture, out Object>> mapping = map {
            `ResourcePile`->CoalescedHolder<ResourcePile, [String, String, String, Integer]>(
                (pile) => [pile.kind, pile.contents, pile.quantity.units, pile.created], combineResources),
            `Animal`->CoalescedHolder<Animal, [String, String, Integer]>(
                (animal) => [animal.kind, animal.status, animal.born], combineAnimals),
            `Implement`->CoalescedHolder<Implement, String>(Implement.kind, combineEquipment),
            `Forest`->CoalescedHolder<Forest, [String, Boolean]>((forest) => [forest.kind, forest.rows],
                combineForests),
            `Grove`->CoalescedHolder<Grove, [Boolean, Boolean, String]>(
                (grove) => [grove.orchard, grove.cultivated, grove.kind], combineGroves),
            `Meadow`->CoalescedHolder<Meadow, [String, Boolean, Boolean, FieldStatus]>(
                (meadow) => [meadow.kind, meadow.field, meadow.cultivated, meadow.status], combineMeadows),
            `Shrub`->CoalescedHolder<Shrub, String>(Shrub.kind, combineShrubs)
        };
        for (fixture in stream) {
            if (is {IFixture*} fixture) {
                String shortDesc;
                if (is TileFixture fixture) {
                    shortDesc = fixture.shortDescription;
                } else {
                    shortDesc = fixture.string;
                }
                if (is IUnit fixture) {
                    coalesceResources(context + "In ``shortDesc``: ", fixture, cli,
                        ifApplicable(fixture.addMember),ifApplicable(fixture.removeMember));
                } else if (is Fortress fixture) {
                    coalesceResources(context + "In ``shortDesc``: ", fixture, cli,
                        ifApplicable(fixture.addMember),ifApplicable(fixture.removeMember));
                }
            } else if (is Animal fixture) {
                if (fixture.traces || fixture.talking) {
                    continue;
                }
                if (exists handler = mapping[`Animal`]) {
                    handler.addIfType(fixture);
                }
            } else if (is HasPopulation fixture, fixture.population < 0) {
                continue;
            } else if (is HasExtent fixture, !fixture.acres.positive) {
                continue;
            } else if (exists handler = mapping[type(fixture)]) {
                handler.addIfType(fixture);
            }
        }
        for (helper in mapping.items) {
            for (list in helper.map((it) => it.sequence())) {
                if (list.size <= 1) {
                    continue;
                }
                assert (nonempty list);
                cli.print(context);
                cli.println("The following ``helper.plural.lowercased`` could be combined:");
                for (item in list) {
                    cli.println(item.string);
                }
                if (cli.inputBoolean("Combine them? ")) {
                    IFixture combined = helper.combineRaw(list);
                    for (item in list) {
                        remove(item);
                    }
                    add(combined);
                }
            }
        }
    }
    Decimal decimalize(Number<out Anything> num) {
        assert (is Decimal|Whole|Integer|Float num);
        switch (num)
        case (is Decimal) {
            return num;
        }
        case (is Whole|Integer|Float) {
            return decimalNumber(num);
        }
    }
    "Combine like [[Forest]]s into a single object. We assume that all Forests are of the
     same kind of tree and either all or none are in rows."
    Forest combineForests({Forest*} list) {
        assert (exists top = list.first);
        return Forest(top.kind, top.rows, top.id,
            list.map(Forest.acres).map(decimalize).fold(decimalNumber(0))(plus));
    }
    "Combine like [[Meadow]]s into a single object. We assume all Meadows are identical except for acreage and ID."
    Meadow combineMeadows({Meadow*} list) {
        assert (exists top = list.first);
        return Meadow(top.kind, top.field, top.cultivated, top.id, top.status,
            list.map(Meadow.acres).map(decimalize).fold(decimalNumber(0))(plus));
    }
    "Combine like [[Grove]]s into a single object. We assume all Groves are identical except for population and ID."
    // TODO: combine these now-identical methods: easier said than done given how they are called
    Grove combineGroves({Grove+} list) => list.rest.fold(list.first)((Grove one, Grove two) => one.combined(two));
    "Combine like [[Shrub]]s into a single object. We assume all Shrubs are of the same kind."
    Shrub combineShrubs({Shrub+} list) => list.rest.fold(list.first)((one, two) => one.combined(two));
    "Combine like [[Implement]]s into a single object. We assume that all Implements are of
     the same kind."
    Implement combineEquipment({Implement+} list) => list.rest.fold(list.first)((one, two) => one.combined(two));
    "Combine like Animals into a single Animal population. We assume that all animals have the
     same kind, domestication status, and turn of birth."
    Animal combineAnimals({Animal+} list) => list.rest.fold(list.first)((one, two) => one.combined(two));
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
