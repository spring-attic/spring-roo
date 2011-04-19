package org.springframework.roo.project;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.roo.support.util.Assert;

/**
 * Immutable representation of an execution specification for a (Maven) build plugin
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

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((goals == null) ? 0 : goals.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((phase == null) ? 0 : phase.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		return obj != null && obj instanceof Execution && this.compareTo((Execution) obj) == 0;
	}

	public int compareTo(Execution o) {
		if (o == null) {
			throw new NullPointerException();
		}
		int result = id.compareTo(o.id);
		if (result == 0) {
			result = phase.compareTo(o.phase);
		}
		if (result == 0) {
			String[] thisGoals = (String[]) goals.toArray();
			String[] oGoals = (String[]) o.goals.toArray();
			Arrays.sort(thisGoals);
			Arrays.sort(oGoals);
			result = Arrays.toString(thisGoals).compareTo(Arrays.toString(oGoals));
		}
		return result;
	}
}
