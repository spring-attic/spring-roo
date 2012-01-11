package org.springframework.roo.project;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.roo.project.AbstractPathResolvingStrategy.ROOT_MODULE;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.support.osgi.OSGiUtils;
import org.springframework.roo.support.util.FileUtils;

/**
 * Unit test of {@link DefaultPathResolvingStrategy}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class DefaultPathResolvingStrategyTest {

    private static final String WORKING_DIRECTORY;

    static {
        try {
            WORKING_DIRECTORY = new File(".").getCanonicalPath();
        }
        catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    // Fixture
    private DefaultPathResolvingStrategy pathResolvingStrategy;

    /**
     * Creates a mock {@link ComponentContext} in which the
     * {@link OSGiUtils#ROO_WORKING_DIRECTORY_PROPERTY} has the given value
     * 
     * @param rooWorkingDirectory the desired property value (can be blank)
     * @return a non-<code>null</code> mock
     */
    private ComponentContext getMockComponentContext(
            final String rooWorkingDirectory) {
        final BundleContext mockBundleContext = mock(BundleContext.class);
        when(
                mockBundleContext
                        .getProperty(OSGiUtils.ROO_WORKING_DIRECTORY_PROPERTY))
                .thenReturn(rooWorkingDirectory);
        final ComponentContext mockComponentContext = mock(ComponentContext.class);
        when(mockComponentContext.getBundleContext()).thenReturn(
                mockBundleContext);
        return mockComponentContext;
    }

    @Before
    public void setUp() {
        // Object under test
        pathResolvingStrategy = new DefaultPathResolvingStrategy();
    }

    @Test
    public void testGetModulePaths() {
        // Set up
        pathResolvingStrategy.activate(getMockComponentContext(null));

        // Invoke
        final List<PhysicalPath> modulePaths = pathResolvingStrategy
                .getPhysicalPaths();

        // Check
        assertEquals(Path.values().length, modulePaths.size());
        for (int i = 0; i < modulePaths.size(); i++) {
            final PhysicalPath modulePath = modulePaths.get(i);
            final LogicalPath modulePathId = modulePath.getLogicalPath();
            final Path subPath = Path.values()[i];
            assertEquals(ROOT_MODULE, modulePathId.getModule());
            assertEquals(subPath, modulePathId.getPath());
            assertEquals(
                    new File(WORKING_DIRECTORY, subPath.getDefaultLocation()),
                    modulePath.getLocation());
        }
    }

    @Test
    public void testGetRootOfPath() {
        // Set up
        pathResolvingStrategy.activate(getMockComponentContext(null));
        final LogicalPath mockContextualPath = mock(LogicalPath.class);
        when(mockContextualPath.getPath()).thenReturn(Path.SRC_MAIN_JAVA);

        // Invoke
        final String root = pathResolvingStrategy.getRoot(mockContextualPath);

        // Check
        final String srcMainJava = FileUtils.getSystemDependentPath("src",
                "main", "java");
        assertTrue("Expected the root to end with '" + srcMainJava
                + "', but was '" + root + "'", root.endsWith(srcMainJava));
    }
}
