// $codepro.audit.disable lineLength
package controller.map.readerng;

import controller.map.formatexceptions.SPFormatException;
import controller.map.formatexceptions.UnwantedChildException;
import controller.map.iointerfaces.ISPReader;
import controller.map.misc.IDFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import model.map.IMutablePlayerCollection;
import org.eclipse.jdt.annotation.NonNull;
import util.EqualsAny;
import util.NullCleaner;
import util.Warning;

/**
 * An alternative approach, to hopefully replace the ReaderFactory---instead of asking for
 * a reader based on what type you expect, you tell the Adapter to parse the node and it
 * does The Right Thing. In theory. This is absolutely necessary to implement the
 * "include" tag.
 *
 * This is part of the Strategic Primer assistive programs suite developed by Jonathan
 * Lovelace.
 *
 * Copyright (C) 2012-2015 Jonathan Lovelace
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of version 3 of the GNU General Public License as published by the Free Software
 * Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this
 * program. If not, see
 * <a href="http://www.gnu.org/licenses/">http://www.gnu.org/licenses/</a>.
 *
 * @author Jonathan Lovelace
 * @deprecated ReaderNG is deprecated
 */
@Deprecated
public final class ReaderAdapter implements INodeHandler<Object> {
	/**
	 * A singleton. Provided for performance; this is, after all, stateless.
	 */
	public static final ReaderAdapter ADAPTER = new ReaderAdapter();

	/**
	 * Parse an element.
	 *
	 * @param element   the element to parse
	 * @param stream    the stream to get more elements from
	 * @param players   the collection of players to use if needed
	 * @param warner    the Warning instance to use
	 * @param idFactory the factory to use to register ID numbers and generate new
	 *                     ones as
	 *                  needed
	 * @return the result of parsing the element
	 * @throws SPFormatException on SP format problems.
	 */
	@Override
	public Object parse(final StartElement element,
	                    final Iterable<XMLEvent> stream,
	                    final IMutablePlayerCollection players,
	                    final Warning warner, final IDFactory idFactory)
			throws SPFormatException {
		final String iLocal = NullCleaner.assertNotNull(element.getName().getLocalPart
				                                                                  ());
		if (EqualsAny.equalsAny(
				NullCleaner.assertNotNull(element.getName().getNamespaceURI()),
				ISPReader.NAMESPACE, XMLConstants.NULL_NS_URI)
				&& READ_CACHE.containsKey(iLocal)) {
			return NullCleaner.assertNotNull(READ_CACHE.get(iLocal))
						   .parse(element, stream, players, warner, idFactory);
		} else {
			throw new UnwantedChildException(new QName("unknown"), element);
		}
	}

	/**
	 * Map from tags to readers. Initializer moved to static block below because here it
	 * made the line *way* too long.
	 */
	private static final Map<String, INodeHandler<@NonNull ?>> READ_CACHE =
			new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	/**
	 * Map from writable objects to writers. Initializer in static block below.
	 */
	private static final Map<Class<?>, INodeHandler<@NonNull ?>> WRITERS =
			new HashMap<>();

	/**
	 * Add a reader to the cache.
	 *
	 * @param reader the reader to add
	 */
	static void factory(final INodeHandler<@NonNull ?> reader) { // NOPMD
		for (final String tag : reader.understands()) {
			READ_CACHE.put(tag, reader);
		}
		WRITERS.put(reader.writtenClass(), reader);
	}

	/**
	 * Add four readers to the cache. CodePro objects to initializers over 40 lines, but
	 * we can't do a var-args method because the parameterization would give us
	 * unsolvable
	 * compiler warnings.
	 *
	 * @param firstReader  the first reader
	 * @param secondReader the second reader
	 * @param thirdReader  the third reader
	 * @param fourthReader the fourth reader
	 */
	private static void factoryFour(final INodeHandler<@NonNull ?> firstReader,
	                                final INodeHandler<@NonNull ?> secondReader,
	                                final INodeHandler<@NonNull ?> thirdReader,
	                                final INodeHandler<@NonNull ?> fourthReader) {
		factory(firstReader);
		factory(secondReader);
		factory(thirdReader);
		factory(fourthReader);
	}

