package impl.dbio;

import common.map.IFixture;
import io.jenetics.facilejdbc.Param;
import io.jenetics.facilejdbc.Query;
import io.jenetics.facilejdbc.Transactional;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import common.map.Point;
import common.map.IMutableMapNG;

import common.map.fixtures.towns.Village;
import common.map.fixtures.towns.TownStatus;
import common.map.fixtures.towns.CommunityStats;
import common.xmlio.Warning;
import common.map.fixtures.towns.ITownFixture;

import static io.jenetics.facilejdbc.Param.value;

final class DBVillageHandler extends AbstractDatabaseWriter<Village, Point> implements MapContentsReader {
	public DBVillageHandler() {
		super(Village.class, Point.class);
	}

	private static final List<Query> INITIALIZERS = Collections.singletonList(
		Query.of("CREATE TABLE IF NOT EXISTS villages (" +
			"    row INTEGER NOT NULL," +
			"    column INTEGER NOT NULL," +
			"    status VARCHAR(9) NOT NULL" +
			"        CHECK(status IN ('abandoned', 'active', 'burned', 'ruined'))," +
			"    name VARCHAR(128) NOT NULL," +
			"    id INTEGER NOT NULL," +
			"    owner INTEGER NOT NULL," +
			"    race VARCHAR(32) NOT NULL," +
			"    image VARCHAR(255)," +
			"    portrait VARCHAR(255)," +
			"    population INTEGER" +
			");"));

	@Override
	public List<Query> getInitializers() {
		return INITIALIZERS;
	}

	private static final Query INSERT_SQL =
		Query.of("INSERT INTO villages (row, column, status, name, id, owner, race, " +
			"    image, portrait, population) " +
			"VALUES(:row, :column, :status, :name, :id, :owner, :race, :image, :portrait, :population);");

	private static final AbstractDatabaseWriter<CommunityStats, ITownFixture> CS_WRITER =
		new DBCommunityStatsHandler();

	@Override
	public void write(final Transactional db, final Village obj, final Point context) throws SQLException {
		final List<Param> params = new ArrayList<>();
		params.add(value("row", context.row()));
		params.add(value("column", context.column()));
		params.add(value("status", obj.getStatus().toString()));
		params.add(value("name", obj.getName()));
		params.add(value("id", obj.getId()));
		params.add(value("owner", obj.owner().getPlayerId()));
		params.add(value("race", obj.getRace()));
		params.add(value("image", obj.getImage()));
		params.add(value("portrait", obj.getPortrait()));
		final CommunityStats stats = obj.getPopulation();
		if (stats != null) {
			params.add(value("population", stats.getPopulation()));
		}
		INSERT_SQL.on(params).execute(db.connection());
		if (stats != null) {
			CS_WRITER.initialize(db);
			CS_WRITER.write(db, stats, obj);
		}
	}

	private static TryBiConsumer<Map<String, Object>, Warning, SQLException> readVillage(final IMutableMapNG map) {
		return (dbRow, warner) -> {
			final int row = (Integer) dbRow.get("row");
			final int column = (Integer) dbRow.get("column");
			final TownStatus status = TownStatus.parse((String) dbRow.get("status"));
			final String name = (String) dbRow.get("name");
			final int id = (Integer) dbRow.get("id");
			final int ownerId = (Integer) dbRow.get("owner");
			final String race = (String) dbRow.get("race");
			final String image = (String) dbRow.get("image");
			final String portrait = (String) dbRow.get("portrait");
			final Integer population = (Integer) dbRow.get("population");
			final Village village = new Village(status, name, id, map.getPlayers().getPlayer(ownerId),
				race);
			if (image != null) {
				village.setImage(image);
			}
			if (portrait != null) {
				village.setPortrait(portrait);
			}
			if (population != null) {
				village.setPopulation(new CommunityStats(population));
			}
			map.addFixture(new Point(row, column), village);
		};
	}

	private static final Query SELECT = Query.of("SELECT * FROM villages");
	@Override
	public void readMapContents(final Connection db, final IMutableMapNG map, final Map<Integer, IFixture> containers,
			final Map<Integer, List<Object>> containees, final Warning warner) throws SQLException {
		handleQueryResults(db, warner, "villages", readVillage(map), SELECT);
	}
}
