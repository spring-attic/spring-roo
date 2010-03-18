package org.springframework.roo.classpath.operations;

import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * Provides inheritance type for JPA entities.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public class InheritanceType implements Comparable<InheritanceType> {

	private String key;

	public static final InheritanceType SINGLE_TABLE = new InheritanceType("SINGLE_TABLE");
	public static final InheritanceType TABLE_PER_CLASS = new InheritanceType("TABLE_PER_CLASS");
	public static final InheritanceType JOINED = new InheritanceType("JOINED");

	public InheritanceType(String key) {
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
		return obj != null && obj instanceof InheritanceType && this.compareTo((InheritanceType)obj) == 0;
	}

	public final int compareTo(InheritanceType o) {
		if (o == null) return -1;
		return this.key.compareTo(o.key);
	}

	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("key", key);
		return tsc.toString();
	}

}
