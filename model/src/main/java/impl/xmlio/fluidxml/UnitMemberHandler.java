package impl.xmlio.fluidxml;

import org.javatuples.Pair;

import common.map.HasImage;
import common.map.HasKind;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;

import common.map.fixtures.mobile.AnimalOrTracks;
import common.idreg.IDRegistrar;
import common.map.IPlayerCollection;
import common.map.fixtures.mobile.IWorker;
import common.map.fixtures.mobile.Worker;
import common.map.fixtures.mobile.SimpleImmortal;
import common.map.fixtures.mobile.Animal;
import common.map.fixtures.mobile.MaturityModel;
import common.map.fixtures.mobile.AnimalImpl;
import common.map.fixtures.mobile.AnimalTracks;
import common.map.fixtures.mobile.ImmortalAnimal;
import common.map.fixtures.mobile.worker.IJob;
import common.map.fixtures.mobile.worker.IMutableJob;
import common.map.fixtures.mobile.worker.Job;
import common.map.fixtures.mobile.worker.WorkerStats;
import common.map.fixtures.mobile.worker.ISkill;
import common.map.fixtures.mobile.worker.Skill;
import common.xmlio.Warning;
import impl.xmlio.exceptions.UnwantedChildException;
import impl.xmlio.exceptions.UnsupportedPropertyException;
import impl.xmlio.exceptions.UnsupportedTagException;
import common.xmlio.SPFormatException;
import lovelace.util.MalformedXMLException;
import java.util.List;
import java.util.stream.StreamSupport;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import common.map.fixtures.mobile.Immortal;
import javax.xml.stream.XMLStreamException;

