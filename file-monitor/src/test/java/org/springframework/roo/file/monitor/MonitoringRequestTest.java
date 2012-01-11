package org.springframework.roo.file.monitor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.roo.file.monitor.event.FileOperation.CREATED;
import static org.springframework.roo.file.monitor.event.FileOperation.DELETED;
import static org.springframework.roo.file.monitor.event.FileOperation.RENAMED;
import static org.springframework.roo.file.monitor.event.FileOperation.UPDATED;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.springframework.roo.file.monitor.event.FileOperation;

/**
 * Unit test of {@link MonitoringRequest}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class MonitoringRequestTest {

    private static final FileOperation[] CRUD_OPERATIONS = { CREATED, RENAMED,
            UPDATED, DELETED };

    /**
     * Asserts that the given {@link MonitoringRequest} is for
     * {@link #CRUD_OPERATIONS} on the current directory.
     * 
     * @param monitoringRequest the request to check (required)
     * @param expectedToWatchSubTree whether we expect the sub-tree to be
     *            monitored as well
     */
    private void assertMonitorsCurrentDirectory(
            final MonitoringRequest monitoringRequest,
            final boolean expectedToWatchSubTree) {
        assertEquals(DirectoryMonitoringRequest.class,
                monitoringRequest.getClass());
        assertEquals(expectedToWatchSubTree,
                ((DirectoryMonitoringRequest) monitoringRequest)
                        .isWatchSubtree());
        final Collection<FileOperation> notifyOn = monitoringRequest
                .getNotifyOn();
        assertEquals(CRUD_OPERATIONS.length, notifyOn.size());
        assertTrue(notifyOn.containsAll(Arrays.asList(CRUD_OPERATIONS)));
        assertEquals(new File("."), monitoringRequest.getFile());
    }

    @Test
    public void testGetMonitoringRequestForCurrentDirectoryAndSubTree() {
        assertMonitorsCurrentDirectory(
                MonitoringRequest.getInitialSubTreeMonitoringRequest(null),
                true);
    }

    @Test
    public void testGetMonitoringRequestForCurrentDirectoryOnly() {
        assertMonitorsCurrentDirectory(
                MonitoringRequest.getInitialMonitoringRequest(null), false);
    }
}
