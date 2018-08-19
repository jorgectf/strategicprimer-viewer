import strategicprimer.model.map {
	Player,
	TileType,
	IMapNG,
	Point,
	HasOwner,
	MapDimensions,
	IFixture,
	HasPopulation,
	TileFixture
}
import ceylon.collection {
	MutableSet,
	LinkedList,
	ArrayList,
	MutableList,
	HashSet,
	Queue
}
import java.io {
	IOException
}
import strategicprimer.drivers.common {
	SPOptions,
	ParamCount,
	IDriverUsage,
	SimpleCLIDriver,
	DriverUsage,
	IDriverModel,
	IMultiMapModel,
	ISPDriver
}
import strategicprimer.model.map.fixtures {
	Ground,
	Quantity
}
import strategicprimer.drivers.common.cli {
	ICLIHelper
}
import lovelace.util.common {
	todo,
	matchingValue,
	simpleMap,
	defer
}
import strategicprimer.model.map.fixtures.terrain {
	Forest
}
import ceylon.numeric.float {
	ceiling,
	sqrt,
	round=halfEven
}
import strategicprimer.model.map.fixtures.mobile {
	IWorker,
	Animal,
	AnimalTracks
}
import strategicprimer.drivers.exploration.common {
	surroundingPointIterable,
	pathfinder
}
import strategicprimer.model {
	DistanceComparator
}
import java.nio.file {
	JPath=Path
}
import strategicprimer.viewer.drivers.exploration {
	HuntingModel
}
import ceylon.logging {
	logger,
	Logger
}
import strategicprimer.model.map.fixtures.towns {
	ITownFixture,
	TownStatus,
	Village
}
import strategicprimer.model.map.fixtures.resources {
	Grove,
	Shrub,
	Meadow
}
"A logger."
Logger log = logger(`module strategicprimer.viewer`);
"A helper object for the query driver."
class QueryHelper {
	"How many hours we assume a working day is for a hunter or such."
	static Integer hunterHours = 10;
	"How many encounters per hour for a hunter or such."
	static Integer hourlyEncounters = 4;
	"Count the workers in an Iterable belonging to a player."
	static Integer countWorkersInIterable(Player player, {IFixture*} fixtures) {
		variable Integer retval = 0;
		for (fixture in fixtures) {
			if (is IWorker fixture, is HasOwner fixtures, player == fixtures.owner) {
				retval++;
			} else if (is {IFixture*} fixture) {
				retval += countWorkersInIterable(player, fixture);
			}
		}
		return retval;
	}
	"The distance between two points in a map with the given dimensions."
	static Float distance(Point base, Point destination, MapDimensions dimensions) {
		Integer rawXDiff = base.row - destination.row;
		Integer rawYDiff = base.column - destination.column;
		Integer xDiff;
		if (rawXDiff < (dimensions.rows / 2)) {
			xDiff = rawXDiff;
		} else {
			xDiff = dimensions.rows - rawXDiff;
		}
		Integer yDiff;
		if (rawYDiff < (dimensions.columns / 2)) {
			yDiff = rawYDiff;
		} else {
			yDiff = dimensions.columns - rawYDiff;
		}
		return sqrt((xDiff * xDiff + yDiff * yDiff).float);
	}
	"A description of what could be a single animal or a population of animals."
	static String populationDescription(Animal animal) {
		if (animal.population > 1) { // TODO: Use proper plural
			return "a group of perhaps ``animal.population`` ``animal.kind``";
		} else {
			return animal.kind;
		}
	}
	"If argument is a meadow, its status in the format used below; otherwise the empty
	 string."
	static String meadowStatus(Anything argument) {
		if (is Meadow argument) {
			return " (``argument.status``)";
		} else {
			return "";
		}
	}
	"Print the String representation of a Float to one decimal place."
	static String formatFloat(Float num) => Float.format(num, 0, 1);
	IDriverModel model;
	IMapNG map;
	ICLIHelper cli;
	HuntingModel huntModel;
	SPOptions options;
	shared new (IDriverModel theModel, ICLIHelper theCLI, HuntingModel theHuntModel,
			SPOptions theOptions) {
		model = theModel;
		map = theModel.map;
		cli = theCLI;
		huntModel = theHuntModel;
		options = theOptions;
	}
	"Count the workers belonging to a player."
	void countWorkers({Player*} players) {
		Player[] playerList = players.sequence();
		value choice = cli.chooseFromList(playerList,
			"Players in the map:", "Map contains no players",
			"Owner of workers to count: ", true);
		if (exists player = choice.item) {
			Integer count = countWorkersInIterable(player,
				map.fixtureEntries.map(Entry.item));
			cli.println("``player.name`` has ``count`` workers");
		}
	}
	"Report the distance between two points."
	void printDistance() {
		Point start = cli.inputPoint("Starting point:\t");
		Point end = cli.inputPoint("Destination:\t");
		if (cli.inputBoolean("Compute ground travel distance?")) {
			cli.println("Distance (on the ground, in MP cost):\t``pathfinder.
			getTravelDistance(map, start, end).first``");
		} else {
			cli.println("Distance (as the crow flies, in tiles):\t``Float
				.format(distance(start, end, map.dimensions), 0, 0)``");
		}
	}
	"If the given driver model *has* subordinate maps, add a copy of the given fixture to
	 them at the given location iff no fixture with the same ID is already there."
	void addToSubMaps(Point point, TileFixture fixture, Boolean zero) {
		if (is IMultiMapModel model) {
			for (map->file in model.subordinateMaps) {
				if (!map.fixtures.get(point).any(matchingValue(fixture.id, TileFixture.id))) {
					map.addFixture(point, fixture.copy(zero));
				}
			}
		}
	}
	"Reduce the population of a group of plants, animals, etc., and copy the reduced form
	 into all subordinate maps."
	void reducePopulation(Point point, HasPopulation<out TileFixture>&TileFixture fixture,
			String plural, Boolean zero) {
		Integer count = Integer.smallest(cli.inputNumber("How many ``plural`` to remove: "),
			fixture.population);
		if (count > 0) {
			model.map.removeFixture(point, fixture);
			Integer remaining = fixture.population - count;
			if (remaining > 0) {
				value addend = fixture.reduced(remaining);
				model.map.addFixture(point, addend);
				if (is IMultiMapModel model) {
					for (map->file in model.subordinateMaps) {
						if (exists found = map.fixtures.get(point)
							.find(shuffle(curry(fixture.isSubset))(noop))) {
							map.removeFixture(point, found);
						}
						map.addFixture(point, addend.copy(zero));
					}
				}
			} else if (is IMultiMapModel model) {
				for (map->file in model.subordinateMaps) {
					if (exists found = map.fixtures.get(point)
						.find(shuffle(curry(fixture.isSubset))(noop))) {
						map.removeFixture(point, found);
					}
				}
			}
		} else {
			addToSubMaps(point, fixture, zero);
		}
	}
	void huntGeneral(
			"How much time is left in the day."
			variable Integer time,
			"How much time is deducted when nothing is found."
			Integer noResultCost,
			"""What verb to use to ask how long an encounter took: "gather", "fight",
			   "process", e.g."""
			String verb,
			"The source of encounters."
			variable {<Point->Animal|AnimalTracks|HuntingModel.NothingFound>*} encounters) {
		while (time > 0, exists loc->encounter = encounters.first) {
			encounters = encounters.rest;
			if (is HuntingModel.NothingFound encounter) {
				cli.println("Found nothing for the next ``noResultCost`` minutes.");
				time -= noResultCost;
			} else if (is AnimalTracks encounter) { // TODO: Is this case actually possible?
				addToSubMaps(loc, encounter, true);
				cli.println("Found only tracks or traces from ``
				encounter.kind`` for the next ``noResultCost`` minutes.");
				time -= noResultCost;
			} else if (cli.inputBooleanInSeries("Found ``populationDescription(encounter)
					``. Should they ``verb``?", encounter.kind)) {
				Integer cost = cli.inputNumber("Time to ``verb``: ");
				time -= cost;
				if (cli.inputBooleanInSeries("Handle processing now?")) {
					// TODO: somehow handle processing-in-parallel case
					for (i in 0:(cli.inputNumber("How many animals?"))) {
						Integer mass = cli.inputNumber("Weight of this animal's meat in pounds: ");
						Integer hands = cli.inputNumber("# of workers processing this carcass: ");
						time -= round(HuntingModel.processingTime(mass) / hands).integer;
					}
				}
				if (cli.inputBooleanInSeries("Reduce animal group population of ``
						encounter.population``?")) {
					reducePopulation(loc, encounter, "animals", true);
				} else {
					addToSubMaps(loc, encounter, true);
				}
				cli.println("``time`` minutes remaining.");
			} else {
				addToSubMaps(loc, encounter, true);
				time -= noResultCost;
			}
		}
	}
	"""Run hunting---that is, produce a list of "encounters"."""
	todo("Distinguish hunting from fishing in no-result time cost?")
	void hunt(Point point,
			"How long to spend hunting."
			Integer time) => huntGeneral(time, 60 / hourlyEncounters,
				"fight and process", huntModel.hunt(point));
	"""Run fishing---that is, produce a list of "encounters"."""
	void fish(Point point,
			"How long to spend hunting."
			Integer time) => huntGeneral(time, 60 / hourlyEncounters,
				"try to catch and process", huntModel.fish(point));
	"""Run food-gathering---that is, produce a list of "encounters"."""
	void gather(Point point, variable Integer time) {
		variable {<Point->Grove|Shrub|Meadow|HuntingModel.NothingFound>*} encounters =
				huntModel.gather(point);
		Integer noResultCost = 60 / hourlyEncounters;
		while (time > 0, exists loc->encounter = encounters.first) {
			encounters = encounters.rest;
			if (is HuntingModel.NothingFound encounter) {
				cli.println("Found nothing for the next ``noResultCost`` minutes.");
				time -= noResultCost;
				continue;
			} else if (cli.inputBooleanInSeries("Found ``encounter.shortDescription````
					meadowStatus(encounter)``. Should they gather?", encounter.kind)) {
				Integer cost = cli.inputNumber("Time to gather: ");
				time -= cost;
				// TODO: Once model supports remaining-quantity-in-fields data, offer to reduce it here
				if (is Shrub encounter, encounter.population > 0,
					cli.inputBooleanInSeries("Reduce shrub population here?")) {
					reducePopulation(loc, encounter, "plants", true);
					cli.println("``time`` minutes remaining.");
					continue;
				}
				cli.println("``time`` minutes remaining.");
			} else {
				time -= noResultCost;
			}
			addToSubMaps(loc, encounter, true);
		}
	}
	"""Handle herding mammals. Returns how many hours each herder spends "herding." """
	Integer herdMammals(MammalModel animal, Integer count, Integer flockPerHerder) {
		cli.println("Taking the day's two milkings together, tending the animals takes ``
			flockPerHerder * animal.dailyTimePerHead`` minutes, or ``flockPerHerder *
			(animal.dailyTimePerHead - 10)`` with expert herders, plus ``
			animal.dailyTimeFloor`` minutes to gather them");
		Quantity base = animal.scaledProduction(count);
		Float production = base.floatNumber;
		cli.println("This produces ``formatFloat(production)`` ``base.units``, ``
			formatFloat(animal.scaledPoundsProduction(count))`` lbs, of milk per day.`");
		Integer cost;
		if (cli.inputBooleanInSeries("Are the herders experts? ")) {
			cost = animal.dailyExpertTime(flockPerHerder);
		} else {
			cost = animal.dailyTime(flockPerHerder);
		}
		return (ceiling(cost / 60.0) + 0.1).integer;
	}
	"""Handle herding mammals. Returns how many hours each herder spends "herding." """
	Integer herdPoultry(PoultryModel bird, Integer count, Integer flockPerHerder) {
		cli.println("Gathering eggs takes ``bird
			.dailyTime(flockPerHerder)`` minutes; cleaning up after them,");
		cli.println("which should be done at least every ``bird.extraChoresInterval
			+ 1`` turns, takes ``formatFloat(bird
				.dailyExtraTime(flockPerHerder) / 60.0)`` hours.");
		Quantity base = bird.scaledProduction(count);
		Float production = base.floatNumber;
		cli.println("This produces ``Float.format(production, 0, 0)`` ``
			base.units``, totaling ``formatFloat(bird.scaledPoundsProduction(count))`` lbs.");
		Integer cost;
		if (cli.inputBooleanInSeries("Do they do the cleaning this turn? ")) {
			cost = bird.dailyTimePerHead + bird.extraTimePerHead;
		} else {
			cost = bird.dailyTimePerHead;
		}
		return (ceiling((flockPerHerder * cost) / 60.0) + 0.1).integer;
	}
	"Run herding."
	void herd() {
		HerdModel herdModel;
		if (cli.inputBooleanInSeries("Are these small animals, like sheep?\t")) {
			herdModel = MammalModel.smallMammals;
		} else if (cli.inputBooleanInSeries("Are these dairy cattle?\t")) {
			herdModel = MammalModel.dairyCattle;
		} else if (cli.inputBooleanInSeries("Are these chickens?\t")) {
			herdModel = PoultryModel.chickens;
		} else if (cli.inputBooleanInSeries("Are these turkeys?\t")) {
			herdModel = PoultryModel.turkeys;
		} else if (cli.inputBooleanInSeries("Are these pigeons?\t")) {
			herdModel = PoultryModel.pigeons;
		} else {
			herdModel = MammalModel.largeMammals;
		}
		Integer count = cli.inputNumber("How many animals?\t");
		if (count == 0) {
			cli.println("With no animals, no cost and no gain.");
			return;
		} else if (count < 0) {
			cli.println("Can't have a negative number of animals.");
			return;
		} else {
			Integer herders = cli.inputNumber("How many herders?\t");
			if (herders <= 0) {
				cli.println("Can't herd with no herders.");
				return;
			}
			Integer flockPerHerder = ((count + herders) - 1) / herders;
			Integer hours;
			if (is PoultryModel herdModel) {
				hours = herdPoultry(herdModel, count, flockPerHerder);
			} else {
				hours = herdMammals(herdModel, count, flockPerHerder);
			}
			if (hours < hunterHours,
				cli.inputBooleanInSeries("Spend remaining time as Food Gatherers? ")) {
				gather(cli.inputPoint("Gathering location? "), hunterHours - hours);
			}
		}
	}
	"Give the data about a tile that the player is supposed to automatically know if he
	 has a fortress on it."
	void fortressInfo(Point location) {
		cli.println("Terrain is ``map.baseTerrain[location] else "unknown"``");
		//        Ground[] ground = map.fixtures[location].narrow<Ground>().sequence();
		Ground[] ground = map.fixtures.get(location).narrow<Ground>().sequence();
		//        Forest[] forests = map.fixtures[location].narrow<Forest>().sequence();
		Forest[] forests = map.fixtures.get(location).narrow<Forest>().sequence();
		if (nonempty ground) {
			cli.println("Kind(s) of ground (rock) on the tile:");
			ground.map(Object.string).each(cli.println);
		}
		if (nonempty forests) {
			cli.println("Kind(s) of forests on the tile:");
			forests.map(Object.string).each(cli.println);
		}
	}
	"Find the nearest obviously-reachable unexplored location."
	Point? findUnexplored(Point base) {
		Queue<Point> queue = LinkedList<Point>();
		queue.offer(base);
		MapDimensions dimensions = map.dimensions;
		MutableSet<Point> considered = HashSet<Point>();
		MutableList<Point> retval = ArrayList<Point>();
		while (exists current = queue.accept()) {
			TileType? currentTerrain = map.baseTerrain[current];
			if (considered.contains(current)) {
				continue;
			} else if (exists currentTerrain) {
				if (currentTerrain != TileType.ocean) {
					Float baseDistance = distance(base, current, dimensions);
					for (neighbor in surroundingPointIterable(current, dimensions, 1)) {
						if (distance(base, neighbor, dimensions) >= baseDistance) {
							queue.offer(neighbor);
						}
					}
				}
			} else {
				retval.add(current);
			}
			considered.add(current);
		}
		return retval.sort(DistanceComparator(base, dimensions).compare).first;
	}
	"Print a list of active towns within the given distance of the given base that produce
	 any resources, and what resources they produce."
	void suggestTrade(Point base, Integer distance) {
		value comparator = DistanceComparator(base, map.dimensions);
		for (location in surroundingPointIterable(base, map.dimensions, distance).distinct
				.sort(comparator.compare)) {
			for (town in map.fixtures.get(location).narrow<ITownFixture>()) {
				//for (town in map.fixtures[location].narrow<ITownFixture>()) { // TODO: syntax sugar once compiler bug fixed
				if (town.status == TownStatus.active, exists population = town.population,
					!population.yearlyProduction.empty) {
					cli.print("At ``location````comparator.distanceString(location, "base")``: ");
					cli.print("``town.name``, a ``town.townSize`` ");
					if (is Village town, town.race != "human") {
						cli.print("``town.race`` village");
					} else {
						cli.print(town.kind);
					}
					cli.println(". Its yearly production:");
					for (resource in population.yearlyProduction) {
						String unitsString;
						if (resource.quantity.units.empty) {
							unitsString = "";
						} else if ("dozen" == resource.quantity.units) {
							unitsString = "dozen ";
						} else {
							unitsString = "``resource.quantity.units`` of ";
						}
						cli.println("- ``resource.kind``: ``resource.quantity.number`` ``
						unitsString````resource.contents``");
						if (resource.contents == "milk") {
							cli.println("- Corresponding livestock");
						} else if (resource.contents == "eggs") {
							cli.println("- Corresponding poultry");
						}
					}
				}
			}
		}
	}
	"Print a usage message for the REPL"
	void replUsage() {
		cli.println("The following commands are supported:");
		cli.println("help, ?: Print this list of commands.");
		cli.println(
			"fortress: Show what a player automatically knows about a fortress's tile.");
		cli.println("hunt, fish: Generate encounters with animals.`");
		cli.println(
			"gather: Generate encounters with fields, meadows, groves, orchards, or shrubs.");
		cli.println(
			"herd: Determine the output from and time required for maintaining a herd.");
		cli.println(
			"trap: Switch to a trap-modeling program to run trapping or fish-trapping.");
		cli.println("distance: Report the distance between two points.");
		cli.println("count: Count how many workers belong to a player.");
		cli.println("unexplored: Find the nearest unexplored tile not behind water.");
		cli.println("trade: Suggest possible trading partners.");
		cli.println("quit: Exit the program.");
		cli.println("Any string that is the beginning of only one command is also accepted for that command.");
	}
	void findUnexploredCommand() {
		Point base = cli.inputPoint("Starting point? ");
		if (exists unexplored = findUnexplored(base)) {
			Float distanceTo = distance(base, unexplored, map.dimensions);
			cli.println("Nearest unexplored tile is ``unexplored``, ``Float
				.format(distanceTo, 0, 1, '.', ',')`` tiles away");
		} else {
			cli.println("No unexplored tiles found.");
		}
	}
	void tradeCommand() =>
			suggestTrade(cli.inputPoint("Base location? "),
				cli.inputNumber("Within how many tiles? "));
	Map<String, Anything()> commands = simpleMap(
		"?"->replUsage,
		"help"->replUsage,
		"fortress"->( // TODO: use defer() to avoid lambdas
				() => fortressInfo(cli.inputPoint("Location of fortress? "))),
		"hunt"->(()=>hunt(cli.inputPoint("Location to hunt? "), hunterHours * 60)),
		"fish"->(()=>fish(cli.inputPoint("Location to fish? "), hunterHours * 60)),
		"gather"->(()=>gather(cli.inputPoint("Location to gather? "), hunterHours * 60)),
		"herd"->herd,
		"trap"->shuffle(compose(TrappingCLI.startDriverOnModel,
			TrappingCLI))(cli, options, model),
		"distance"->printDistance,
		"count"->defer(countWorkers, [model.map.players]),
		"unexplored"->findUnexploredCommand,
		"trade"->tradeCommand
	);
	"Handle a series of user commands." // TODO: Move loop back to caller
	shared void handleCommands() {
		while (true) {
			String command = cli.inputString("Command:").lowercased;
			if ("quit".startsWith(command)) {
				break;
			}
			{<String->Anything()>*} matches =
					commands.filterKeys(shuffle(String.startsWith)(command));
			if (matches.size == 1) { // TODO: Test 'first' and 'rest' instead of asserting
				assert (exists first = matches.first);
				first.item();
			} else if (matches.empty) {
				cli.println("Unknown command.");
				replUsage();
			} else {
				cli.println("That command was ambiguous between the following: ");
				assert (exists first = matches.first);
				cli.print(first.key);
				for (key->val in matches.rest) {
					cli.print(", ``key``");
				}
				cli.println("");
				replUsage();
			}
		}
	}
}
"A driver for 'querying' the driver model about various things."
service(`interface ISPDriver`)
// FIXME: Write GUI equivalent of query CLI
shared class QueryCLI() satisfies SimpleCLIDriver {
	shared actual IDriverUsage usage = DriverUsage(false, ["-q", "--query"], ParamCount.one,
		"Answer questions about a map.",
		"Look a tiles on a map. Or run hunting, gathering, or fishing.", true, false);
	"Accept and respond to commands."
	shared actual void startDriverOnModel(ICLIHelper cli, SPOptions options,
			IDriverModel model) {
		QueryHelper helper = QueryHelper(model, cli, HuntingModel(model.map), options);
		try {
			helper.handleCommands();
		} catch (IOException except) {
			log.error("I/O error", except);
		}
	}
	"As we're a CLI driver, we can't show a file-chooser dialog."
	shared actual {JPath*} askUserForFiles() => [];
}
