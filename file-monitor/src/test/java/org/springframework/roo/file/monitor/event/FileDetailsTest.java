package org.springframework.roo.file.monitor.event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.File;

import org.junit.Test;

/**
 * Unit test of {@link FileDetails}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class FileDetailsTest {

    @Test
    public void testInstancesWithSameFileAndNullTimestamp() {
        // Set up
        final File mockFile = mock(File.class);
        final FileDetails fileDetails1 = new FileDetails(mockFile, null);
        final FileDetails fileDetails2 = new FileDetails(mockFile, null);

        // Invoke and check
        assertTrue(fileDetails1.equals(fileDetails2)
                && fileDetails2.equals(fileDetails1));
        assertEquals(fileDetails1.hashCode(), fileDetails2.hashCode());
        assertEquals(0, fileDetails1.compareTo(fileDetails2));
    }
}
