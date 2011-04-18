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

    public int hashCode() {
		return 11 * this.id.hashCode() * this.phase.hashCode() * this.goals.hashCode();
	}

	public boolean equals(Object obj) {
		return obj != null && obj instanceof Execution && this.compareTo((Execution) obj) == 0;
	}

	public int compareTo(Execution o) {
		if (o == null) {
			throw new NullPointerException();
		}
		int result = this.id.compareTo(o.id);
		if (result == 0) {
			result = this.phase.compareTo(o.phase);
		}
		if (result == 0) {
			String[] thisGoals = (String[]) this.goals.toArray();
			String[] oGoals = (String[]) o.goals.toArray();
			Arrays.sort(thisGoals);
			Arrays.sort(oGoals);
			result = Arrays.toString(thisGoals).compareTo(Arrays.toString(oGoals));
		}
		return result;
	}
}
