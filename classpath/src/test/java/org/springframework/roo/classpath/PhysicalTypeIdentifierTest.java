package org.springframework.roo.classpath;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;

/**
 * Unit test of {@link PhysicalTypeIdentifier}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class PhysicalTypeIdentifierTest {

    private static final String USER_PROJECT_TYPE = "com.foo.Bar";

    @Test
    public void testGetJavaType() {
        // Set up
        final String metadataId = "MID:"
                + PhysicalTypeIdentifier.class.getName() + "#"
                + Path.SRC_MAIN_JAVA + "?" + USER_PROJECT_TYPE;

        // Invoke
        final JavaType javaType = PhysicalTypeIdentifier
                .getJavaType(metadataId);

        // Check
        assertEquals(USER_PROJECT_TYPE, javaType.getFullyQualifiedTypeName());
    }
}
