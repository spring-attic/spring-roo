package org.springframework.roo.classpath.layers;

/**
 * Priority enum.
 * 
 * @author Stefan Schmidt
 * @since 1.2.0
 */
public enum Priority {
    LOW(0), MEDIUM(50), HIGH(100);

    private int priority;

    private Priority(final int priority) {
        this.priority = priority;
    }

    public int getNumericValue() {
        return priority;
    }
}
