package org.springframework.roo.project;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.roo.support.util.Assert;

/**
 * Immutable representation of an execution specification for a (maven) build plugin
 * 
 * @author Adrian Colyer
 * @author Alan Stewart
 * @since 1.0
 */
public final class Execution {
	private final String id;
	private final String phase;
	private final List<String> goals;

	public Execution(String id, String phase, String... goals) {
		Assert.notNull(id, "execution id must be specified");
		Assert.notNull(phase, "execution phase must be specified");
		Assert.notEmpty(goals, "at least one goal must be specified");
		this.id = id;
		this.phase = phase;
		this.goals = Collections.unmodifiableList(Arrays.asList(goals));
	}

	public String getPhase() {
		return this.phase;
	}

	public String getId() {
		return this.id;
	}

	public List<String> getGoals() {
		return this.goals;
	}
}
