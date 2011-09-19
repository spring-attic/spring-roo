package org.springframework.roo.model;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/**
 * Unit test of {@link JavaPackage}
 *
 * @author Andrew Swan
 * @since 1.2.0
 */
public class JavaPackageTest {

	@Test
	public void testGetElementsOfMultiLevelPackage() {
		// Set up
		final JavaPackage javaPackage = new JavaPackage("com.foo.bar");
		
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
}
