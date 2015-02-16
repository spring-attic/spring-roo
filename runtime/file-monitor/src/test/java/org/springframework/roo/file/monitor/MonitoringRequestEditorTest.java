package org.springframework.roo.file.monitor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.roo.file.monitor.event.FileOperation.CREATED;
import static org.springframework.roo.file.monitor.event.FileOperation.DELETED;
import static org.springframework.roo.file.monitor.event.FileOperation.RENAMED;
import static org.springframework.roo.file.monitor.event.FileOperation.UPDATED;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.roo.file.monitor.event.FileOperation;

/**
 * Unit test of {@link MonitoringRequestEditor}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class MonitoringRequestEditorTest {

    private static final File TEMP_DIR = new File(
            System.getProperty("java.io.tmpdir"));

    private MonitoringRequestEditor editor;
    // Fixture
    private File testDirectory;
    private File testFile;

    /**
     * Asserts that the editor converts the given {@link MonitoringRequest} to
     * the given text
     * 
     * @param mockMonitoringRequest
     * @param expectedText
     * @throws Exception
     */
    private void assertAsText(final MonitoringRequest mockMonitoringRequest,
            final String expectedText) throws Exception {
        // Set up
        final File mockFile = mock(File.class);
        when(mockMonitoringRequest.getFile()).thenReturn(mockFile);
        when(mockFile.getCanonicalPath()).thenReturn("/path/to/file");
        final FileOperation[] operations = { CREATED, DELETED };
        when(mockMonitoringRequest.getNotifyOn()).thenReturn(
                Arrays.asList(operations));
        editor.setValue(mockMonitoringRequest);

        // Invoke
        final String text = editor.getAsText();

        // Check
        assertEquals(expectedText, text);
    }

    /**
     * Asserts that passing the given text to the
     * {@link MonitoringRequestEditor} results in a <code>null</code>
     * {@link MonitoringRequest}.
     * 
     * @param text the text to pass (can be blank)
     */
    private void assertCreatesNullMonitoringRequest(final String text) {
        // Set up
        editor.setAsText(text);

        // Invoke
        final MonitoringRequest monitoringRequest = editor.getValue();

        // Check
        assertNull(monitoringRequest);
    }

    /**
     * Asserts that passing the given text to
     * {@link MonitoringRequestEditor#setAsText(String)} results in a
     * {@link MonitoringRequest} with the given values
     * 
     * @param text the text to parse as a {@link MonitoringRequest}
     * @param expectedFile the file we expect to be monitored
     * @param expectedFileOperations the operations about which we expect to be
     *            notified
     * @return the generated {@link MonitoringRequest} for any further
     *         assertions
     */
    private MonitoringRequest assertMonitoringRequest(final String text,
            final File expectedFile,
            final FileOperation... expectedFileOperations) {
        // Set up
        editor.setAsText(text);

        // Invoke
        final MonitoringRequest monitoringRequest = editor.getValue();

        // Check
        assertEquals(expectedFile, monitoringRequest.getFile());
        final Collection<FileOperation> notifyOn = monitoringRequest
                .getNotifyOn();
        assertEquals(expectedFileOperations.length, notifyOn.size());
        assertTrue("Expected " + Arrays.toString(expectedFileOperations)
                + " but was " + notifyOn,
                notifyOn.containsAll(Arrays.asList(expectedFileOperations)));
        return monitoringRequest;
    }

    @Before
    public void setUp() throws Exception {
        editor = new MonitoringRequestEditor();
        testDirectory = new File(TEMP_DIR, getClass().getSimpleName());
        testDirectory.mkdir();
        testFile = File.createTempFile(getClass().getSimpleName(), null);
    }

    @After
    public void tearDown() {
        testDirectory.delete();
        testFile.delete();
    }

    @Test
    public void testGetAsTextWhenMonitoringDirectoryAndSubTree()
            throws Exception {
        // Set up
        final DirectoryMonitoringRequest mockMonitoringRequest = mock(DirectoryMonitoringRequest.class);
        when(mockMonitoringRequest.isWatchSubtree()).thenReturn(true);
        assertAsText(mockMonitoringRequest, "/path/to/file,CD,**");
    }

    @Test
    public void testGetAsTextWhenMonitoringDirectoryOnly() throws Exception {
        // Set up
        final DirectoryMonitoringRequest mockMonitoringRequest = mock(DirectoryMonitoringRequest.class);
        when(mockMonitoringRequest.isWatchSubtree()).thenReturn(false);
        assertAsText(mockMonitoringRequest, "/path/to/file,CD");
    }

    @Test
    public void testGetAsTextWhenMonitoringFile() throws Exception {
        assertAsText(mock(MonitoringRequest.class), "/path/to/file,CD");
    }

    @Test
    public void testGetAsTextWhenNoValueSet() {
        assertNull(editor.getAsText());
    }

    @Test
    public void testMonitorDirectoryAndSubtreeForDelete() {
        final MonitoringRequest monitoringRequest = assertMonitoringRequest(
                testDirectory.getAbsolutePath() + ",D,**", testDirectory,
                DELETED);
        final DirectoryMonitoringRequest directoryMonitoringRequest = (DirectoryMonitoringRequest) monitoringRequest;
        assertTrue(directoryMonitoringRequest.isWatchSubtree());
    }

    @Test
    public void testMonitorDirectoryButNotSubtreeForRename() {
        final MonitoringRequest monitoringRequest = assertMonitoringRequest(
                testDirectory.getAbsolutePath() + ",R", testDirectory, RENAMED);
        final DirectoryMonitoringRequest directoryMonitoringRequest = (DirectoryMonitoringRequest) monitoringRequest;
        assertFalse(directoryMonitoringRequest.isWatchSubtree());
    }

    @Test
    public void testMonitorFileForRenameUpdateOrDelete() {
        assertMonitoringRequest(testFile.getAbsolutePath() + ",RUD", testFile,
                RENAMED, UPDATED, DELETED);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMonitoringSubTreeOfFileIsInvalid() {
        editor.setAsText(testFile.getAbsolutePath() + ",C,**");
    }

    @Test
    public void testSettingEmptyAsTextCreatesNullValue() {
        assertCreatesNullMonitoringRequest("");
    }

    @Test
    public void testSettingNullAsTextCreatesNullValue() {
        assertCreatesNullMonitoringRequest(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSettingTextWithNoCommaIsInvalid() {
        editor.setAsText("foo");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSettingTextWithNoOperationCodesIsInvalid() {
        editor.setAsText("foo,");
    }
}
