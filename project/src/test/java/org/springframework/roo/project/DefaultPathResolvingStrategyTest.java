package org.springframework.roo.project;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

	// Fixture
	private DefaultPathResolvingStrategy pathResolvingStrategy;
	
	@Before
	public void setUp() {
		// Object under test
		this.pathResolvingStrategy = new DefaultPathResolvingStrategy();
	}

	/**
	 * Creates a mock {@link ComponentContext} in which the
	 * {@link OSGiUtils#ROO_WORKING_DIRECTORY_PROPERTY} has the given value
	 * 
	 * @param rooWorkingDirectory the desired property value (can be blank)
	 * @return a non-<code>null</code> mock
	 */
	private ComponentContext getMockComponentContext(final String rooWorkingDirectory) {
		final BundleContext mockBundleContext = mock(BundleContext.class);
		when(mockBundleContext.getProperty(OSGiUtils.ROO_WORKING_DIRECTORY_PROPERTY)).thenReturn(rooWorkingDirectory);
		final ComponentContext mockComponentContext = mock(ComponentContext.class);
		when(mockComponentContext.getBundleContext()).thenReturn(mockBundleContext);
		return mockComponentContext;
	}
	
	@Test
	public void testGetRootOfPath() {
		// Set up
		this.pathResolvingStrategy.activate(getMockComponentContext(null));
		final ContextualPath mockContextualPath = mock(ContextualPath.class);
		when(mockContextualPath.getPath()).thenReturn(Path.SRC_MAIN_JAVA);
		
		// Invoke
		final String root = this.pathResolvingStrategy.getRoot(mockContextualPath);
		
		// Check
		final String srcMainJava = FileUtils.getSystemDependentPath("src", "main", "java");
		assertTrue("Expected the root to end with '" + srcMainJava + "', but was '" + root + "'", root.endsWith(srcMainJava));
	}
}
