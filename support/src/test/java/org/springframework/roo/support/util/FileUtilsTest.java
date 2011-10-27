package org.springframework.roo.support.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

/**
 * Unit test of {@link FileUtils}
 *
 * @author Andrew Swan
 * @since 1.2.0
 */
public class FileUtilsTest {
	
	@Test(expected = NullPointerException.class)
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
	
	@Test
	public void testRemoveTrailingSeparatorFromNullPath() {
		assertNull(FileUtils.removeTrailingSeparator(null));
	}
	
	@Test
	public void testRemoveTrailingSeparatorFromEmptyPath() {
		assertEquals("", FileUtils.removeTrailingSeparator(""));
	}
	
	@Test
	public void testRemoveTrailingSeparatorFromPathWithLeadingSeparator() {
		final String path = File.separator + "foo";
		assertEquals(path, FileUtils.removeTrailingSeparator(path));
	}
	
	@Test
	public void testRemoveTrailingSeparatorFromPathWithMultipleTrailingSeparators() {
		final String path = "foo" + StringUtils.repeat(File.separator, 3);
		assertEquals("foo", FileUtils.removeTrailingSeparator(path));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testEnsureTrailingSeparatorForNullPath() {
		FileUtils.ensureTrailingSeparator(null);
	}
	
	@Test
	public void testEnsureTrailingSeparatorForEmptyPath() {
		assertEquals(File.separator, FileUtils.ensureTrailingSeparator(""));
	}
	
	@Test
	public void testEnsureTrailingSeparatorForPathWithNoTrailingSeparator() {
		final String path = "foo";
		assertEquals(path + File.separator, FileUtils.ensureTrailingSeparator(path));
	}
	
	@Test
	public void testEnsureTrailingSeparatorForPathWithOneTrailingSeparator() {
		final String path = "foo" + File.separator;
		assertEquals(path, FileUtils.ensureTrailingSeparator(path));
	}
	
	@Test
	public void testEnsureTrailingSeparatorFromPathWithMultipleTrailingSeparators() {
		final String path = "foo" + StringUtils.repeat(File.separator, 3);
		assertEquals("foo" + File.separator, FileUtils.ensureTrailingSeparator(path));
	}
	
	@Test
	public void testGetCanonicalPathForNullFile() {
		assertNull(FileUtils.getCanonicalPath(null));
	}
	
	@Test(expected = IllegalStateException.class)
	public void testGetCanonicalPathForInvalidFile() throws Exception {
		// Set up
		final File invalidFile = mock(File.class);
		when(invalidFile.getCanonicalPath()).thenThrow(new IOException("dummy"));
		
		// Invoke
		FileUtils.getCanonicalPath(invalidFile);
	}
	
	@Test
	public void testGetCanonicalPathForValidFile() throws Exception {
		// Set up
		final File validFile = mock(File.class);
		final String canonicalPath = "the_path";
		when(validFile.getCanonicalPath()).thenReturn(canonicalPath);
		
		// Invoke
		final String actualPath = FileUtils.getCanonicalPath(validFile);
		
		// Check
		assertEquals(canonicalPath, actualPath);
	}
	
	@Test
	public void testRemoveLeadingAndTrailingSeparatorsFromNullPath() {
		assertNull(FileUtils.removeLeadingAndTrailingSeparators(null));
	}
	
	@Test
	public void testRemoveLeadingAndTrailingSeparatorsFromEmptyPath() {
		assertEquals("", FileUtils.removeLeadingAndTrailingSeparators(""));
	}
	
	@Test
	public void testRemoveLeadingAndTrailingSeparatorsFromPlainPath() {
		final String path = "foo";
		assertEquals(path, FileUtils.removeLeadingAndTrailingSeparators(path));
	}
	
	@Test
	public void testRemoveLeadingAndTrailingSeparatorsFromPathWithBoth() {
		// Set up
		final String separators = StringUtils.repeat(File.separator, 4);
		final String path = separators + "foo" + separators;
		
		// Invoke and check
		assertEquals("foo", FileUtils.removeLeadingAndTrailingSeparators(path));
	}
}
