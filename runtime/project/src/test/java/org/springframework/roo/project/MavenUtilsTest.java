package org.springframework.roo.project;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit test of {@link MavenUtils}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class MavenUtilsTest {

    @Test
    public void testEmptyStringIsNotAValidId() {
        assertFalse(MavenUtils.isValidMavenId(""));
    }

    @Test
    public void testNullIsNotAValidId() {
        assertFalse(MavenUtils.isValidMavenId(null));
    }

    @Test
    public void testValidArtifactIdIsAValidId() {
        assertTrue(MavenUtils.isValidMavenId("spring-core"));
    }

    @Test
    public void testValidGroupIdIsAValidId() {
        assertTrue(MavenUtils.isValidMavenId("org.springframework"));
    }
}