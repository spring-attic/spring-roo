package org.springframework.roo.project;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

/**
 * Unit test of {@link GAV}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class GAVTest {

    private static final String GROUP_ID = "org.apache.maven";
    private static final String VERSION = "5.6";
    private static final String ARTIFACT_ID = "maven-surefire-plugin";
    private static final GAV GAV_1A = new GAV(GROUP_ID, ARTIFACT_ID, VERSION);
    private static final GAV GAV_1B = new GAV(GROUP_ID, ARTIFACT_ID, VERSION);
    private static final GAV GAV_2 = new GAV(GROUP_ID, ARTIFACT_ID, VERSION
            + ".1");

    @Test
    public void testConstructorAndGetters() {
        // Set up

        // Invoke
        final GAV gav = new GAV(GROUP_ID, ARTIFACT_ID, VERSION);

        // Check
        assertEquals(GROUP_ID, gav.getGroupId());
        assertEquals(ARTIFACT_ID, gav.getArtifactId());
        assertEquals(VERSION, gav.getVersion());
    }

    @Test
    public void testGetInstance() {
        // Set up
        final String coordinates = StringUtils.join(new String[] { GROUP_ID,
                ARTIFACT_ID, VERSION }, MavenUtils.COORDINATE_SEPARATOR);

        // Invoke
        final GAV gav = GAV.getInstance(coordinates);

        // Check
        assertEquals(GROUP_ID, gav.getGroupId());
        assertEquals(ARTIFACT_ID, gav.getArtifactId());
        assertEquals(VERSION, gav.getVersion());
    }

    @Test
    public void testInstancesWithDifferentVersionsAreNotEqual() {
        assertFalse(GAV_1A.equals(GAV_2));
        assertFalse(GAV_2.equals(GAV_1A));
    }

    @Test
    public void testInstancesWithDifferentVersionsCompareUnequally() {
        assertFalse(GAV_1A.compareTo(GAV_2) == 0);
    }

    @Test
    public void testInstancesWithSameCoordinatesAreEqual() {
        assertTrue(GAV_1A.equals(GAV_1B));
        assertTrue(GAV_1B.equals(GAV_1A));
    }

    @Test
    public void testInstancesWithSameCoordinatesCompareEqually() {
        assertEquals(0, GAV_1A.compareTo(GAV_1B));
    }

    @Test
    public void testInstancesWithSameCoordinatesHaveSameHashCode() {
        assertEquals(GAV_1A.hashCode(), GAV_1B.hashCode());
    }
}
