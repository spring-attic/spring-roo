package org.springframework.roo.support.util;

import static org.junit.Assert.assertEquals;

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
}
