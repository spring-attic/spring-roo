package org.springframework.roo.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/**
 * Unit test of {@link JavaPackage}.
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class JavaPackageTest {

    private static final JavaPackage CHILD = new JavaPackage("com.foo.bar");
    private static final JavaPackage PARENT = new JavaPackage("com.foo");

    @Test
    public void testChildPackageIsWithinParent() {
        assertTrue(CHILD.isWithin(PARENT));
    }

    @Test
    public void testGetElementsOfMultiLevelPackage() {
        // Set up
        final JavaPackage javaPackage = CHILD;

        // Invoke
        final List<String> elements = javaPackage.getElements();

        // Check
        assertEquals(Arrays.asList("com", "foo", "bar"), elements);
        assertEquals("bar", javaPackage.getLastElement());
    }

    @Test
    public void testGetElementsOfSingleLevelPackage() {
        // Set up
        final JavaPackage javaPackage = new JavaPackage("me");

        // Invoke
        final List<String> elements = javaPackage.getElements();

        // Check
        assertEquals(Arrays.asList("me"), elements);
        assertEquals("me", javaPackage.getLastElement());
    }

    @Test
    public void testPackageIsNotWithinNullPackage() {
        assertFalse(PARENT.isWithin(null));
    }

    @Test
    public void testPackageIsWithinSelf() {
        assertTrue(PARENT.isWithin(PARENT));
    }

    @Test
    public void testParentPackageIsNotWithinChild() {
        assertFalse(PARENT.isWithin(CHILD));
    }
}
