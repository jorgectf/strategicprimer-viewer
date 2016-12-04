package util;

import java.util.Objects;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * From <http://stackoverflow.com/a/3646398>.
 *
 * @param <L> The first type in the pair.
 * @param <R> The second type in the pair.
 * @author Peter Lawrey
 * @author adapted by Jonathan Lovelace to pass muster with static analysis, etc.
 */
public class Pair<@NonNull L, @NonNull R> {
	/**
	 * The first item in the pair.
	 */
	private final L first;

	/**
	 * The second item in the pair.
	 */
	private final R second;

	/**
	 * Constructor.
	 *
	 * @param firstItem  The first item in the pair.
	 * @param secondItem The second item in the pair.
	 */
	protected Pair(final L firstItem, final R secondItem) {
		first = firstItem;
		second = secondItem;
	}

	/**
	 * Create a pair without having to specify the types.
	 *
	 * @param <T>        The type of the first element in the pair
	 * @param <U>        The type of the second element in the pair
	 * @param firstItem  The first element in the pair.
	 * @param secondItem The second element in the pair.
	 * @return a pair containing the two elements
	 */
	public static <@NonNull T, @NonNull U> Pair<T, U> of(final T firstItem,
														 final U secondItem) {
		return new Pair<>(firstItem, secondItem);
	}

	/**
	 * @return the first item in the pair
	 */
	public L first() {
		return first;
	}

	/**
	 * @return the second item in the pair
	 */
	public R second() {
		return second;
	}

	/**
	 * @return a hash code for the pair.
	 */
	@Override
	public int hashCode() {
		return (31 * first.hashCode()) + second.hashCode();
	}

	/**
	 * @param obj an object
	 * @return whether it's the same as this one
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public boolean equals(@Nullable final Object obj) {
		return (this == obj) || ((obj instanceof Pair)
										 && Objects.equals(first, ((Pair) obj).first)
										 && Objects.equals(second, ((Pair) obj).second));
	}

	/**
	 * @return a String representation of the pair.
	 */
	@Override
	public String toString() {
		return '(' + first.toString() + ", " + second + ')';
	}
}
