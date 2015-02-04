package org.springframework.roo.project;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.support.util.FileUtils;

/**
 * Unit test of {@link MavenPathResolvingStrategy}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class MavenPathResolvingStrategyTest {

    private static final String NEW_MODULE = "new";
    private static final String PATH_RELATIVE_TO_POM = FileUtils
            .getSystemDependentPath("src", "main", "java");
    private static final String POM_PATH = File.separator
            + FileUtils.getSystemDependentPath("path", "to", "the", "pom");
    private static final String ROOT_MODULE = "";

    @Mock private LogicalPath mockNonSourcePath;
    @Mock private PomManagementService mockPomManagementService;
    @Mock private LogicalPath mockSourcePath;
    // Fixture
    private MavenPathResolvingStrategy strategy;

    /**
     * Asserts that calling
     * {@link MavenPathResolvingStrategy#getIdentifier(LogicalPath, String)}
     * with the given parameters results in the given expected identifier.
     * 
     * @param pom the POM that the mock {@link PomManagementService} should
     *            return for the given module name (can be <code>null</code>)
     * @param module the module to be returned by the {@link LogicalPath}
     * @param relativePath cannot be <code>null</code>
     * @param expectedIdentifier
     */
    private void assertIdentifier(final Pom pom, final String module,
            final String relativePath, final String expectedIdentifier) {
        // Set up
        final LogicalPath mockContextualPath = getMockContextualPath(module,
                pom);
        when(mockPomManagementService.getPomFromModuleName(module)).thenReturn(
                pom);

        // Invoke
        final String identifier = strategy.getIdentifier(mockContextualPath,
                relativePath);

        // Check
        assertEquals(expectedIdentifier, identifier.replaceFirst("[A-Z]:", ""));
    }

    private LogicalPath getMockContextualPath(final String module, final Pom pom) {
        final LogicalPath mockContextualPath = mock(LogicalPath.class);
        when(mockContextualPath.getModule()).thenReturn(module);
        when(mockContextualPath.getPathRelativeToPom(pom)).thenReturn(
                PATH_RELATIVE_TO_POM);
        return mockContextualPath;
    }

    private PhysicalPath getMockModulePath(final boolean isSource,
            final LogicalPath logicalPath) {
        final PhysicalPath mockModulePath = mock(PhysicalPath.class);
        when(mockModulePath.isSource()).thenReturn(isSource);
        when(mockModulePath.getLogicalPath()).thenReturn(logicalPath);
        return mockModulePath;
    }

    private Pom getMockPom(final String rootPath) {
        final Pom mockPom = mock(Pom.class);
        when(mockPom.getRoot()).thenReturn(rootPath);
        return mockPom;
    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        strategy = new MavenPathResolvingStrategy();
        strategy.pomManagementService = mockPomManagementService;
    }

    private void setUpModulePaths() {
        final PhysicalPath mockModuleSourcePath = getMockModulePath(true,
                mockSourcePath);
        final PhysicalPath mockModuleNonSourcePath = getMockModulePath(false,
                mockNonSourcePath);
        final Pom mockPom = mock(Pom.class);
        when(mockPom.getPhysicalPaths()).thenReturn(
                Arrays.asList(mockModuleSourcePath, mockModuleNonSourcePath));
        when(mockPomManagementService.getPoms()).thenReturn(
                Arrays.asList(mockPom));
    }

    @Test
    public void testGetAllPaths() {
        // Set up
        setUpModulePaths();

        // Invoke
        final Object modulePathIds = strategy.getPaths();

        // Check
        assertEquals(Arrays.asList(mockSourcePath, mockNonSourcePath),
                modulePathIds);
    }

    @Test
    public void testGetIdentifierForNewModuleWithEmptyRelativePath() {
        final Pom mockParentPom = getMockPom(POM_PATH);
        when(mockPomManagementService.getFocusedModule()).thenReturn(
                mockParentPom);
        final String expectedIdentifier = FileUtils.getSystemDependentPath(
                POM_PATH, NEW_MODULE, PATH_RELATIVE_TO_POM) + File.separator;
        assertIdentifier(null, NEW_MODULE, "", expectedIdentifier);
    }

    @Test
    public void testGetIdentifierForRootModuleWithEmptyRelativePath() {
        final String expectedIdentifier = FileUtils.getSystemDependentPath(
                POM_PATH, PATH_RELATIVE_TO_POM) + File.separator;
        assertIdentifier(getMockPom(POM_PATH), ROOT_MODULE, "",
                expectedIdentifier);
    }

    @Test
    public void testGetIdentifierForRootModuleWithNonEmptyRelativePath() {
        final String relativePath = FileUtils.getSystemDependentPath("com",
                "example", "domain", "PersonTest.java");
        final String expectedIdentifier = FileUtils.getSystemDependentPath(
                POM_PATH, PATH_RELATIVE_TO_POM, relativePath);
        assertIdentifier(getMockPom(POM_PATH), ROOT_MODULE, relativePath,
                expectedIdentifier);
    }

    @Test
    public void testGetSourcePaths() {
        // Set up
        setUpModulePaths();

        // Invoke
        final Object modulePathIds = strategy.getSourcePaths();

        // Check
        assertEquals(Arrays.asList(mockSourcePath), modulePathIds);
    }
}
