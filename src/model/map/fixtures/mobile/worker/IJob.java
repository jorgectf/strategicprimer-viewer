package model.map.fixtures.mobile.worker;

import java.util.Arrays;
import java.util.Collections;
import model.map.HasMutableName;
import model.map.Subsettable;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import util.NullCleaner;

/**
 * An interface for Jobs.
 *
 * This is part of the Strategic Primer assistive programs suite developed by Jonathan
 * Lovelace.
 *
 * Copyright (C) 2014-2015 Jonathan Lovelace
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
public interface IJob
		extends HasMutableName, Iterable<@NonNull ISkill>, Subsettable<@NonNull IJob> {
	/**
	 * Skill names that are suspicious when they are the only Skill a Job has. In many
	 * cases they should be 'miscellaneous' instead.
	 */
	Iterable<String> SUSPICIOUS_SKILLS = NullCleaner.assertNotNull(
			Collections.unmodifiableList(
					Arrays.asList("hunter", "hunting", "explorer", "exploration",
							"research", "carpentry", "woodcutting", "farming",
							"food gathering", "scientist", "woodcutter", "farmer",
							"brickmaker", "brickmaking")));
	/**
	 * Add a skill.
	 *
	 * Note that this does not guarantee that the Job will contain this Skill object, nor
	 * that any changes made to it will be applied to the Skill that the Job already had
	 * or that is actually added. (TODO: implementations *should* do that.) If levels or
	 * hours *need* to be added, callers should get the Skill the Job contains after this
	 * returns using {@link #getSkill(String)} and apply changes to that.
	 *
	 * @param skill the skill to add
	 * @return the result of the operation
	 */
	boolean addSkill(ISkill skill);

	/**
	 * @return the worker's level in the job
	 */
	int getLevel();

	/**
	 * @param zero whether to "zero out" sensitive details
	 * @return a copy of this Job
	 */
	IJob copy(boolean zero);

	/**
	 * A Job is "empty" if the worker has no levels in it and no experience in any skills
	 * it contains.
	 *
	 * @return whether this Job is "empty"
	 */
	boolean isEmpty();

	/**
	 * TODO: Should we add and return a new Skill in the not-present case?
	 *
	 * @param skillName the name of a Skill
	 * @return the Skill by that name in the Job, or none if not present
	 */
	@Nullable ISkill getSkill(String skillName);
}
