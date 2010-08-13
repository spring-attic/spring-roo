package org.springframework.roo.classpath.operations;

import org.springframework.roo.support.style.ToStringCreator;

/**
 * Provides fetch type options for "set" relationships.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public enum Fetch {
	EAGER, LAZY;

	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("name", name());
		return tsc.toString();
	}
}
