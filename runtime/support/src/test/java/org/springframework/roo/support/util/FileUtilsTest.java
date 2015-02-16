package org.springframework.roo.support.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.springframework.roo.support.util.loader.Loader;

/**
 * Unit test of {@link FileUtils}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class FileUtilsTest {

    private static final String MISSING_FILE = "no-such-file.txt";
    private static final String TEST_FILE = "sub" + File.separator
            + "file-utils-test.txt";

    private void assertFirstDirectory(final String path,
            final String expectedFirstDirectory) {
        // Invoke
        final String firstDirectory = FileUtils.getFirstDirectory(path);

        // Check
        assertEquals(expectedFirstDirectory, firstDirectory);
    }

    @Test
    public void testBackOneDirectory() {
        assertEquals(
                "foo" + File.separator + "bar",
                FileUtils.backOneDirectory("foo" + File.separator + "bar"
                        + File.separator + "baz" + File.separator));
    }

    @Test
    public void testEnsureTrailingSeparatorForEmptyPath() {
        assertEquals(File.separator, FileUtils.ensureTrailingSeparator(""));
    }

    @Test(expected = NullPointerException.class)
    public void testEnsureTrailingSeparatorForNullPath() {
        FileUtils.ensureTrailingSeparator(null);
    }

    @Test
    public void testEnsureTrailingSeparatorForPathWithNoTrailingSeparator() {
        final String path = "foo";
        assertEquals(path + File.separator,
                FileUtils.ensureTrailingSeparator(path));
    }

    @Test
    public void testEnsureTrailingSeparatorForPathWithOneTrailingSeparator() {
        final String path = "foo" + File.separator;
        assertEquals(path, FileUtils.ensureTrailingSeparator(path));
    }

    @Test
    public void testEnsureTrailingSeparatorFromPathWithMultipleTrailingSeparators() {
        final String path = "foo" + StringUtils.repeat(File.separator, 3);
        assertEquals("foo" + File.separator,
                FileUtils.ensureTrailingSeparator(path));
    }

    @Test(expected = IllegalStateException.class)
    public void testGetCanonicalPathForInvalidFile() throws Exception {
        // Set up
        final File invalidFile = mock(File.class);
        when(invalidFile.getCanonicalPath())
                .thenThrow(new IOException("dummy"));

        // Invoke
        FileUtils.getCanonicalPath(invalidFile);
    }

    @Test
    public void testGetCanonicalPathForNullFile() {
        assertNull(FileUtils.getCanonicalPath(null));
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
    public void testGetFileSeparatorAsRegex() throws Exception {
        // Set up
        final String regex = FileUtils.getFileSeparatorAsRegex();
        final String currentDirectory = new File(FileUtils.CURRENT_DIRECTORY)
                .getCanonicalPath();

        // Invoke
        final String[] pathElements = currentDirectory.split(regex);

        // Check
        assertTrue(pathElements.length > 0);
    }

    @Test
    public void testGetFirstDirectoryOfExistingDirectory() throws Exception {
        // Set up

        final URL url = Loader.class.getResource(TEST_FILE);
        final File file = org.apache.commons.io.FileUtils.toFile(url);
        final String directory = file.getParent();

        // Invoke
        final String firstDirectory = FileUtils.getFirstDirectory(directory);

        // Check
        assertTrue(firstDirectory.endsWith("sub"));
    }

    @Test
    public void testGetFirstDirectoryOfExistingFile() {
        assertFirstDirectory(TEST_FILE, "sub");
    }

    @Test
    public void testGetInputStreamOfFileInSubDirectory() throws Exception {
        // Invoke
        final InputStream inputStream = FileUtils.getInputStream(Loader.class,
                TEST_FILE);

        // Check
        final String contents = IOUtils.toString(inputStream);
        assertEquals("This file is required for FileUtilsTest.", contents);
    }

    @Test(expected = NullPointerException.class)
    public void testGetInputStreamOfInvalidFile() throws Exception {
        FileUtils.getInputStream(Loader.class, MISSING_FILE);
    }

    @Test
    public void testGetPath() {
        assertEquals(
                "/org/springframework/roo/support/util/loader/sub/file-utils-test.txt",
                FileUtils.getPath(Loader.class, "sub/file-utils-test.txt"));
    }

    @Test
    public void testGetSystemDependentPathFromMultipleElements() {
        final String expectedPath = "foo" + File.separator + "bar";
        assertEquals(expectedPath,
                FileUtils.getSystemDependentPath("foo", "bar"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetSystemDependentPathFromNoElements() {
        FileUtils.getSystemDependentPath();
    }

    @Test(expected = NullPointerException.class)
    public void testGetSystemDependentPathFromNullArray() {
        FileUtils.getSystemDependentPath((String[]) null);
    }

    @Test
    public void testGetSystemDependentPathFromOneElement() {
        assertEquals("foo", FileUtils.getSystemDependentPath("foo"));
    }
}
