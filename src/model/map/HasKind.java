package model.map;

/**
 * An interface for fixtures that have a 'kind' property.
 *
 * FIXME: Should we split this, and other similar interfaces, into "HasX" and
 * "MutableHasX"?
 *
 * This is part of the Strategic Primer assistive programs suite developed by Jonathan
 * Lovelace.
 *
 * Copyright (C) 2012-2014 Jonathan Lovelace
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
 */
public interface HasKind {
	/**
	 * @return the kind of whatever this is
	 */
	String getKind();

	/**
	 * @param nKind the thing's new kind
	 */
	void setKind(String nKind);
}
