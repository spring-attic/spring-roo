package org.springframework.roo.classpath.operations;

import org.apache.commons.lang3.builder.ToStringBuilder;

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
        final ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("name", name());
        return builder.toString();
    }
}