	static {
		factory(new PlayerReader());
		factory(new AnimalReader());
		factoryFour(new CacheReader(), new CentaurReader(), new DjinnReader(),
				new DragonReader());
		factoryFour(new FairyReader(), new ForestReader(),
				new FortressReader(), new GiantReader());
		factoryFour(new GriffinReader(), new GroundReader(), new GroveReader(),
				new HillReader());
		factoryFour(new MeadowReader(), new MineReader(), new MinotaurReader(),
				new MountainReader());
		factoryFour(new OasisReader(), new OgreReader(), new PhoenixReader(),
				new SandbarReader());
		factoryFour(new ShrubReader(), new SimurghReader(), new SphinxReader(),
				new TrollReader());
		factoryFour(new TextReader(), new UnitReader(), new VillageReader(),
				new BattlefieldReader());
		factoryFour(new CaveReader(), new CityReader(),
				new FortificationReader(), new TownReader());
		factory(new MineralReader());
		factory(new StoneReader());
		factory(new RiverReader());
		factoryFour(new WorkerReader(), new SkillReader(), new StatsReader(),
				new AdventureReader());
		factoryFour(new PortalReader(), new MapNGReader(),
				new ImplementReader(), new ResourceReader());
	}

	/**
	 * @return nothing, as this method always throws IllegalStateException---if it gets
	 * called, we're adding it to its own cache.
	 */
	@Override
	public List<String> understands() {
		throw new IllegalStateException(
				                               "ReaderAdapter#understands() should never" +
						                               " be called");
	}

	/**
	 * @param <T>  a type
	 * @param obj  an object
	 * @param type a type
	 * @return obj, if it is assignable to the specified type; throw an exception
	 * otherwise.
	 */
	@SuppressWarnings("unchecked")
	// The point here is that we *are* checking ... but you can't do that
	// statically in a generic method.
	public static <T> T checkedCast(final Object obj, final Class<T> type) {
		if (type.isAssignableFrom(obj.getClass())) {
			return (T) obj;
		} else {
			throw new IllegalStateException("Wanted " + type.getSimpleName()
					                                + ", was " +
					                                obj.getClass().getSimpleName());
		}
	}

	/**
	 * Create an intermediate representation to convert to XML.
	 *
	 * @param <S> the actual type of the object
	 * @param obj the object to write
	 * @return an intermediate representation
	 */
	@Override
	public <@NonNull S> SPIntermediateRepresentation write(final S obj) {
		if (WRITERS.containsKey(obj.getClass())) {
			//noinspection unchecked
			return ((INodeHandler<S>) NullCleaner.assertNotNull(WRITERS.get(obj.getClass())))
					       .write(obj);
		} else {
			for (final Map.Entry<Class<?>, INodeHandler<@NonNull ?>> entry : WRITERS
					                                                                 .entrySet()) {
				if (entry.getKey().isAssignableFrom(obj.getClass())) {
					//noinspection unchecked
					return ((INodeHandler<S>) entry.getValue()).write(obj);
				}
			}
			throw new IllegalArgumentException(
					                                  "Writable type this adapter can't handle: "
							                                  + obj.getClass()
									                                    .getSimpleName());
		}
	}

	/**
	 * @return nothing---this should never be called, so we object with an exception.
	 */
	@Override
	public Class<Object> writtenClass() {
		throw new IllegalStateException("This should never be called.");
	}

	/**
	 * @return a String representation of the object
	 */
	@SuppressWarnings("MethodReturnAlwaysConstant")
	@Override
	public String toString() {
		return "ReaderAdapter";
	}
}
