package org.springframework.roo.addon.propfiles;

import static org.junit.Assert.assertEquals;

import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.springframework.roo.addon.propfiles.caller.PropertiesTestClient;

/**
 * Unit test of {@link PropFileOperationsImpl} N.B. for this test to pass, the
 * following folder must be on the classpath:
 * <code>org.springframework.roo.addon.propfiles/src/test/resources</code> This
 * is automatically the case when run by Maven, but not in Eclipse/STS, where
 * the first time you run this test, you need to add the above folder explicitly
 * via the "Run As -> Run Configurations..." dialog (in the "Classpath" tab,
 * click "Advanced" and add the above path as a "folder").
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class PropFileOperationsImplTest {

    // Fixture
    private PropFileOperationsImpl propFileOperations;

    @Before
    public void setUp() {
        propFileOperations = new PropFileOperationsImpl();
    }

    @Test
    public void testLoadPropertiesFromClasspathWhenItExists() {
        // Invoke
        final Properties properties = propFileOperations.loadProperties(
                "bike.properties", PropertiesTestClient.class);

        // Check
        assertEquals("Fondriest", properties.getProperty("frame"));
        assertEquals("Shimano Ultegra", properties.getProperty("groupset"));
        assertEquals("Rolf Vector", properties.getProperty("wheels"));
    }
}
