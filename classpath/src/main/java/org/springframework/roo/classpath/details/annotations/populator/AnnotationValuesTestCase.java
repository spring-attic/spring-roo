package org.springframework.roo.classpath.details.annotations.populator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.apache.commons.lang3.Validate;
import org.junit.Test;

/**
 * Convenience superclass for unit-testing subclasses of
 * {@link AbstractAnnotationValues}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 * @param <A> the annotation type
 * @param <V> the annotation values class
 */
public abstract class AnnotationValuesTestCase<A, V extends AbstractAnnotationValues> {

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

    @Test
    public void testAllAnnotationAttributesHaveAValueField() {
        final Class<A> annotationClass = getAnnotationClass();
        assertTrue("Invalid annotation class " + annotationClass,
                annotationClass.isAnnotation());
        for (final Method method : annotationClass.getDeclaredMethods()) {
            // Look for a field of this name in the values class or any
            // superclass
            final Field valueField = findField(getValuesClass(),
                    method.getName());
            assertNotNull("No value field found for annotation attribute "
                    + method, valueField);
            final int fieldModifiers = valueField.getModifiers();
            assertFalse("Value field " + valueField + " is final",
                    Modifier.isFinal(fieldModifiers));
            assertNotNull("Value field " + valueField
                    + " is not auto-populated",
                    valueField.getAnnotation(AutoPopulate.class));
        }
    }

    private Field findField(final Class<?> clazz, final String name) {
        Validate.notNull(clazz, "Class must not be null");
        Validate.isTrue(name != null,
                "Either name or type of the field must be specified");
        Class<?> searchType = clazz;
        while (!Object.class.equals(searchType) && searchType != null) {
            final Field[] fields = searchType.getDeclaredFields();
            for (final Field field : fields) {
                if ((name == null || name.equals(field.getName()))) {
                    return field;
                }
            }
            searchType = searchType.getSuperclass();
        }
        return null;
    }
}
