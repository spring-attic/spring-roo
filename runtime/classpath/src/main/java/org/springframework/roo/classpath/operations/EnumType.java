package org.springframework.roo.classpath.operations;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Provides enum types for JPA use.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public enum EnumType {
    ORDINAL, STRING;

    @Override
    public String toString() {
        final ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("name", name());
        return builder.toString();
    }
}
