package org.springframework.roo.classpath.operations;

import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * Provides cardinality options for "set" relationships.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public class Cardinality implements Comparable<Cardinality> {

	private String key;

	public static final Cardinality ONE_TO_MANY = new Cardinality("ONE_TO_MANY");
	public static final Cardinality MANY_TO_MANY = new Cardinality("MANY_TO_MANY");

	public Cardinality(String key) {
		Assert.hasText(key, "Key required");
		this.key = key;
	}

	public String getKey() {
		return key;
	}

	public final int hashCode() {
		return this.key.hashCode();
	}

	public final boolean equals(Object obj) {
		return obj != null && obj instanceof Cardinality && this.compareTo((Cardinality)obj) == 0;
	}

	public final int compareTo(Cardinality o) {
		if (o == null) return -1;
		return this.key.compareTo(o.key);
	}

	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("key", key);
		return tsc.toString();
	}

}
