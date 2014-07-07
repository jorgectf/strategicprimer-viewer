package model.listeners;

import java.util.EventListener;

import model.map.fixtures.UnitMember;

import org.eclipse.jdt.annotation.Nullable;

/**
 * An interface for objects that want to know when a new unit member (usually a
 * worker) is selected.
 *
 * @author Jonathan Lovelace
 *
 */
public interface UnitMemberListener extends EventListener {
	/**
	 * @param old the previous selection
	 * @param selected the new selection. Because sometimes there's no
	 *        selection, may be null.
	 */
	void memberSelected(@Nullable  UnitMember old,
			@Nullable  UnitMember selected);
}
