package org.springframework.roo.classpath.details.annotations;

import static org.junit.Assert.assertEquals;
import static org.springframework.roo.model.JavaType.STRING;
import static org.springframework.roo.model.JpaJavaType.ID;

import org.junit.Test;

/**
 * Unit test of {@link AnnotationMetadataBuilder}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class AnnotationMetadataBuilderTest {

    @Test
    public void testGetInstanceFromClassObject() {
        // Invoke
        final AnnotationMetadata annotationMetadata = AnnotationMetadataBuilder
                .getInstance(String.class);

        // Check
        assertEquals(0, annotationMetadata.getAttributeNames().size());
        assertEquals(STRING.getFullyQualifiedTypeName(), annotationMetadata
                .getAnnotationType().getFullyQualifiedTypeName());
    }

    @Test
    public void testGetInstanceFromFullyQualifiedClassName() {
        // Invoke
        final AnnotationMetadata annotationMetadata = AnnotationMetadataBuilder
                .getInstance(ID);

        // Check
        assertEquals(0, annotationMetadata.getAttributeNames().size());
        assertEquals(ID.getFullyQualifiedTypeName(), annotationMetadata
                .getAnnotationType().getFullyQualifiedTypeName());
    }
}
