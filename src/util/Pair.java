// $codepro.audit.disable lineLength
package util;

import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;

/**
 * From
 * <http://stackoverflow.com/questions/156275/what-is-the-equivalent-of-the-
 * c-pairl-r-in-java/3646398#3646398>.
 *
 * @author Peter Lawrey
 * @author adapted by Jonathan Lovelace to pass muster with static analysis,
 *         etc.
 *
 * @param <L> The first type in the pair.
 * @param <R> The second type in the pair.
 */
public class Pair<L, R> {
	/**
	 * The first item in the pair.
	 */
	private final L first;

	/**
	 * The second item in the pair.
	 */
	private final R second;

	/**
	 *
	 * @return the first item in the pair
	 */
	public L first() {
		return first;
	}

	/**
	 *
	 * @return the second item in the pair
	 */
	public R second() {
		return second;
	}

	// ESCA-JAVA0029:
	/**
	 * Constructor.
	 *
	 * @param firstItem The first item in the pair.
	 * @param secondItem The second item in the pair.
	 */
	protected Pair(final L firstItem, final R secondItem) {
		first = firstItem;
		second = secondItem;
	}

	/**
	 * Create a pair without having to specify the types.
	 *
	 * @param <FIRST> The type of the first element in the pair
	 * @param <SECOND> The type of the second element in the pair
	 * @param first The first element in the pair.
	 * @param second The second element in the pair.
	 * @return a pair containing the two elements
	 */
	public static <FIRST, SECOND> Pair<FIRST, SECOND> of(final FIRST first, // NOPMD
			final SECOND second) {
		return new Pair<>(first, second);
	}

	/**
	 *
	 * @return a hash code for the pair.
	 */
	@Override
	public int hashCode() {
		return 31 * first.hashCode() + second.hashCode();
	}

	/**
	 * @param obj an object
	 *
	 * @return whether it's the same as this one
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public boolean equals(@Nullable final Object obj) {
		return this == obj || obj instanceof Pair
				&& Objects.equals(first, ((Pair) obj).first)
				&& Objects.equals(second, ((Pair) obj).second);
	}

	/**
	 * @return a String representation of the pair.
	 */
	@Override
	public String toString() {
		return '(' + first.toString() + ", " + second + ')';
	}
}
