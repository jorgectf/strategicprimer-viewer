import ceylon.dbc {
	Sql
}

import strategicprimer.model.map {
	IMutableMapNG,
	IFixture,
	IMapNG
}
import strategicprimer.model.xmlio {
	Warning
}
"An interface for code to read map contents from an SQL database."
interface MapContentsReader {
	"Read map direct contents---that is, anything directly at a location on the map."
	shared formal void readMapContents(Sql db, IMutableMapNG map, Warning warner);
	"Read non-direct contents---that is, unit and fortress members and the like. Because
	 in many cases this doesn't apply, it's by default a noop."
	shared default void readExtraMapContents(Sql db, IMutableMapNG map, Warning warner) {}
	"Find a tile fixture or unit or fortress member within a given stream of such objects
	 by its ID, if present."
	// TODO: Allow callers to narrow by type?
	shared default IFixture? findByIdImpl({IFixture*} stream, Integer id) {
		for (fixture in stream) {
			if (fixture.id == id) {
				return fixture;
			} else if (is {IFixture*} fixture, exists retval = findByIdImpl(fixture, id)) {
				return retval;
			}
		}
		return null;
	}
	"Find a tile fixture or unit or fortress member by ID."
	shared default IFixture findById(IMapNG map, Integer id, Warning warner) {
		if (exists retval = findByIdImpl(map.fixtureEntries.map(Entry.item), id)) {
			return retval;
		} else {
			throw AssertionError("ID ``id`` not found");
		}
	}
}