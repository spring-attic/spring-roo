package org.springframework.roo.project;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.roo.support.util.StringUtils;

/**
 * Unit test of {@link GAV}
 *
 * @author Andrew Swan
 * @since 1.2.0
 */
public class GAVTest {
	
	// Constants
	private static final String GROUP_ID = "org.apache.maven";
	private static final String ARTIFACT_ID = "maven-surefire-plugin";
	private static final String VERSION = "5.6";

	@Test
	public void testConstructorAndGetters() {
		// Set up
		
		// Invoke
		final GAV gav = new GAV(GROUP_ID, ARTIFACT_ID, VERSION);
		
		// Check
		assertEquals(GROUP_ID, gav.getGroupId());
		assertEquals(ARTIFACT_ID, gav.getArtifactId());
		assertEquals(VERSION, gav.getVersion());
	}
	
	@Test
	public void testGetInstance() {
		// Set up
		final String coordinates = StringUtils.arrayToDelimitedString(new String[] {GROUP_ID, ARTIFACT_ID, VERSION}, MavenUtils.COORDINATE_SEPARATOR);
		
		// Invoke
		final GAV gav = GAV.getInstance(coordinates);
		
		// Check
		assertEquals(GROUP_ID, gav.getGroupId());
		assertEquals(ARTIFACT_ID, gav.getArtifactId());
		assertEquals(VERSION, gav.getVersion());
	}
}
