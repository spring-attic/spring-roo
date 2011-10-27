package org.springframework.roo.project;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.roo.project.maven.Pom;

/**
 * Unit test of {@link MavenPathResolvingStrategy}
 *
 * @author Andrew Swan
 * @since 1.2.0
 */
public class MavenPathResolvingStrategyTest {
	
	// Constants
	private static String ROOT_MODULE = "";
	
	// Fixture
	private MavenPathResolvingStrategy strategy;
	@Mock private PomManagementService mockPomManagementService;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		strategy = new MavenPathResolvingStrategy();
		strategy.pomManagementService = mockPomManagementService;
	}

	@Test
	public void testGetIdentifierForRootPathOfRootModule() {
		// Set up
		final ContextualPath mockContextualPath = mock(ContextualPath.class);
		when(mockContextualPath.getModule()).thenReturn(ROOT_MODULE);
		when(mockContextualPath.getPath()).thenReturn(Path.ROOT);	// can't be mocked
		final Pom mockPom = mock(Pom.class);
		final String rootPath = "/path/to/the/pom";
		when(mockPom.getRoot()).thenReturn(rootPath);
		when(mockPomManagementService.getPomFromModuleName(ROOT_MODULE)).thenReturn(mockPom);
		
		// Invoke
		final String identifier = strategy.getIdentifier(mockContextualPath , "");
		
		// Check
		assertEquals(rootPath + File.separator, identifier);	// TODO check this is what we actually want
	}
}
