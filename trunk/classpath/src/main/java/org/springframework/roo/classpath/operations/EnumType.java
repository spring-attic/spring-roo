package org.springframework.roo.classpath.operations;

import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * Provides enum types for JPA use.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public class EnumType implements Comparable<EnumType> {

	private String key;

	public static final EnumType ORDINAL = new EnumType("ORDINAL");
	public static final EnumType STRING = new EnumType("STRING");

	public EnumType(String key) {
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
		return obj != null && obj instanceof EnumType && this.compareTo((EnumType)obj) == 0;
	}

	public final int compareTo(EnumType o) {
		if (o == null) return -1;
		return this.key.compareTo(o.key);
	}

	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("key", key);
		return tsc.toString();
	}

}
