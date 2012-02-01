package org.springframework.roo.project.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.roo.project.GAV;
import org.springframework.roo.project.MavenUtils;
import org.springframework.roo.shell.Completion;

/**
 * Unit test of {@link GAVConverter}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class GAVConverterTest {

    // Fixture
    private GAVConverter converter;

    private void assertInvalidString(final String string,
            final String expectedMessage) {
        try {
            converter.convertFromText(string, GAV.class, null);
            fail("Expected a " + IllegalArgumentException.class);
        }
        catch (final Exception e) {
            assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Asserts the expected completions for the given input string
     * 
     * @param existingData
     * @param expectedComplete whether we expect the converter to report the
     *            conversion as complete
     * @param expectedCompletions
     */
    private void assertPossibleValues(final String existingData,
            final boolean expectedComplete,
            final Completion... expectedCompletions) {
        // Set up
        final List<Completion> completions = new ArrayList<Completion>();

        // Invoke
        final boolean complete = converter.getAllPossibleValues(completions,
                null, existingData, null, null);

        // Check
        assertEquals(expectedComplete, complete);
        assertEquals(Arrays.asList(expectedCompletions), completions);
    }

    @Before
    public void setUp() {
        converter = new GAVConverter();
    }

    @Test
    public void testConvertFromEmptyString() {
        assertInvalidString("",
                "Expected three coordinates, but found 0: []; did you use the ':' separator?");
    }

    @Test
    public void testConvertFromNull() {
        assertInvalidString(null,
                "Expected three coordinates, but found 0: []; did you use the ':' separator?");
    }

    @Test
    public void testConvertFromOneTooFewCoordinates() {
        assertInvalidString(
                "foo:bar",
                "Expected three coordinates, but found 2: [foo, bar]; did you use the ':' separator?");
    }

    @Test
    public void testConvertFromOneTooManyCoordinates() {
        assertInvalidString(
                "foo:bar:baz:bop",
                "Expected three coordinates, but found 4: [foo, bar, baz, bop]; did you use the ':' separator?");
    }

    @Test
    public void testConvertFromValidCoordinates() {
        // Set up
        final String groupId = "org.springframework.roo";
        final String artifactId = "addon-gradle";
        final String version = "-0.1";
        final String coordinates = StringUtils.join(
                Arrays.asList(groupId, artifactId, version),
                MavenUtils.COORDINATE_SEPARATOR);

        // Invoke
        final GAV gav = converter.convertFromText(coordinates, GAV.class, null);

        // Check
        assertEquals(groupId, gav.getGroupId());
        assertEquals(artifactId, gav.getArtifactId());
        assertEquals(version, gav.getVersion());
    }

    @Test
    public void testDoesNotSupportObjects() {
        assertFalse(converter.supports(Object.class, null));
    }

    @Test
    public void testGetAllPossibleValuesForNullInput() {
        assertPossibleValues(null, true);
    }

    @Test
    public void testSupportsGAVs() {
        assertTrue(converter.supports(GAV.class, null));
    }

    @Test
    public void testSupportsSubclassOfGAV() {
        // Set up
        final Class<? extends GAV> subclass = new GAV("a", "b", "c") {
        }.getClass();

        // Invoke and check
        assertTrue(converter.supports(subclass, null));
    }
}