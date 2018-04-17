import ceylon.dbc {
	Sql,
	SqlNull
}

import java.sql {
	Types
}

import strategicprimer.model.map {
	Point,
	IMutableMapNG,
	pointFactory
}
import strategicprimer.model.map.fixtures {
	TextFixture
}
import strategicprimer.model.xmlio {
	Warning
}

import lovelace.util.common {
	as
}

object dbTextHandler extends AbstractDatabaseWriter<TextFixture, Point>() satisfies MapContentsReader {
	shared actual {String+} initializers = [
		"""CREATE TABLE IF NOT EXISTS text_notes (
			   row INTEGER NOT NULL,
			   column INTEGER NOT NULL,
			   turn INTEGER,
			   text VARCHAR(1024) NOT NULL,
			   image VARCHAR(255)
		   );"""
	];
	shared actual void write(Sql db, TextFixture obj, Point context) {
		Integer|SqlNull turn;
		if (obj.turn >= 0) {
			turn = obj.turn;
		} else {
			turn = SqlNull(Types.integer);
		}
		db.Insert("""INSERT INTO text_notes (row, column, turn, text, image) VALUES(?, ?, ?, ?, ?);""")
				.execute(context.row, context.column, turn, obj.text, obj.image);
	}
	shared actual void readMapContents(Sql db, IMutableMapNG map, Warning warner) {
		log.trace("About to read text notes");
		variable Integer count = 0;
		for (dbRow in db.Select("""SELECT * FROM text_notes""").Results()) {
			assert (is Integer row = dbRow["row"], is Integer column = dbRow["column"],
				is Integer|SqlNull turn = dbRow["turn"], is String text = dbRow["text"],
				is String|SqlNull image = dbRow["image"]);
			value fixture = TextFixture(text, as<Integer>(turn) else -1);
			if (is String image) {
				fixture.image = image;
			}
			map.addFixture(pointFactory(row, column), fixture);
			count++;
			if ((count % 50) == 0) {
				log.trace("Read ``count`` text notes");
			}
		}
		log.trace("Finished reading text notes");
	}
}