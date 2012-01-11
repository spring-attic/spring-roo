package org.springframework.roo.project.maven;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit test of the {@link Module} class.
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class ModuleTest {

    private static final String VALID_NAME = "web";
    private static final String VALID_PATH = "/path/to/pom.xml";

    @Test(expected = IllegalArgumentException.class)
    public void testConstructWithEmptyName() {
        new Module("", VALID_PATH);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructWithEmptyPath() {
        new Module(VALID_NAME, "");
    }

    @Test
    public void testConstructWithValidArguments() {
        // Invoke
        final Module module = new Module(VALID_NAME, VALID_PATH);

        // Check
        assertEquals(VALID_NAME, module.getName());
        assertEquals(VALID_PATH, module.getPomPath());
    }
}
