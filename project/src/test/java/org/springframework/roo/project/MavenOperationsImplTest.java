package org.springframework.roo.project;

import static org.apache.commons.io.IOUtils.LINE_SEPARATOR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.maven.Pom;

/**
 * Unit test of {@link MavenOperationsImpl}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class MavenOperationsImplTest {

    private static final String ARTIFACT_ID = "foo-lib";
    private static final String CLASSIFIER = "exe";
    private static final String GROUP_ID = "com.example";
    private static final String VERSION = "1.0.Final";
    private static final String POM_AFTER_DEPENDENCY_REMOVED = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
            + "<project>    <dependencies>\n"
            + "        <dependency>\n"
            + "            <groupId>com.example</groupId>\n"
            + "            <artifactId>test-lib</artifactId>\n"
            + "            <version>2.0.Final</version>\n"
            + "            <scope>test</scope>\n"
            + "        </dependency>\n"
            + "    </dependencies>\n" + "</project>\n";
    private static final String POM_BEFORE_DEPENDENCY_REMOVED = "<project>"
            + "    <dependencies>" + "        <dependency>"
            + "            <groupId>" + GROUP_ID + "</groupId>"
            + "            <artifactId>" + ARTIFACT_ID + "</artifactId>"
            + "            <version>" + VERSION + "</version>"
            + "            <scope>provided</scope>"
            + "            <classifier>" + CLASSIFIER + "</classifier>"
            + "        </dependency>" + "        <dependency>"
            + "            <groupId>com.example</groupId>"
            + "            <artifactId>test-lib</artifactId>"
            + "            <version>2.0.Final</version>"
            + "            <scope>test</scope>" + "        </dependency>"
            + "    </dependencies>" + "</project>";
    private static final String POM_PATH = "/any/old/path";
    private static final String SIMPLE_DESCRIPTION = "Foo Library";

    @Mock private FileManager mockFileManager;
    @Mock private MetadataService mockMetadataService;
    @Mock private PathResolver mockPathResolver;
    @Mock private PomManagementService mockPomManagementService;
    @Mock private ProjectMetadata mockProjectMetadata;

    // Fixture
    private MavenOperationsImpl projectOperations;

    private void assertModuleFocusAllowed(final boolean expectedResult,
            final String... moduleNames) {
        // Set up
        when(mockPomManagementService.getModuleNames()).thenReturn(
                Arrays.asList(moduleNames));

        // Invoke and check
        assertEquals(expectedResult, projectOperations.isModuleFocusAllowed());
    }

    @Before
    public void setUp() {
        // Mocks
        MockitoAnnotations.initMocks(this);

        when(
                mockPathResolver.getIdentifier(Path.ROOT.getModulePathId(""),
                        MavenProjectMetadataProvider.POM_RELATIVE_PATH))
                .thenReturn(POM_PATH);

        // Object under test
        projectOperations = new MavenOperationsImpl();
        projectOperations.fileManager = mockFileManager;
        projectOperations.metadataService = mockMetadataService;
        projectOperations.pathResolver = mockPathResolver;
        projectOperations.pomManagementService = mockPomManagementService;
    }

    @Test
    public void testCannotFocusModuleWhenMoreThanOneModuleExists() {
        assertModuleFocusAllowed(true, "", "core");
    }

    @Test
    public void testCannotFocusModuleWhenOneOrLessModulesExist() {
        assertModuleFocusAllowed(false, "");
    }

    @Test
    public void testGetFocusedModuleWhenChildModuleHasFocus() {
        // Set up
        when(mockPomManagementService.getFocusedModuleName()).thenReturn(
                "child");
        final Pom mockChildPom = mock(Pom.class);
        final ProjectMetadata mockChildMetadata = mock(ProjectMetadata.class);
        when(mockChildMetadata.getPom()).thenReturn(mockChildPom);
        when(
                mockMetadataService.get(ProjectMetadata
                        .getProjectIdentifier("child"))).thenReturn(
                mockChildMetadata);

        // Invoke and check
        assertEquals(mockChildPom, projectOperations.getFocusedModule());
    }

    @Test
    public void testGetFocusedModuleWhenNoModulesExist() {
        // Set up
        when(mockPomManagementService.getFocusedModuleName()).thenReturn("");
        when(mockMetadataService.get(ProjectMetadata.getProjectIdentifier("")))
                .thenReturn(null);

        // Invoke and check
        assertNull(projectOperations.getFocusedModule());
    }

    @Test
    public void testRemoveDependencyTwiceWhenItExistsOnce() {
        // Set up
        when(mockFileManager.getInputStream(POM_PATH)).thenReturn(
                new ByteArrayInputStream(POM_BEFORE_DEPENDENCY_REMOVED
                        .getBytes()));

        // -- Dependency to remove
        final Dependency mockDependency = mock(Dependency.class);
        when(mockDependency.getArtifactId()).thenReturn(ARTIFACT_ID);
        when(mockDependency.getClassifier()).thenReturn(CLASSIFIER);
        when(mockDependency.getGroupId()).thenReturn(GROUP_ID);
        when(mockDependency.getSimpleDescription()).thenReturn(
                SIMPLE_DESCRIPTION);
        when(mockDependency.getType()).thenReturn(DependencyType.JAR);
        when(mockDependency.getVersion()).thenReturn(VERSION);
        when(mockMetadataService.get(ProjectMetadata.getProjectIdentifier("")))
                .thenReturn(mockProjectMetadata);

        final Pom pom = mock(Pom.class);
        when(pom.getPath()).thenReturn(POM_PATH);
        when(mockProjectMetadata.getPom()).thenReturn(pom);

        final Collection<Dependency> dependencies = Arrays.asList(
                mockDependency, mockDependency);
        when(pom.isAnyDependenciesRegistered(dependencies)).thenReturn(true);
        when(pom.isDependencyRegistered(mockDependency)).thenReturn(true);

        // Invoke
        projectOperations.removeDependencies("", dependencies);

        // Check
        final String expectedPom = POM_AFTER_DEPENDENCY_REMOVED.replace("\n",
                LINE_SEPARATOR);
        verify(mockFileManager).createOrUpdateTextFileIfRequired(eq(POM_PATH),
                eq(expectedPom), (String) any(), eq(false));
    }
}
