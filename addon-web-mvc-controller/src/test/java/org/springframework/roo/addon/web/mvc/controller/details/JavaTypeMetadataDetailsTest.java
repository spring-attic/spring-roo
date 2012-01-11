package org.springframework.roo.addon.web.mvc.controller.details;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.springframework.roo.model.JavaType;

/**
 * Unit test of {@link JavaTypeMetadataDetails}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class JavaTypeMetadataDetailsTest {

    /**
     * Creates a test instance with the given {@link JavaType}
     * 
     * @param javaType
     * @return a non-<code>null</code> instance
     */
    private JavaTypeMetadataDetails getTestInstance(final JavaType javaType) {
        return new JavaTypeMetadataDetails(javaType, "the-plural", false,
                false, null, "the-controller-path");
    }

    @Test
    public void testInstancesWithDifferentJavaTypesAreNotEqual() {
        // Set up
        final JavaType mockJavaType1 = mock(JavaType.class);
        final JavaType mockJavaType2 = mock(JavaType.class);
        final JavaTypeMetadataDetails javaTypeMetadataDetails = getTestInstance(mockJavaType1);
        final JavaTypeMetadataDetails otherJavaTypeMetadataDetails = mock(JavaTypeMetadataDetails.class);
        when(otherJavaTypeMetadataDetails.getJavaType()).thenReturn(
                mockJavaType2);

        // Invoke and check
        assertFalse(javaTypeMetadataDetails
                .equals(otherJavaTypeMetadataDetails));
    }

    @Test
    public void testInstancesWithSameJavaTypesAreEqual() {
        // Set up
        final JavaType mockJavaType = mock(JavaType.class);
        final JavaTypeMetadataDetails javaTypeMetadataDetails = getTestInstance(mockJavaType);
        final JavaTypeMetadataDetails otherJavaTypeMetadataDetails = mock(JavaTypeMetadataDetails.class);
        when(otherJavaTypeMetadataDetails.getJavaType()).thenReturn(
                mockJavaType);

        // Invoke and check
        assertTrue(javaTypeMetadataDetails.equals(otherJavaTypeMetadataDetails));
    }
}
