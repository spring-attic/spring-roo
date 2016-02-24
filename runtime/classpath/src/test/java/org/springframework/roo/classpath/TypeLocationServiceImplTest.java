package org.springframework.roo.classpath;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

/**
 * Unit test of {@link TypeLocationServiceImpl}.
 * 
 * @author Andrew Swan
 * @since 1.3.0
 */
public class TypeLocationServiceImplTest extends TestCase {

    public void testGetAllPackages() {
        // Set up
        final String leafPackage = "com.foo.bar";

        // Invoke
        final Set<String> allPackages = TypeLocationServiceImpl
                .getAllPackages(leafPackage);

        // Check
        assertEquals(3, allPackages.size());
        assertTrue(allPackages.contains("com"));
        assertTrue(allPackages.contains("com.foo"));
        assertTrue(allPackages.contains("com.foo.bar"));
    }

    public void testGetPackageFromType() {
        // Set up
        final String type = "com.foo.Bar";

        // Invoke
        final String pkg = TypeLocationServiceImpl.getPackageFromType(type);

        // Check
        assertEquals("com.foo", pkg);
    }

    public void testGetLowestCommonPackageWhenOneExists() {
        // Set up
        final String type1 = "com.foo.bar.A";
        final String type2 = "com.foo.baz.B";
        final Map<String, Collection<String>> typesByPackage = new LinkedHashMap<String, Collection<String>>();
        typesByPackage.put("com", Arrays.asList(type1, type2));
        typesByPackage.put("com.foo", Arrays.asList(type1, type2));
        typesByPackage.put("com.foo.bar", Arrays.asList(type1));
        typesByPackage.put("com.foo.baz", Arrays.asList(type2));

        // Invoke
        final String lowestCommonPackage = TypeLocationServiceImpl
                .getLowestCommonPackage(2, typesByPackage);

        // Check
        assertEquals("com.foo", lowestCommonPackage);
    }
}
