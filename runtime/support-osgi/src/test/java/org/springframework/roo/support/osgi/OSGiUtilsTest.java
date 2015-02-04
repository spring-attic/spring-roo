package org.springframework.roo.support.osgi;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.roo.support.osgi.OSGiUtils.ROO_WORKING_DIRECTORY_PROPERTY;

import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

/**
 * Unit test of {@link OSGiUtils}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class OSGiUtilsTest {

    private static final String ROO_WORKING_DIRECTORY = "/some/file/path";

    @Test
    public void testGetRooWorkingDirectory() {
        // Set up
        final BundleContext mockBundleContext = mock(BundleContext.class);
        when(mockBundleContext.getProperty(ROO_WORKING_DIRECTORY_PROPERTY))
                .thenReturn(ROO_WORKING_DIRECTORY);
        final ComponentContext mockComponentContext = mock(ComponentContext.class);
        when(mockComponentContext.getBundleContext()).thenReturn(
                mockBundleContext);

        // Invoke
        final String rooWorkingDirectory = OSGiUtils
                .getRooWorkingDirectory(mockComponentContext);

        // Check
        assertEquals(ROO_WORKING_DIRECTORY, rooWorkingDirectory);
    }
}
