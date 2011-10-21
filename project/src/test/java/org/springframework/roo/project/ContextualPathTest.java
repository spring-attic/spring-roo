package org.springframework.roo.project;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.roo.project.ContextualPath.MODULE_PATH_SEPARATOR;

import org.junit.Test;

/**
 * Unit test of {@link ContextualPath}
 *
 * @author Andrew Swan
 * @since 1.2.0
 */
public class ContextualPathTest {
	
	// Constants
	private static final Path PATH = Path.SRC_TEST_JAVA;	// arbitrary; can't be mocked
	private static final String MODULE_NAME = "web";
	private static final String MODULE_PLUS_PATH = MODULE_NAME + MODULE_PATH_SEPARATOR + PATH.name();

	@Test
	public void testGetInstanceWithNullModuleName() {
		assertGetInstance(null, "", PATH.toString());
	}

	@Test
	public void testGetInstanceWithEmptyModuleName() {
		assertGetInstance("", "", PATH.toString());
	}

	@Test
	public void testGetInstanceWithBlankModuleName() {
		assertGetInstance(" ", "", PATH.toString());
	}

	@Test
	public void testGetInstanceWithNonBlankModuleName() {
		assertGetInstance(MODULE_NAME, MODULE_NAME, MODULE_NAME + MODULE_PATH_SEPARATOR + PATH.toString());
	}
	
	@Test
	public void testGetInstanceFromPath() {
		// Invoke
		final ContextualPath instance = ContextualPath.getInstance(PATH);
		
		// Check
		assertContextualPath(instance, PATH.toString(), "");
	}
	
	/**
	 * Asserts that calling {@link ContextualPath#getInstance(Path, String)}
	 * with the given module name results in the expected behaviour
	 * 
	 * @param inputModuleName
	 * @param expectedModuleName
	 * @param expectedInstanceName
	 */
	private void assertGetInstance(final String inputModuleName, final String expectedModuleName, final String expectedInstanceName) {
		// Set up
		
		// Invoke
		final ContextualPath instance = ContextualPath.getInstance(PATH, inputModuleName);
		
		// Check
		assertContextualPath(instance, expectedInstanceName, expectedModuleName);
	}

	/**
	 * Asserts that the given instance has the expected values
	 * 
	 * @param instance the instance to check (required)
	 * @param expectedInstanceName
	 * @param expectedModuleName
	 */
	private void assertContextualPath(final ContextualPath instance, final String expectedInstanceName, final String expectedModuleName) {
		assertEquals(expectedInstanceName, instance.getName());
		assertEquals(expectedModuleName, instance.getModule());
		assertEquals(PATH, instance.getPath());
		assertEquals(instance.getName(), instance.toString());
	}
	
	@Test
	public void testSamePathsInSameModuleAreEqual() {
		// Set up
		final ContextualPath instance1 = ContextualPath.getInstance(PATH, MODULE_NAME);
		final ContextualPath instance2 = ContextualPath.getInstance(PATH, MODULE_NAME);
		
		// Invoke
		final boolean equal = instance1.equals(instance2) && instance2.equals(instance1);
		
		// Check
		assertTrue(equal);
	}
	
	@Test
	public void testSamePathsInDifferentModulesAreNotEqual() {
		// Set up
		final ContextualPath instance1 = ContextualPath.getInstance(PATH, "module1");
		final ContextualPath instance2 = ContextualPath.getInstance(PATH, "module2");
		
		// Invoke
		final boolean equal = instance1.equals(instance2) || instance2.equals(instance1);
		
		// Check
		assertFalse(equal);
	}
	
	@Test
	public void testDoesNotEqualOtherType() {
		assertFalse(ContextualPath.getInstance(PATH).equals(PATH));
	}
	
	@Test
	public void testGetInstanceFromPathNameOnly() {
		// Invoke
		final ContextualPath instance = ContextualPath.getInstance(PATH.name());
		
		// Check
		assertContextualPath(instance, PATH.name(), "");
	}
	
	@Test
	public void testGetInstanceFromCombinedPathAndModuleName() {
		// Invoke
		final ContextualPath instance = ContextualPath.getInstance(MODULE_PLUS_PATH);
		
		// Check
		assertContextualPath(instance, MODULE_PLUS_PATH, MODULE_NAME);
	}
	
	@Test(expected = NullPointerException.class)
	public void testCompareToNull() {
		ContextualPath.getInstance(PATH).compareTo(null);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testGetInstanceFromNullString() {
		ContextualPath.getInstance((String) null);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testGetInstanceFromEmptyString() {
		ContextualPath.getInstance("");
	}	
	
	@Test(expected = IllegalArgumentException.class)
	public void testGetInstanceFromBlankString() {
		ContextualPath.getInstance(" ");
	}	
}
