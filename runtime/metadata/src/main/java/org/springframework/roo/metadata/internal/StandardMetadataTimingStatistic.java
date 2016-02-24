package org.springframework.roo.metadata.internal;

import org.apache.commons.lang3.Validate;
import org.springframework.roo.metadata.MetadataTimingStatistic;

/**
 * Standard implementation of {@link MetadataTimingStatistic}.
 * 
 * @author Ben Alex
 */
public class StandardMetadataTimingStatistic implements MetadataTimingStatistic {

    // Chosen to match the maximum
    private static final int MAXIMUM_EXPECTED_INVOCATIONS = 5;

    private static final String INVOCATION_COUNT_FORMAT = "%"
            + MAXIMUM_EXPECTED_INVOCATIONS + "d";

    static final long NANOSECONDS_IN_MILLISECOND = 1000000L;
    private static final String TIME_FORMAT = "%"
            + (String.valueOf(NANOSECONDS_IN_MILLISECOND).length() - 1) + "d";

    private final long invocations;
    private final String name;
    private final long nanoseconds;

    /**
     * Constructor
     * 
     * @param name (required)
     * @param nanoseconds the elasped time in nanoseconds (zero or more)
     * @param invocations (zero or more)
     */
    public StandardMetadataTimingStatistic(final String name,
            final long nanoseconds, final long invocations) {
        Validate.notBlank(name, "Name required");
        Validate.isTrue(invocations >= 0, "Invocations must be zero or more");
        Validate.isTrue(nanoseconds >= 0, "Nanoseconds must be zero or more");
        this.invocations = invocations;
        this.name = name;
        this.nanoseconds = nanoseconds;
    }

    public int compareTo(final MetadataTimingStatistic o) {
        int result = Long.valueOf(nanoseconds).compareTo(o.getTime());
        if (result == 0) {
            result = Long.valueOf(invocations).compareTo(o.getInvocations());
        }
        if (result == 0) {
            result = name.compareTo(o.getName());
        }
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof MetadataTimingStatistic
                && compareTo((MetadataTimingStatistic) obj) == 0;
    }

    public long getInvocations() {
        return invocations;
    }

    public String getName() {
        return name;
    }

    public long getTime() {
        return nanoseconds;
    }

    @Override
    public int hashCode() {
        return Long.valueOf(nanoseconds).hashCode()
                * Long.valueOf(invocations).hashCode() * name.hashCode();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        if (nanoseconds < NANOSECONDS_IN_MILLISECOND) {
            // Display as nanoseconds
            sb.append(String.format(TIME_FORMAT, nanoseconds)).append(" ns; ");
        }
        else {
            // Display as milliseconds
            sb.append(String.format(TIME_FORMAT, nanoseconds / 1000000))
                    .append(" ms; ");
        }
        sb.append(String.format(INVOCATION_COUNT_FORMAT, invocations)).append(
                " call(s): ");
        sb.append(name);
        return sb.toString();
    }
}
