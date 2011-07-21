package org.springframework.roo.classpath.details.annotations.populator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.junit.Test;

/**
 * Convenience superclass for unit-testing subclasses of {@link AbstractAnnotationValues}
 *
 * @author Andrew Swan
 * @since 1.2
 * @param <A> the annotation type
 * @param <V> the annotation values class
 */
public abstract class AnnotationValuesTestCase<A, V extends AbstractAnnotationValues> {

	@Test
	public void testAllAnnotationAttributesHaveAValueField() {
		final Class<A> annotationClass = getAnnotationClass();
		assertTrue("Invalid annotation class " + annotationClass, annotationClass.isAnnotation());
		for (final Method method : annotationClass.getDeclaredMethods()) {
			try {
				final Field valueField = getValuesClass().getDeclaredField(method.getName());
				final int fieldModifiers = valueField.getModifiers();
				assertFalse("Value field " + valueField + " is final", Modifier.isFinal(fieldModifiers));
				assertNotNull("Value field " + valueField + " is not auto-populated", valueField.getAnnotation(AutoPopulate.class));
			} catch (final NoSuchFieldException e) {
				fail("No value field found for annotation attribute " + method);
			}
		}
	}
	
	/**
	 * Subclasses must return the class of annotation whose value class is being
	 * tested
	 * 
	 * @return a non-<code>null</code> annotation type
	 */
	protected abstract Class<A> getAnnotationClass();
	
	/**
	 * Subclasses must return the values class being tested
	 * 
	 * @return a non-<code>null</code> class
	 */
	protected abstract Class<V> getValuesClass();
}
