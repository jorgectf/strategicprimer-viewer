package model.map.fixtures.towns;

import controller.map.formatexceptions.SPFormatException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import javax.xml.stream.XMLStreamException;
import model.map.BaseTestFixtureSerialization;
import model.map.Player;
import model.workermgmt.RaceFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * A class to test serialization of Villages.
 *
 * This is part of the Strategic Primer assistive programs suite developed by Jonathan
 * Lovelace.
 *
 * Copyright (C) 2012-2016 Jonathan Lovelace
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of version 3 of the GNU General Public License as published by the Free Software
 * Foundation; see COPYING or
 * <a href="http://www.gnu.org/licenses/">http://www.gnu.org/licenses/</a>.
 *
 * @author Jonathan Lovelace
 */
@SuppressWarnings("ClassHasNoToStringMethod")
@RunWith(Parameterized.class)
public class TestVillageSerialization extends BaseTestFixtureSerialization {
	/**
	 * Extracted constant.
	 */
	private static final String NAME_PROPERTY = "name";
	/**
	 * Extracted constant.
	 */
	private static final String STATUS_PROPERTY = "status";
	/**
	 * Extracted constant.
	 */
	private static final String OWNER_PROPERTY = "owner";
	/**
	 * The status to use for the village in the test.
	 */
	private final TownStatus status;
	/**
	 * The race to use for the village in the test.
	 */
	private final String race;
	/**
	 * Constructor for parametrized testing.
	 * @param villageStatus the status to use for the village in the test
	 * @param villageRace   the race to use for the village in the test
	 */
	public TestVillageSerialization(final TownStatus villageStatus,
									final String villageRace) {
		status = villageStatus;
		race = villageRace;
	}

	/**
	 * A factory method for the data to use as parameters for the test.
	 *
	 * @return the data to use as parameters for the test.
	 */
	@SuppressWarnings("ObjectAllocationInLoop")
	@Parameterized.Parameters
	public static Collection<Object[]> generateData() {
		final TownStatus[] statuses = TownStatus.values();
		final Collection<String> races = new HashSet<>(RaceFactory.getRaces());
		final Collection<Object[]> retval =
				new ArrayList<>(statuses.length * races.size());
		for (final TownStatus status : statuses) {
			for (final String race : races) {
				retval.add(new Object[]{status, race});
			}
		}
		return retval;
	}

	/**
	 * Test Village serialization.
	 *
	 * @throws SPFormatException  on XML format error
	 * @throws XMLStreamException on XML reader error
	 * @throws IOException        on I/O error creating serialized form
	 */
	@Test
	public void testVillageSerialization()
			throws XMLStreamException, SPFormatException, IOException {
		assert status != null;
		assert race != null;
		final Player owner = new Player(-1, "");
		assertSerialization("First Village serialization test, " + status,
				new Village(status, "villageOne", 1, owner, race));
		assertSerialization("2nd Village serialization test,  " + status,
				new Village(status, "villageTwo", 2, owner, race));
		final Village thirdVillage = new Village(status, "", 3, owner, race);
		assertMissingPropertyDeserialization(
				"Village serialization with no or empty name does The Right Thing",
				thirdVillage, createSerializedForm(thirdVillage, true), NAME_PROPERTY);
		assertMissingPropertyDeserialization(
				"Village serialization with no or empty name does The Right Thing",
				thirdVillage, createSerializedForm(thirdVillage, false), NAME_PROPERTY);
		assertUnwantedChild("<village status=\"" + status + "\"><village /></village>",
				Village.class, false);
		assertMissingProperty("<village />", Village.class, STATUS_PROPERTY,
				false);
		assertMissingProperty("<village name=\"name\" status=\"" + status + "\" />",
				Village.class, "id", true);
		assertMissingProperty(
				"<village name=\"name\" status=\"" + status + "\" id=\"0\" />",
				Village.class, OWNER_PROPERTY, true);
		assertImageSerialization("Village image property is preserved", thirdVillage);
		assertPortraitSerialization("Village portrait property is preserved",
				thirdVillage);
	}
}
