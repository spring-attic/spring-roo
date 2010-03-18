package org.springframework.roo.classpath.operations;

import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * Provides fetch type options for "set" relationships.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public class Fetch implements Comparable<Fetch> {

	private String key;

	public static final Fetch EAGER = new Fetch("EAGER");
	public static final Fetch LAZY = new Fetch("LAZY");

	public Fetch(String key) {
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
		return obj != null && obj instanceof Fetch && this.compareTo((Fetch)obj) == 0;
	}

	public final int compareTo(Fetch o) {
		if (o == null) return -1;
		return this.key.compareTo(o.key);
	}

	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("key", key);
		return tsc.toString();
	}

}
