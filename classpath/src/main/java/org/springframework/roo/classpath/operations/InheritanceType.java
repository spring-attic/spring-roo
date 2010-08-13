package org.springframework.roo.classpath.operations;

import org.springframework.roo.support.style.ToStringCreator;

/**
 * Provides inheritance type for JPA entities.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public enum InheritanceType {
	SINGLE_TABLE, TABLE_PER_CLASS, JOINED;

	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("name", name());
		return tsc.toString();
	}
}
