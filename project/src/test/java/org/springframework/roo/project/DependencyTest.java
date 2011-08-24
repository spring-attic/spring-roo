package org.springframework.roo.project;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit test of the {@link Dependency} class
 *
 * @author Andrew Swan
 * @since 1.2.0
 */
public class DependencyTest {

	// Constants
	private static final String ARTIFACT_ID = "foo-api";
	private static final String EXCLUSION_ARTIFACT_ID = "ugly-api";
	private static final String EXCLUSION_GROUP_ID = "com.ugliness";
	private static final String GROUP_ID = "com.bar";
	private static final String VERSION = "6.6.6";

	@Test
	public void testAddExclusion() {
		// Set up
		final Dependency dependency = new Dependency(GROUP_ID, ARTIFACT_ID, VERSION);
		final int originalExclusionCount = dependency.getExclusions().size();
		
		// Invoke
		dependency.addExclusion(EXCLUSION_GROUP_ID, EXCLUSION_ARTIFACT_ID);
		
		// Check
		assertEquals(originalExclusionCount + 1, dependency.getExclusions().size());
	}
}
