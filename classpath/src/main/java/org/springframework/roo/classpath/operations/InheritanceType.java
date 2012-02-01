package org.springframework.roo.classpath.operations;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Provides inheritance type for JPA entities.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public enum InheritanceType {
    JOINED, SINGLE_TABLE, TABLE_PER_CLASS;

    @Override
    public String toString() {
        final ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("name", name());
        return builder.toString();
    }
}
