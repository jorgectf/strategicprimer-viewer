package impl.xmlio.fluidxml;

import java.util.Collection;
import javax.xml.stream.XMLStreamException;
import org.javatuples.Pair;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.text.ParseException;

import common.idreg.IDRegistrar;
import common.map.IPlayerCollection;
import common.map.River;
import common.map.fixtures.Ground;
import common.map.fixtures.terrain.Forest;
import common.xmlio.Warning;
import impl.xmlio.exceptions.MissingPropertyException;
import common.xmlio.SPFormatException;
import common.map.HasExtent;

/* package */ class FluidTerrainHandler extends FluidBase {
	public static Ground readGround(final StartElement element, final QName parent, final Iterable<XMLEvent> stream,
	                                final IPlayerCollection players, final Warning warner, final IDRegistrar idFactory)
			throws SPFormatException {
		requireTag(element, parent, "ground");
		expectAttributes(element, warner, "id", "kind", "ground", "image", "exposed");
		final int id = getIntegerAttribute(element, "id", -1, warner);
		if (id >= 0) {
			idFactory.register(id, warner, element.getLocation());
		}
		final String kind = getAttrWithDeprecatedForm(element, "kind", "ground", warner);
		spinUntilEnd(element.getName(), stream);
		return setImage(new Ground(id, kind, getBooleanAttribute(element, "exposed")),
			element, warner);
	}

	public static Forest readForest(final StartElement element, final QName parent, final Iterable<XMLEvent> stream,
	                                final IPlayerCollection players, final Warning warner, final IDRegistrar idFactory)
			throws SPFormatException {
		requireTag(element, parent, "forest");
		expectAttributes(element, warner, "id", "kind", "rows", "image", "acres");
		final int id = getIntegerAttribute(element, "id", -1, warner);
		if (id >= 0) {
			idFactory.register(id, warner, element.getLocation());
		}
		final Forest retval = new Forest(getAttribute(element, "kind"),
			getBooleanAttribute(element, "rows", false), id,
			getNumericAttribute(element, "acres", -1));
		spinUntilEnd(element.getName(), stream);
		return setImage(retval, element, warner);
	}

	public static void writeGround(final XMLStreamWriter ostream, final Ground obj, final int indent)
			throws XMLStreamException {
		writeTag(ostream, "ground", indent, true);
		writeAttributes(ostream, Pair.with("kind", obj.getKind()),
			Pair.with("exposed", obj.isExposed()), Pair.with("id", obj.getId()));
		writeImage(ostream, obj);
	}

	public static void writeForest(final XMLStreamWriter ostream, final Forest obj, final int indent)
			throws XMLStreamException {
		writeTag(ostream, "forest", indent, true);
		writeAttributes(ostream, Pair.with("kind", obj.getKind()));
		if (obj.isRows()) {
			writeAttributes(ostream, Pair.with("rows", true));
		}
		if (HasExtent.isPositive(obj.getAcres())) {
			writeAttributes(ostream, Pair.with("acres", obj.getAcres()));
		}
		writeAttributes(ostream, Pair.with("id", obj.getId()));
		writeImage(ostream, obj);
	}

	public static River readLake(final StartElement element, final QName parent, final Iterable<XMLEvent> stream,
	                             final IPlayerCollection players, final Warning warner, final IDRegistrar idFactory)
			throws SPFormatException {
		requireTag(element, parent, "lake");
		expectAttributes(element, warner);
		spinUntilEnd(element.getName(), stream);
		return River.Lake;
	}

	public static River readRiver(final StartElement element, final QName parent, final Iterable<XMLEvent> stream,
	                              final IPlayerCollection players, final Warning warner, final IDRegistrar idFactory)
			throws SPFormatException {
		requireTag(element, parent, "river");
		expectAttributes(element, warner, "direction");
		spinUntilEnd(element.getName(), stream);
		try {
			return River.parse(getAttribute(element, "direction"));
		} catch (final ParseException|IllegalArgumentException except) {
			throw new MissingPropertyException(element, "direction", except);
		}
	}

	public static void writeRiver(final XMLStreamWriter ostream, final River obj, final int indent)
			throws XMLStreamException {
		if (River.Lake == obj) {
			writeTag(ostream, "lake", indent, true);
		} else {
			writeTag(ostream, "river", indent, true);
			writeAttributes(ostream, Pair.with("direction", obj.getDescription()));
		}
	}

	public static void writeRivers(final XMLStreamWriter ostream, final Collection<River> obj, final int indent)
			throws XMLStreamException {
		// Can't use forEach() instead of collecting to a new list because of declared exception
		for (final River river : obj.stream().sorted().toList()) {
			writeRiver(ostream, river, indent);
		}
	}
}
