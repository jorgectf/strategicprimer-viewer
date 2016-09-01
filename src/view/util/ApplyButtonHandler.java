package view.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.eclipse.jdt.annotation.Nullable;

/**
 * A class to handle Apply and Revert button presses.
 *
 * This is part of the Strategic Primer assistive programs suite developed by Jonathan
 * Lovelace.
 *
 * Copyright (C) 2010-2016 Jonathan Lovelace
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of version 3 of the GNU General Public License as published by the Free Software
 * Foundation; see COPYING or
 * <a href="http://www.gnu.org/licenses/">http://www.gnu.org/licenses/</a>.
 *
 * @author Jonathan Lovelace
 */
public final class ApplyButtonHandler implements ActionListener {
	/**
	 * The Applyable that does the heavy lifting.
	 */
	private final Applyable objOne;
	/**
	 * The Revertible that does the other half of the heavy lifting.
	 */
	private final Revertible objTwo;

	/**
	 * Constructor.
	 *
	 * @param app the Applyable that does the heavy lifting.
	 * @param rev the Revertible that does the other heavy lifting
	 */
	public ApplyButtonHandler(final Applyable app, final Revertible rev) {
		objOne = app;
		objTwo = rev;
	}

	/**
	 * Handle button presses.
	 *
	 * @param evt the event to handle
	 */
	@SuppressWarnings("ParameterNameDiffersFromOverriddenParameter")
	@Override
	public void actionPerformed(@Nullable final ActionEvent evt) {
		if (evt != null) {
			if ("Apply".equals(evt.getActionCommand())) {
				objOne.apply();
			} else if ("Revert".equals(evt.getActionCommand())) {
				objTwo.revert();
			}
		}
	}

	/**
	 * @return a String representation of the object.
	 */
	@SuppressWarnings("MethodReturnAlwaysConstant")
	@Override
	public String toString() {
		return "ApplyButtonHandler";
	}
}
