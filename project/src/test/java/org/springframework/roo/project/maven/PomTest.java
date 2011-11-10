package org.springframework.roo.project.maven;

import static org.junit.Assert.assertEquals;
import static org.springframework.roo.project.Path.ROOT;
import static org.springframework.roo.project.Path.SPRING_CONFIG_ROOT;
import static org.springframework.roo.project.Path.SRC_MAIN_JAVA;
import static org.springframework.roo.project.Path.SRC_MAIN_RESOURCES;
import static org.springframework.roo.project.Path.SRC_TEST_JAVA;
import static org.springframework.roo.project.Path.SRC_TEST_RESOURCES;
import static org.springframework.roo.project.maven.Pom.DEFAULT_PACKAGING;

import java.io.File;

import org.junit.Test;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PhysicalPath;

/**
 * Unit test of the {@link Pom} class
 *
 * @author Andrew Swan
 * @since 1.2.0
 */
public class PomTest {

	// Constants
	private static final String ARTIFACT_ID = "my-app";
	private static final String GROUP_ID = "com.example";
	private static final String PROJECT_ROOT = "/users/jbloggs/projects/clinic";
	private static final String ROOT_MODULE = "";
	private static final String VERSION = "1.0.1.RELEASE";
	
	@Test
	public void testDefaultPackaging() {
		assertEquals(DEFAULT_PACKAGING, getMinimalPom().getPackaging());
	}

	@Test
	public void testGetModulePathsForMinimalJarPom() {
		// Set up
		final Pom pom = getMinimalPom();
		final Path[] expectedPaths = { SRC_MAIN_JAVA, SRC_MAIN_RESOURCES, SRC_TEST_JAVA, SRC_TEST_RESOURCES, ROOT, SPRING_CONFIG_ROOT };
		
		// Invoke and check
		assertEquals(expectedPaths.length, pom.getPathInformation().size());
		for (final Path path : expectedPaths) {
			final PhysicalPath modulePath = pom.getPathInformation(path);
			assertEquals(new File(PROJECT_ROOT, path.getDefaultLocation()), modulePath.getLocation());
			assertEquals(path.isJavaSource(), modulePath.isSource());
			final LogicalPath moduelPathId = modulePath.getContextualPath();
			assertEquals(path, moduelPathId.getPath());
			assertEquals(ROOT_MODULE, moduelPathId.getModule());
		}
	}

	private Pom getMinimalPom() {
		return new Pom(GROUP_ID, ARTIFACT_ID, VERSION, null, null, null, null, null, null, null, null, null, null, null, null, null, PROJECT_ROOT + "/pom.xml", ROOT_MODULE);
	}
}
