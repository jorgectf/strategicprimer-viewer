package model.map;

/**
 * An interface for model elements that have images that can be used to represent them.
 * This interface should really be in model.viewer, but that would, I think, introduce
 * circular dependencies between packages.
 *
 * This is part of the Strategic Primer assistive programs suite developed by Jonathan
 * Lovelace.
 *
 * Copyright (C) 2011-2016 Jonathan Lovelace
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of version 3 of the GNU General Public License as published by the Free Software
 * Foundation; see COPYING or
 * <a href="http://www.gnu.org/licenses/">http://www.gnu.org/licenses/</a>.
 *
 * @author Jonathan Lovelace
 */
public interface HasImage {
	/**
	 * This is the image to use if the individual fixture doesn't specify a different
	 * image. It should be a "constant function."
	 *
	 * TODO: replace this with a centralized registry
	 *
	 * @return the name of an image to represent this kind of fixture.
	 */
	String getDefaultImage();

	/**
	 * The per-instance icon filename.
	 * @return the name of an image to represent this individual fixture.
	 */
	String getImage();
}
