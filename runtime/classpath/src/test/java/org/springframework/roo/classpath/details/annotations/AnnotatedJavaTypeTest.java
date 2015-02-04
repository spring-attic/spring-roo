package org.springframework.roo.classpath.details.annotations;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.springframework.roo.model.JavaType;

/**
 * Unit test of {@link AnnotatedJavaType}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class AnnotatedJavaTypeTest {

    @Test
    public void testConvertEmptyArrayOfJavaTypes() {
        // Invoke
        final List<AnnotatedJavaType> annotatedTypes = AnnotatedJavaType
                .convertFromJavaTypes();

        // Check
        assertEquals(0, annotatedTypes.size());
    }

    @Test
    public void testConvertNullListOfJavaTypes() {
        // Invoke
        final List<AnnotatedJavaType> annotatedTypes = AnnotatedJavaType
                .convertFromJavaTypes((List<JavaType>) null);

        // Check
        assertEquals(0, annotatedTypes.size());
    }
}
