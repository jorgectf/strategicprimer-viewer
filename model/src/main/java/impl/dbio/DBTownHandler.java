package impl.dbio;

import buckelieg.jdbc.fn.DB;

import common.map.IFixture;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Map;

import common.map.IMutableMapNG;
import common.map.Player;
import common.map.Point;

import common.map.fixtures.towns.AbstractTown;
import common.map.fixtures.towns.ITownFixture;
import common.map.fixtures.towns.TownStatus;
import common.map.fixtures.towns.TownSize;
import common.map.fixtures.towns.Fortification;
import common.map.fixtures.towns.City;
import common.map.fixtures.towns.Town;
import common.map.fixtures.towns.CommunityStats;
import common.xmlio.Warning;

final class DBTownHandler extends AbstractDatabaseWriter<AbstractTown, Point> implements MapContentsReader {
	public DBTownHandler() {
		super(AbstractTown.class, Point.class);
	}

	private static final List<String> INITIALIZERS = Collections.singletonList(
		"CREATE TABLE IF NOT EXISTS towns (" +
			"    row INTEGER NOT NULL," +
			"    column INTEGER NOT NULL," +
			"    id INTEGER NOT NULL," +
			"    kind VARCHAR(13) NOT NULL" +
			"        CHECK(kind IN ('town', 'city', 'fortification'))," +
			"    status VARCHAR(9) NOT NULL" +
			"        CHECK(status IN ('abandoned', 'active', 'burned', 'ruined'))," +
			"    size VARCHAR(6) NOT NULL" +
			"        CHECK(size IN ('small', 'medium', 'large'))," +
			"    dc INTEGER," +
			"    name VARCHAR(128) NOT NULL," +
			"    owner INTEGER NOT NULL," +
			"    image VARCHAR(255)," +
			"    portrait VARCHAR(255)," +
			"    population INTEGER" +
			");");

	@Override
	public List<String> getInitializers() {
		return INITIALIZERS;
	}

	private static final String INSERT_SQL =
		"INSERT INTO towns (row, column, id, kind, status, size, dc, name, " +
			"    owner, image, portrait, population) " +
			"VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";

	private static final AbstractDatabaseWriter<CommunityStats, ITownFixture> CS_WRITER =
		new DBCommunityStatsHandler();

	@Override
	public void write(final DB db, final AbstractTown obj, final Point context) {
		db.update(INSERT_SQL, context.getRow(), context.getColumn(), obj.getId(), obj.getKind(),
			obj.getStatus().toString(), obj.getTownSize().toString(), obj.getDC(), obj.getName(),
			obj.getOwner().getPlayerId(), obj.getImage(), obj.getPortrait(),
			Optional.ofNullable(obj.getPopulation()).map(CommunityStats::getPopulation)
				.orElse(null)).execute();
		final CommunityStats stats = obj.getPopulation();
		if (stats != null) {
			CS_WRITER.initialize(db);
			CS_WRITER.write(db, stats, obj);
		}
	}

	private TryBiConsumer<Map<String, Object>, Warning, Exception> readTown(final IMutableMapNG map,
			final Map<Integer, List<Object>> containees) {
		return (dbRow, warner) -> {
			final int row = (Integer) dbRow.get("row");
			final int column = (Integer) dbRow.get("column");
			final int id = (Integer) dbRow.get("id");
			final String kind = (String) dbRow.get("kind");
			final TownStatus status = TownStatus.parse((String) dbRow.get("status"));
			final TownSize size = TownSize.parseTownSize((String) dbRow.get("size"));
			final int dc = (Integer) dbRow.get("dc");
			final String name = (String) dbRow.get("name");
			final int ownerNum = (Integer) dbRow.get("owner");
			final String image = (String) dbRow.get("image");
			final String portrait = (String) dbRow.get("portrait");
			final Integer population = (Integer) dbRow.get("population");
			final AbstractTown town;
			final Player owner = map.getPlayers().getPlayer(ownerNum);
			switch (kind) {
			case "fortification":
				town = new Fortification(status, size, dc, name, id, owner);
				break;
			case "city":
				town = new City(status, size, dc, name, id, owner);
				break;
			case "town":
				town = new Town(status, size, dc, name, id, owner);
				break;
			default:
				throw new IllegalArgumentException("Unhandled kind of town");
			}
			if (image != null) {
				town.setImage(image);
			}
			if (portrait != null) {
				town.setPortrait(portrait);
			}
			if (population != null) {
				// Don't add it directly because it's also read in the
				// CommunityStats handler, which needs to get it out of the
				// containees to avoid conflicts.
				multimapPut(containees, id, new CommunityStats(population));
			}
			map.addFixture(new Point(row, column), town);
		};
	}

	@Override
	public void readMapContents(final DB db, final IMutableMapNG map, final Map<Integer, IFixture> containers,
			final Map<Integer, List<Object>> containees, final Warning warner) {
		try {
			handleQueryResults(db, warner, "towns", readTown(map, containees),
				"SELECT * FROM towns");
		} catch (final RuntimeException except) {
			// Don't wrap RuntimeExceptions in RuntimeException
			throw except;
		} catch (final Exception except) {
			// FIXME Antipattern
			throw new RuntimeException(except);
		}
	}
}
