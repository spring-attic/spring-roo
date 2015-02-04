package org.springframework.roo.metadata.internal;

import static org.junit.Assert.assertEquals;
import static org.springframework.roo.metadata.internal.StandardMetadataTimingStatistic.NANOSECONDS_IN_MILLISECOND;

import org.junit.Test;

/**
 * Unit test of {@link StandardMetadataTimingStatistic}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class StandardMetadataTimingStatisticTest {

    private static final long INVOCATIONS = 5;

    private static final String NAME = "MyProcess";

    /**
     * Asserts that calling {@link StandardMetadataTimingStatistic#toString()}
     * on an instance with the given duration results in the expected output
     * 
     * @param nanoseconds
     * @param expectedToString
     */
    private void assertToString(final long nanoseconds,
            final String expectedToString) {
        assertEquals(expectedToString, getTestInstance(nanoseconds).toString());
    }

    /**
     * Creates an instance with fixed test values and the given duration
     * 
     * @param nanoseconds
     * @return a non-<code>null</code> instance
     */
    private StandardMetadataTimingStatistic getTestInstance(
            final long nanoseconds) {
        return new StandardMetadataTimingStatistic(NAME, nanoseconds,
                INVOCATIONS);
    }

    @Test
    public void testToStringForLessThanOneMillisecond() {
        assertToString(NANOSECONDS_IN_MILLISECOND - 1,
                "999999 ns;     5 call(s): MyProcess");
    }

    @Test
    public void testToStringForOneMillisecond() {
        assertToString(NANOSECONDS_IN_MILLISECOND,
                "     1 ms;     5 call(s): MyProcess");
    }

    @Test
    public void testToStringForTwoMilliseconds() {
        assertToString(NANOSECONDS_IN_MILLISECOND * 2,
                "     2 ms;     5 call(s): MyProcess");
    }
}
