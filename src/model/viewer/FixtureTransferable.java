package model.viewer;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import model.map.TileFixture;
import org.eclipse.jdt.annotation.Nullable;

/**
 * A class to transfer a TileFixture.
 *
 * This is part of the Strategic Primer assistive programs suite developed by Jonathan
 * Lovelace.
 *
 * Copyright (C) 2011-2014 Jonathan Lovelace
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
public final class FixtureTransferable implements Transferable {
	/**
	 * The object we're transfering.
	 */
	private final TileFixture data;
	/**
	 * a DataFlavor representing its class.
	 */
	public static final DataFlavor FLAVOR = new DataFlavor(TileFixture.class,
			                                                      "TileFixture");

	/**
	 * Constructor.
	 *
	 * @param theData the object
	 */
	public FixtureTransferable(final TileFixture theData) {
		data = theData;
	}

	/**
	 * @return the supported DataFlavors.
	 */
	@Override
	public DataFlavor[] getTransferDataFlavors() {
		return new DataFlavor[]{FLAVOR};
	}

	/**
	 * @param flavor a DataFlavor
	 * @return whether it's the one we support
	 */
	@Override
	public boolean isDataFlavorSupported(@Nullable final DataFlavor flavor) {
		return FLAVOR.equals(flavor);
	}

	/**
	 * This now returns the source component's listened property for text flavors, as
	 * part
	 * of a hack to disallow intra-component drops.
	 *
	 * @param flavor a DataFlavor
	 * @return our underlying data if they want it in the flavor we support
	 * @throws UnsupportedFlavorException if they want an unsupported flavor
	 */
	@Override
	public TileFixture getTransferData(@Nullable final DataFlavor flavor)
			throws UnsupportedFlavorException {
		if (FLAVOR.equals(flavor)) {
			return data; // NOPMD
		} else {
			throw new UnsupportedFlavorException(flavor);
		}
	}

	/**
	 * @return a String representation of this object
	 */
	@Override
	public String toString() {
		return "FixtureTransferable transferring " + data.shortDesc();
	}

	/**
	 * @param obj an object
	 * @return whether it's equal to this one
	 */
	@Override
	public boolean equals(@Nullable final Object obj) {
		return (this == obj) || ((obj instanceof FixtureTransferable)
				                         &&
				                         data.equals(((FixtureTransferable) obj).data));
	}

	/**
	 * @return a hash value for the object
	 */
	@Override
	public int hashCode() {
		return data.hashCode();
	}

}
