package org.springframework.roo.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.roo.model.JavaType.BOOLEAN_OBJECT;
import static org.springframework.roo.model.JavaType.BOOLEAN_PRIMITIVE;
import static org.springframework.roo.model.JavaType.BYTE_ARRAY_PRIMITIVE;
import static org.springframework.roo.model.JavaType.INT_OBJECT;
import static org.springframework.roo.model.JavaType.OBJECT;
import static org.springframework.roo.model.JavaType.STRING;
import static org.springframework.roo.model.JavaType.listOf;

import org.junit.Test;

public class JavaTypeTest {

    @Test
    public void testArrayTypeIsMultiValued() {
        assertTrue(BYTE_ARRAY_PRIMITIVE.isMultiValued());
    }

    @Test
    public void testBooleanObjectIsBoolean() {
        assertTrue(BOOLEAN_OBJECT.isBoolean());
    }

    @Test
    public void testBooleanPrimitiveIsBoolean() {
        assertTrue(BOOLEAN_PRIMITIVE.isBoolean());
    }

    @Test
    public void testCollectionTypesAreMultiValued() {
        assertTrue(listOf(INT_OBJECT).isMultiValued());
    }

    @Test
    public void testCoreTypeIsCoreType() {
        assertTrue(STRING.isCoreType());
    }

    @Test
    public void testEnclosingTypeDetection() {

        // No enclosing types
        assertNull(new JavaType("BarBar").getEnclosingType());
        assertNull(new JavaType("com.foo.Car").getEnclosingType());
        assertNull(new JavaType("foo.Sar").getEnclosingType());
        assertNull(new JavaType("bob").getEnclosingType());

        // Enclosing type in default package
        assertEquals(new JavaType("Bob"),
                new JavaType("Bob.Smith").getEnclosingType());
        assertEquals(new JavaPackage(""), new JavaType("Bob.Smith")
                .getEnclosingType().getPackage());

        // Enclosing type in declared package
        assertEquals(new JavaType("foo.My"),
                new JavaType("foo.My.Sar").getEnclosingType());

        // Enclosing type in declared package several levels deep
        assertEquals(new JavaType("foo.bar.My"),
                new JavaType("foo.bar.My.Sar").getEnclosingType());
        assertEquals("com.foo._MyBar",
                new JavaType("com.foo._MyBar").getFullyQualifiedTypeName());
        assertEquals(new JavaType("com.Foo.Bar"),
                new JavaType("com.Foo.Bar.My").getEnclosingType());
        assertEquals(new JavaType("com.foo.BAR"),
                new JavaType("com.foo.BAR.My").getEnclosingType());

        // Enclosing type us explicitly specified
        assertEquals(new JavaPackage("com.foo"), new JavaType(
                "com.foo.Bob.Smith", new JavaType("com.foo.Bob"))
                .getEnclosingType().getPackage());
        assertEquals(new JavaType("com.foo.Bob"),
                new JavaType("com.foo.Bob.Smith", new JavaType("com.foo.Bob"))
                        .getEnclosingType());
    }

    @Test
    public void testGetBaseTypeForNonCollectionType() {
        assertEquals(STRING, STRING.getBaseType());
    }

    @Test
    public void testGetBaseTypeForParameterisedCollectionType() {
        assertEquals(STRING, JavaType.listOf(STRING).getBaseType());
    }

    @Test
    public void testGetBaseTypeForUnparameterisedCollectionType() {
        assertNull(JdkJavaType.LIST.getBaseType());
    }

    @Test
    public void testObjectIsNotBoolean() {
        assertFalse(OBJECT.isBoolean());
    }

    @Test
    public void testSingleValuedTypeIsNotMultiValued() {
        assertFalse(STRING.isMultiValued());
    }

    @Test
    public void testTypeInRootPackage() {
        assertEquals("", new JavaType("MyRootClass").getPackage()
                .getFullyQualifiedPackageName());
    }

    @Test
    public void testUserTypeIsNonCoreType() {
        assertFalse(new JavaType("com.example.Thing").isCoreType());
    }
}
