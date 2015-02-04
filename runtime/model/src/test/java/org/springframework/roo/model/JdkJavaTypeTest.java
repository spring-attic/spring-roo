package org.springframework.roo.model;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.junit.Test;

/**
 * Unit test of {@link JdkJavaType}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class JdkJavaTypeTest {

    /**
     * Asserts that the given {@link JavaType} represents a valid JDK type
     * 
     * @param javaType
     * @throws Exception
     */
    private void assertValidJdkType(final JavaType javaType) throws Exception {
        Class.forName(javaType.getFullyQualifiedTypeName());
    }

    /**
     * Indicates whether the given field is a public constant
     * 
     * @param field the field to check (required)
     * @return see above
     */
    private boolean isPublicConstant(final Field field) {
        final int modifiers = field.getModifiers();
        return Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers)
                && Modifier.isFinal(modifiers);
    }

    @Test
    public void testClassNamesAreActualJdkTypes() throws Exception {
        for (final Field field : JdkJavaType.class.getDeclaredFields()) {
            if (isPublicConstant(field)
                    && JavaType.class.equals(field.getType())) {
                assertValidJdkType((JavaType) field.get(null));
            }
        }
    }
}
