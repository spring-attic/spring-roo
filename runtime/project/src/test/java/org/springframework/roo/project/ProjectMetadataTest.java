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

    private static final String LEVEL_ONE_MODULE = "core";
    private static final String LEVEL_ONE_MID = PROJECT_MID_PREFIX
            + MODULE_SEPARATOR + LEVEL_ONE_MODULE;
    private static final String LEVEL_TWO_MODULE = LEVEL_ONE_MODULE
            + MODULE_SEPARATOR + "sub";
    private static final String LEVEL_TWO_MID = PROJECT_MID_PREFIX
            + MODULE_SEPARATOR + LEVEL_TWO_MODULE;
    private static final String ROOT_MID = PROJECT_MID_PREFIX;

    @Test
    public void testConstructorForLevelTwoModule() {
        // Set up
        final Pom mockPom = mock(Pom.class);
        when(mockPom.getModuleName()).thenReturn(LEVEL_TWO_MODULE);

        // Invoke
        final ProjectMetadata projectMetadata = new ProjectMetadata(mockPom);

        // Check
        assertEquals(mockPom, projectMetadata.getPom());
        assertEquals(LEVEL_TWO_MODULE, projectMetadata.getModuleName());
        assertEquals(LEVEL_TWO_MID, projectMetadata.getId());
    }

    @Test
    public void testGetModuleNameFromLevelOneModuleMID() {
        assertEquals(LEVEL_ONE_MODULE,
                ProjectMetadata.getModuleName(LEVEL_ONE_MID));
    }

    @Test
    public void testGetModuleNameFromRootModuleMID() {
        assertEquals("", ProjectMetadata.getModuleName(ROOT_MID));
    }

    @Test
    public void testGetProjectIdentifierForLevelOneModule() {
        assertEquals(LEVEL_ONE_MID,
                ProjectMetadata.getProjectIdentifier(LEVEL_ONE_MODULE));
    }

    @Test
    public void testGetProjectIdentifierForLevelTwoModule() {
        assertEquals(LEVEL_TWO_MID,
                ProjectMetadata.getProjectIdentifier(LEVEL_TWO_MODULE));
    }

    @Test
    public void testGetProjectIdentifierForRootModule() {
        assertEquals(ROOT_MID, ProjectMetadata.getProjectIdentifier(""));
    }

    @Test
    public void testInvalidMIDIsNotValid() {
        assertFalse(ProjectMetadata.isValid("MID:foo#bar?baz"));
    }

    @Test
    public void testLevelOneMIDIsValid() {
        assertTrue(ProjectMetadata.isValid(LEVEL_ONE_MID));
    }

    @Test
    public void testLevelTwoMIDIsValid() {
        assertTrue(ProjectMetadata.isValid(LEVEL_TWO_MID));
    }

    @Test
    public void testRootMIDIsValid() {
        assertTrue(ProjectMetadata.isValid(ROOT_MID));
    }
}