/* package */ class UnitMemberHandler extends FluidBase {
	public static Worker readWorker(StartElement element, QName parent, Iterable<XMLEvent> stream,
			IPlayerCollection players, Warning warner, IDRegistrar idFactory)
			throws SPFormatException {
		requireTag(element, parent, "worker");
		expectAttributes(element, warner, "name", "race", "portrait", "id", "image");
		Worker retval = setImage(
			new Worker(getAttribute(element, "name"),
				getAttribute(element, "race", "human"),
				getOrGenerateID(element, warner, idFactory)),
			element, warner);
		retval.setPortrait(getAttribute(element, "portrait", ""));
		for (XMLEvent event : stream) {
			if (event instanceof StartElement && isSPStartElement((StartElement) event)) {
				switch (((StartElement) event).getName().getLocalPart().toLowerCase()) {
				case "job":
					retval.addJob(readJob((StartElement) event, element.getName(),
						stream, players, warner, idFactory));
					break;
				case "stats":
					retval.setStats(readStats((StartElement) event, element.getName(),
						stream, players, warner, idFactory));
					break;
				case "note":
					retval.setNote(
						players.getPlayer(getIntegerAttribute((StartElement) event,
							"player")),
						readNote((StartElement) event, element.getName(), stream,
							warner));
					break;
				default:
					throw UnwantedChildException.listingExpectedTags(element.getName(),
						(StartElement) event, "job", "stats", "note");
				}
			} else if (event instanceof EndElement &&
					element.getName().equals(((EndElement) event).getName())) {
				break;
			}
		}
		return retval;
	}

	private static String readNote(StartElement element, QName parent, Iterable<XMLEvent> stream,
			Warning warner) throws SPFormatException {
		requireTag(element, parent, "note");
		expectAttributes(element, warner, "player");
		final StringBuilder retval = new StringBuilder();
		for (XMLEvent event : stream) {
			if (event instanceof StartElement && isSPStartElement((StartElement) event)) {
				throw new UnwantedChildException(element.getName(), (StartElement) event);
			} else if (event instanceof EndElement &&
					element.getName().equals(((EndElement) event).getName())) {
				break;
			} else if (event instanceof Characters) {
				retval.append(((Characters) event).getData());
			}
		}
		return retval.toString().trim();
	}

	public static IJob readJob(StartElement element, QName parent, Iterable<XMLEvent> stream,
			IPlayerCollection players, Warning warner, IDRegistrar idFactory) 
			throws SPFormatException {
		requireTag(element, parent, "job");
		expectAttributes(element, warner, "name", "level");
		IMutableJob retval = new Job(getAttribute(element, "name"),
			getIntegerAttribute(element, "level"));
		for (XMLEvent event : stream) {
			if (event instanceof StartElement && isSPStartElement((StartElement) event)) {
				if ("skill".equalsIgnoreCase(
						((StartElement) event).getName().getLocalPart())) {
					retval.addSkill(readSkill((StartElement) event, element.getName(),
						stream, players, warner, idFactory));
				} else {
					throw UnwantedChildException.listingExpectedTags(element.getName(), 
						(StartElement) event, "skill");
				}
			} else if (event instanceof  EndElement &&
					element.getName().equals(((EndElement) event).getName())) {
				break;
			}
		}
		return retval;
	}

	public static ISkill readSkill(StartElement element, QName parent, Iterable<XMLEvent> stream,
			IPlayerCollection players, Warning warner, IDRegistrar idFactory) 
			throws SPFormatException {
		requireTag(element, parent, "skill");
		expectAttributes(element, warner, "name", "level", "hours");
		requireNonEmptyAttribute(element, "name", true, warner);
		spinUntilEnd(element.getName(), stream);
		return new Skill(getAttribute(element, "name"),
			getIntegerAttribute(element, "level"),
			getIntegerAttribute(element, "hours"));
	}

	public static WorkerStats readStats(StartElement element, QName parent, Iterable<XMLEvent> stream,
			IPlayerCollection players, Warning warner, IDRegistrar idFactory) 
			throws SPFormatException {
		requireTag(element, parent, "stats");
		expectAttributes(element, warner, "hp", "max", "str", "dex", "con", "int",
			"wis", "cha");
		spinUntilEnd(element.getName(), stream);
		return new WorkerStats(getIntegerAttribute(element, "hp"),
			getIntegerAttribute(element, "max"),
			getIntegerAttribute(element, "str"),
			getIntegerAttribute(element, "dex"),
			getIntegerAttribute(element, "con"),
			getIntegerAttribute(element, "int"),
			getIntegerAttribute(element, "wis"),
			getIntegerAttribute(element, "cha"));
	}

	public static void writeWorker(XMLStreamWriter ostream, IWorker obj, int indentation)
			throws MalformedXMLException {
		WorkerStats stats = obj.getStats();
		List<IJob> jobs = StreamSupport.stream(obj.spliterator(), true)
			.filter(((Predicate<IJob>) IJob::isEmpty).negate()).collect(Collectors.toList());
		boolean hasJobs = !jobs.isEmpty();
		writeTag(ostream, "worker", indentation, !hasJobs && stats == null);
		writeAttributes(ostream, Pair.with("name", obj.getName()));
		if (!"human".equals(obj.getRace())) {
			writeAttributes(ostream, Pair.with("race", obj.getRace()));
		}
		writeAttributes(ostream, Pair.with("id", obj.getId()));
		writeImage(ostream, obj);
		writeNonEmptyAttributes(ostream, Pair.with("portrait", obj.getPortrait()));
		if (stats != null) {
			writeStats(ostream, stats, indentation + 1);
		}
		for (IJob job : jobs) {
			writeJob(ostream, job, indentation + 1);
		}
		for (Integer player : obj.getNotesPlayers()) {
			writeNote(ostream, player, obj.getNote(player), indentation +1);
		}
		if (hasJobs || stats != null || obj.getNotesPlayers().iterator().hasNext()) {
			indent(ostream, indentation);
			try {
				ostream.writeEndElement();
			} catch (XMLStreamException except) {
				throw new MalformedXMLException(except);
			}
		}
	}

	private static void writeNote(XMLStreamWriter ostream, int player, String note, int indentation)
			throws MalformedXMLException {
		writeTag(ostream, "note", indentation, false);
		writeAttributes(ostream, Pair.with("player", player));
		try {
			ostream.writeCharacters(note);
			ostream.writeEndElement();
		} catch (XMLStreamException except) {
			throw new MalformedXMLException(except);
		}
	}

	public static void writeStats(XMLStreamWriter ostream, WorkerStats obj, int indentation) 
			throws MalformedXMLException {
		writeTag(ostream, "stats", indentation, true);
		writeAttributes(ostream, Pair.with("hp", obj.getHitPoints()),
			Pair.with("max", obj.getMaxHitPoints()),
			Pair.with("str", obj.getStrength()), Pair.with("dex", obj.getDexterity()),
			Pair.with("con", obj.getConstitution()), Pair.with("int", obj.getIntelligence()),
			Pair.with("wis", obj.getWisdom()), Pair.with("cha", obj.getCharisma()));
	}

	public static void writeJob(XMLStreamWriter ostream, IJob obj, int indentation)
			throws MalformedXMLException {
		boolean hasSkills = !obj.isEmpty();
		if (obj.getLevel() <= 0 && !hasSkills) {
			return;
		}
		writeTag(ostream, "job", indentation, !hasSkills);
		writeAttributes(ostream, Pair.with("name", obj.getName()),
			Pair.with("level", obj.getLevel()));
		for (ISkill skill : obj) {
			writeSkill(ostream, skill, indentation + 1);
		}
		if (hasSkills) {
			indent(ostream, indentation);
			try {
				ostream.writeEndElement();
			} catch (XMLStreamException except) {
				throw new MalformedXMLException(except);
			}
		}
	}

	public static void writeSkill(XMLStreamWriter ostream, ISkill obj, int indentation) 
			throws MalformedXMLException {
		if (!obj.isEmpty()) {
			writeTag(ostream, "skill", indentation, true);
			writeAttributes(ostream, Pair.with("name", obj.getName()),
				Pair.with("level", obj.getLevel()), Pair.with("hours", obj.getHours()));
		}
	}

	// TODO: split into Animal and Tracks methods, if at all possible
	public static AnimalOrTracks readAnimal(StartElement element, QName parent,
			Iterable<XMLEvent> stream, IPlayerCollection players, Warning warner,
			IDRegistrar idFactory) throws SPFormatException {
		requireTag(element, parent, "animal");
		String tag = element.getName().getLocalPart().toLowerCase();
		String kind;
		if ("animal".equals(tag)) {
			expectAttributes(element, warner, "traces", "id", "count", "kind", "talking",
				"status", "wild", "born", "image");
			kind = getAttribute(element, "kind");
		} else {
			warner.handle(UnsupportedTagException.future(element));
			expectAttributes(element, warner, "id", "count", "image");
			kind = tag;
		}
		spinUntilEnd(element.getName(), stream);
		// To get the intended meaning of existing maps, we have to parse
		// traces="" as traces="true". If compatibility with existing maps
		// ever becomes unnecessary, I will change the default-value here to
		// simply `false`.
		boolean traces = getBooleanAttribute(element, "traces",
			hasAttribute(element, "traces") && getAttribute(element, "traces", "").isEmpty(),
			warner);
		boolean talking = getBooleanAttribute(element, "talking", false, warner);
		String status = getAttribute(element, "status", "wild");
		int born = getIntegerAttribute(element, "born", -1, warner);
		// TODO: We'd like the default to be 1 inside a unit and -1 outside
		int count = getIntegerAttribute(element, "count", 1, warner);
		int id;
		if (traces) {
			if (hasAttribute(element, "id")) {
				warner.handle(UnsupportedPropertyException.inContext(element, "id",
					"when tracks=\"true\""));
			}
			if (talking) {
				warner.handle(UnsupportedPropertyException.inContext(element, "talking",
					"when tracks=\"true\""));
			}
			if (!"wild".equals(status)) {
				warner.handle(UnsupportedPropertyException.inContext(element, "status",
					"when tracks=\"true\""));
			}
			if (born != -1) {
				warner.handle(UnsupportedPropertyException.inContext(element, "born",
					"when tracks=\"true\""));
			}
			if (count != 1) {
				warner.handle(UnsupportedPropertyException.inContext(element, "count",
					"when tracks=\"true\""));
			}
			return setImage(new AnimalTracks(kind), element, warner);
		} else {
			id = getOrGenerateID(element, warner, idFactory);
			return setImage(
				new AnimalImpl(kind, talking, status, id, born, count), element, warner);
		}
	}

	public static void writeAnimalTracks(XMLStreamWriter ostream, AnimalTracks obj,
			int indentation) throws MalformedXMLException {
		writeTag(ostream, "animal", indentation, true);
		writeAttributes(ostream, Pair.with("kind", obj.getKind()), Pair.with("traces", true));
		writeImage(ostream, obj);
	}

	public static void writeAnimal(XMLStreamWriter ostream, Animal obj, int indentation) 
			throws MalformedXMLException {
		writeTag(ostream, "animal", indentation, true);
		writeAttributes(ostream, Pair.with("kind", obj.getKind()));
		if (obj.isTalking()) {
			writeAttributes(ostream, Pair.with("talking", true));
		}
		if (!"wild".equals(obj.getStatus())) {
			writeAttributes(ostream, Pair.with("status", obj.getStatus()));
		}
		writeAttributes(ostream, Pair.with("id", obj.getId()));
		if (obj.getBorn() >= 0) {
			// Write turn-of-birth if and only if it is fewer turns before the current
			// turn than this kind of animal's age of maturity.
			if (MaturityModel.getCurrentTurn() >= 0 &&
					MaturityModel.getMaturityAges().containsKey(obj.getKind()) &&
					MaturityModel.getMaturityAges().get(obj.getKind()) <=
						(MaturityModel.getCurrentTurn() - obj.getBorn())) {
				// do nothing
			} else {
				writeAttributes(ostream, Pair.with("born", obj.getBorn()));
			}
		}
		if (obj.getPopulation() > 1) {
			writeAttributes(ostream, Pair.with("count", obj.getPopulation()));
		}
		writeImage(ostream, obj);
	}

	public static void writeSimpleImmortal(XMLStreamWriter ostream, Immortal obj, int indentation)
			throws MalformedXMLException {
		// TODO: split this method so we can get this back in the type system
		if (!(obj instanceof SimpleImmortal || obj instanceof ImmortalAnimal)) {
			throw new IllegalArgumentException("Only works with simple immortals");
		}
		writeTag(ostream, ((HasKind) obj).getKind(), indentation, true);
		writeAttributes(ostream, Pair.with("id", obj.getId()));
		writeImage(ostream, (HasImage) obj);
	}
}
