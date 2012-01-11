package org.springframework.roo.classpath.layers;

/**
 * Priority enum.
 * 
 * @author Stefan Schmidt
 * @since 1.2.0
 */
public enum Priority {
    HIGH(100), LOW(0), MEDIUM(50);

    private int priority;

    private Priority(final int priority) {
        this.priority = priority;
    }

    public int getNumericValue() {
        return priority;
    }
}
