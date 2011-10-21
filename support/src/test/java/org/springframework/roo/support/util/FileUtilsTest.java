package org.springframework.roo.support.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

/**
 * Unit test of {@link FileUtils}
 *
 * @author Andrew Swan
 * @since 1.2.0
 */
public class FileUtilsTest {
	
	@Test(expected = IllegalArgumentException.class)
	public void testGetSystemDependentPathFromNullArray() {
		FileUtils.getSystemDependentPath((String[]) null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetSystemDependentPathFromNoElements() {
		FileUtils.getSystemDependentPath();
	}
	
	@Test
	public void testGetSystemDependentPathFromOneElement() {
		assertEquals("foo", FileUtils.getSystemDependentPath("foo"));
	}
	
	@Test
	public void testGetSystemDependentPathFromMultipleElements() {
		final String expectedPath = "foo" + File.separator + "bar";
		assertEquals(expectedPath, FileUtils.getSystemDependentPath("foo", "bar"));
	}
	
	@Test
	public void testGetFileSeparatorAsRegex() throws Exception {
		// Set up
		final String regex = FileUtils.getFileSeparatorAsRegex();
		final String currentDirectory = new File(FileUtils.CURRENT_DIRECTORY).getCanonicalPath();
		
		// Invoke
		final String[] pathElements = currentDirectory.split(regex);
		
		// Check
		assertTrue(pathElements.length > 0);
	}
}
