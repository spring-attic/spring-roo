package org.springframework.roo.classpath.details;

import static org.junit.Assert.assertEquals;

import org.springframework.roo.classpath.itd.ItdSourceFileComposer;

/**
 * Superclass for testing {@link ItdTypeDetails} instances and subclasses
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public abstract class ItdTypeDetailsTestCase {

    /**
     * Asserts that the given ITD produces the given output
     * 
     * @param expectedOutput the ITD's expected output
     * @param itd the ITD to check (required)
     */
    protected void assertOutput(final String expectedOutput,
            final ItdTypeDetails itd) {
        // Set up
        final ItdSourceFileComposer composer = new ItdSourceFileComposer(itd);

        // Invoke
        final String actualOutput = composer.getOutput();

        // Check
        assertEquals(expectedOutput, actualOutput);
    }
}
