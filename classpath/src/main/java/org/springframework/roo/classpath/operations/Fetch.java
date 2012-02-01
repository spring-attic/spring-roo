package org.springframework.roo.classpath.operations;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Provides fetch type options for "set" relationships.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public enum Fetch {
    EAGER, LAZY;

    @Override
    public String toString() {
        final ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("name", name());
        return builder.toString();
    }
}
