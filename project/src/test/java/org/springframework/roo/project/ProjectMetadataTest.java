package org.springframework.roo.project;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.roo.project.ProjectMetadata.MODULE_SEPARATOR;
import static org.springframework.roo.project.ProjectMetadata.PROJECT_MID_PREFIX;

import org.junit.Test;
import org.springframework.roo.project.maven.Pom;

/**
 * Unit test of {@link ProjectMetadata}
 *
 * @author Andrew Swan
 * @since 1.2.0
 */
public class ProjectMetadataTest {

	// Constants
	private static final String MODULE_NAME = "core";
	private static final String ROOT_MID = PROJECT_MID_PREFIX;
	private static final String NON_ROOT_MID = PROJECT_MID_PREFIX + MODULE_SEPARATOR + MODULE_NAME;

	@Test
	public void testGetProjectIdentifierForRootModule() {
		assertEquals(ROOT_MID, ProjectMetadata.getProjectIdentifier(""));
	}

	@Test
	public void testGetProjectIdentifierForNonRootModule() {
		assertEquals(NON_ROOT_MID, ProjectMetadata.getProjectIdentifier(MODULE_NAME));
	}
	
	@Test
	public void testGetModuleNameFromRootModuleMID() {
		assertEquals("", ProjectMetadata.getModuleName(ROOT_MID));
	}
	
	@Test
	public void testGetModuleNameFromNonRootModuleMID() {
		assertEquals(MODULE_NAME, ProjectMetadata.getModuleName(NON_ROOT_MID));
	}
	
	@Test
	public void testRootMIDIsValid() {
		assertTrue(ProjectMetadata.isValid(ROOT_MID));
	}
	
	@Test
	public void testNonRootMIDIsValid() {
		assertTrue(ProjectMetadata.isValid(NON_ROOT_MID));
	}
	
	@Test
	public void testInvalidMIDIsNotValid() {
		assertFalse(ProjectMetadata.isValid("MID:foo#bar?baz"));
	}
	
	@Test
	public void testConstructor() {
		// Set up
		final Pom mockPom = mock(Pom.class);
		when(mockPom.getModuleName()).thenReturn(MODULE_NAME);
		
		// Invoke
		final ProjectMetadata projectMetadata = new ProjectMetadata(mockPom);
		
		// Check
		assertEquals(mockPom, projectMetadata.getPom());
		assertEquals(MODULE_NAME, projectMetadata.getModuleName());
		assertEquals(NON_ROOT_MID, projectMetadata.getId());
	}
}
