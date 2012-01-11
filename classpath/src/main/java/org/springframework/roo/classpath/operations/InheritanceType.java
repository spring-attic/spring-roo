package org.springframework.roo.classpath.operations;

import org.springframework.roo.support.style.ToStringCreator;

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
        final ToStringCreator tsc = new ToStringCreator(this);
        tsc.append("name", name());
        return tsc.toString();
    }
}
