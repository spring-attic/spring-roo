package org.springframework.roo.classpath;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;

/**
 * Unit test of {@link PhysicalTypeIdentifierNamingUtils}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class PhysicalTypeIdentifierNamingUtilsTest {

    // instance ID
    private static final String METADATA_CLASS_ID = "MID:org.springframework.roo.addon.plural.PluralMetadata";
    private static final String METADATA_INSTANCE_ID = "MID:org.springframework.roo.addon.plural.PluralMetadata#core|SRC_MAIN_JAVA?com.example.domain.Thing";
    private static final String MODULE = "core"; // Same as in the above
                                                 // instance ID
    private static final Path PATH = Path.SRC_MAIN_JAVA; // Same as in the below

    @Test
    public void testGetLogicalPathFromMetadataInstanceId() {
        // Invoke
        final LogicalPath logicalPath = PhysicalTypeIdentifierNamingUtils
                .getPath(METADATA_INSTANCE_ID);

        // Check
        assertEquals(LogicalPath.getInstance(PATH, MODULE), logicalPath);
    }

    @Test
    public void testGetModuleNameFromMetadataInstanceId() {
        // Invoke
        final String module = PhysicalTypeIdentifierNamingUtils
                .getModule(METADATA_INSTANCE_ID);

        // Check
        assertEquals(MODULE, module);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetPathFromMetadataClassId() {
        PhysicalTypeIdentifierNamingUtils.getPath(METADATA_CLASS_ID);
    }
}
