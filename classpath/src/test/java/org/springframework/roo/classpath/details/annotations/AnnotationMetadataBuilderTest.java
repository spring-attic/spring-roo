package org.springframework.roo.classpath.details.annotations;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit test of {@link AnnotationMetadataBuilder}
 *
 * @author Andrew Swan
 * @since 1.2
 */
public class AnnotationMetadataBuilderTest {

	// Constants
	private static final String ANNOTATION_CLASS_NAME = "javax.persistence.Id";

	@Test
	public void testGetInstanceFromClassObject() {
		// Invoke
		final AnnotationMetadata annotationMetadata = AnnotationMetadataBuilder.getInstance(String.class);
		
		// Check
		assertEquals(0, annotationMetadata.getAttributeNames().size());
		assertEquals("java.lang.String", annotationMetadata.getAnnotationType().getFullyQualifiedTypeName());
	}
	
	@Test
	public void testGetInstanceFromFullyQualifiedClassName() {
		// Invoke
		final AnnotationMetadata annotationMetadata = AnnotationMetadataBuilder.getInstance(ANNOTATION_CLASS_NAME);
		
		// Check
		assertEquals(0, annotationMetadata.getAttributeNames().size());
		assertEquals(ANNOTATION_CLASS_NAME, annotationMetadata.getAnnotationType().getFullyQualifiedTypeName());
	}
}
