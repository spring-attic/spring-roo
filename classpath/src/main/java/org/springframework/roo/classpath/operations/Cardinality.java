package org.springframework.roo.classpath.operations;

import org.springframework.roo.support.style.ToStringCreator;

/**
 * Provides cardinality options for "set" relationships.
 * 
 * @author Ben Alex
 * @author Alan Stewart
 * @since 1.0
 */
public enum Cardinality {
    MANY_TO_MANY, MANY_TO_ONE, ONE_TO_MANY, ONE_TO_ONE;

    @Override
    public String toString() {
        final ToStringCreator tsc = new ToStringCreator(this);
        tsc.append("name", name());
        return tsc.toString();
    }
}
