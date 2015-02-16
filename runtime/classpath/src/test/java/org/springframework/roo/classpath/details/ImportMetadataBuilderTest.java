package org.springframework.roo.classpath.details;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.springframework.roo.model.JavaType;

/**
 * Unit test of {@link ImportMetadataBuilder}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class ImportMetadataBuilderTest {

    private static final String CALLER_MID = "MID:foo#bar";

    @Test
    public void testGetImport() {
        // Set up
        final JavaType mockTypeToImport = mock(JavaType.class);

        // Invoke
        final ImportMetadata importMetadata = ImportMetadataBuilder.getImport(
                CALLER_MID, mockTypeToImport);

        // Check
        assertEquals(mockTypeToImport, importMetadata.getImportType());
        assertEquals(0, importMetadata.getModifier());
        assertFalse(importMetadata.isAsterisk());
        assertFalse(importMetadata.isStatic());
    }
}
