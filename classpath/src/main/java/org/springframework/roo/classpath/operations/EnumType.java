package org.springframework.roo.classpath.operations;

import org.springframework.roo.support.style.ToStringCreator;

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
        final ToStringCreator tsc = new ToStringCreator(this);
        tsc.append("name", name());
        return tsc.toString();
    }
}
