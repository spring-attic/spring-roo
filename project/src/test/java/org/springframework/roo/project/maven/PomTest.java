package org.springframework.roo.project.maven;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.roo.project.DependencyScope.COMPILE;
import static org.springframework.roo.project.Path.ROOT;
import static org.springframework.roo.project.maven.Pom.DEFAULT_PACKAGING;

import java.io.File;
import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.DependencyType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PhysicalPath;
import org.springframework.roo.support.util.FileUtils;

/**
 * Unit test of the {@link Pom} class
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class PomTest {

    private static final String ARTIFACT_ID = "my-app";
    private static final String DEPENDENCY_ARTIFACT_ID = "commons-foo";
    private static final String DEPENDENCY_GROUP_ID = "org.apache";
    private static final String GROUP_ID = "com.example";
    private static final String JAR = "jar";
    private static final String POM = "pom";
    private static final String PROJECT_ROOT = File.separator
            + FileUtils.getSystemDependentPath("users", "jbloggs", "projects",
                    "clinic");
    private static final String ROOT_MODULE = "";
    private static final String VERSION = "1.0.1.RELEASE";
    private static final String WAR = "war";

    private Pom getMinimalPom(final String packaging,
            final Dependency... dependencies) {
        return new Pom(GROUP_ID, ARTIFACT_ID, VERSION, packaging,
                Arrays.asList(dependencies), null, null, null, null, null,
                null, null, null, null, null, null, PROJECT_ROOT
                        + File.separator + "pom.xml", ROOT_MODULE, null);
    }

    private Dependency getMockDependency(final String groupId,
            final String artifactId, final String version,
            final DependencyType type) {
        final Dependency mockDependency = mock(Dependency.class);
        when(mockDependency.getGroupId()).thenReturn(groupId);
        when(mockDependency.getArtifactId()).thenReturn(artifactId);
        when(mockDependency.getVersion()).thenReturn(version);
        when(mockDependency.getType()).thenReturn(type);
        return mockDependency;
    }

    @Test
    public void testCanAddNewDependencyOfLowerType() {
        // Set up
        final Dependency mockNewDependency = mock(Dependency.class);
        when(mockNewDependency.getType()).thenReturn(DependencyType.JAR);
        final Pom pom = getMinimalPom(WAR);

        // Invoke and check
        assertTrue(pom.canAddDependency(mockNewDependency));
    }

    @Test
    public void testCanAddNewDependencyWhenOwnTypeIsNonStandard() {
        // Set up
        final Dependency mockNewDependency = mock(Dependency.class);
        when(mockNewDependency.getType()).thenReturn(DependencyType.WAR);
        final Pom pom = getMinimalPom("custom");

        // Invoke and check
        assertTrue(pom.canAddDependency(mockNewDependency));
    }

    @Test
    public void testCannotAddAlreadyRegisteredDependency() {
        // Set up
        final Dependency mockExistingDependency = mock(Dependency.class);
        when(mockExistingDependency.getType()).thenReturn(DependencyType.JAR);
        final Pom pom = getMinimalPom(POM, mockExistingDependency);

        // Invoke and check
        assertFalse(pom.canAddDependency(mockExistingDependency));
    }

    @Test
    public void testCannotAddNewDependencyOfHigherType() {
        // Set up
        final Dependency mockNewDependency = mock(Dependency.class);
        when(mockNewDependency.getType()).thenReturn(DependencyType.WAR);
        final Pom pom = getMinimalPom(JAR);

        // Invoke and check
        assertFalse(pom.canAddDependency(mockNewDependency));
    }

    @Test
    public void testCannotAddNullDependency() {
        // Set up
        final Pom pom = getMinimalPom(POM);

        // Invoke and check
        assertFalse(pom.canAddDependency(null));
    }

    @Test
    public void testDefaultPackaging() {
        assertEquals(DEFAULT_PACKAGING, getMinimalPom(DEFAULT_PACKAGING)
                .getPackaging());
    }

    @Test
    public void testGetAsDependency() {
        // Set up
        final Pom pom = getMinimalPom(WAR);

        // Invoke
        final Dependency dependency = pom.asDependency(COMPILE);

        // Check
        assertEquals(GROUP_ID, dependency.getGroupId());
        assertEquals(ARTIFACT_ID, dependency.getArtifactId());
        assertEquals(VERSION, dependency.getVersion());
        assertEquals(DependencyType.WAR, dependency.getType());
        assertEquals(COMPILE, dependency.getScope());
        assertTrue(StringUtils.isBlank(dependency.getClassifier()));
    }

    @Test
    public void testGetModulePathsForMinimalJarPom() {
        // Set up
        final Pom pom = getMinimalPom(DEFAULT_PACKAGING);
        final Path[] expectedPaths = { ROOT };

        // Invoke and check
        assertEquals(expectedPaths.length, pom.getPhysicalPaths().size());
        for (final Path path : expectedPaths) {
            final PhysicalPath modulePath = pom.getPhysicalPath(path);
            assertEquals(new File(PROJECT_ROOT, path.getDefaultLocation()),
                    modulePath.getLocation());
            assertEquals(path.isJavaSource(), modulePath.isSource());
            final LogicalPath moduelPathId = modulePath.getLogicalPath();
            assertEquals(path, moduelPathId.getPath());
            assertEquals(ROOT_MODULE, moduelPathId.getModule());
        }
    }

    @Test
    public void testHasDependencyExcludingVersionWhenDependencyHasDifferentGroupId() {
        // Set up
        final Dependency mockExistingDependency = getMockDependency(
                DEPENDENCY_GROUP_ID, DEPENDENCY_ARTIFACT_ID, "1.0",
                DependencyType.JAR);
        final Pom pom = getMinimalPom(JAR, mockExistingDependency);
        final Dependency mockOtherDependency = getMockDependency("au."
                + DEPENDENCY_GROUP_ID, DEPENDENCY_ARTIFACT_ID, "1.0",
                DependencyType.JAR);

        // Invoke and check
        assertFalse(pom.hasDependencyExcludingVersion(mockOtherDependency));
    }

    @Test
    public void testHasDependencyExcludingVersionWhenDependencyHasDifferentType() {
        // Set up
        final Dependency mockExistingDependency = getMockDependency(
                DEPENDENCY_GROUP_ID, DEPENDENCY_ARTIFACT_ID, "1.0",
                DependencyType.JAR);
        final Pom pom = getMinimalPom(JAR, mockExistingDependency);
        final Dependency mockOtherDependency = getMockDependency(
                DEPENDENCY_GROUP_ID, DEPENDENCY_ARTIFACT_ID, "1.0",
                DependencyType.OTHER);

        // Invoke and check
        assertFalse(pom.hasDependencyExcludingVersion(mockOtherDependency));
    }

    @Test
    public void testHasDependencyExcludingVersionWhenDependencyHasDifferentVersion() {
        // Set up
        final String existingVersion = "1.0";
        final Dependency mockExistingDependency = getMockDependency(
                DEPENDENCY_GROUP_ID, DEPENDENCY_ARTIFACT_ID, existingVersion,
                DependencyType.JAR);
        final Pom pom = getMinimalPom(JAR, mockExistingDependency);
        final Dependency mockOtherDependency = getMockDependency(
                DEPENDENCY_GROUP_ID, DEPENDENCY_ARTIFACT_ID, existingVersion
                        + ".1", DependencyType.JAR);

        // Invoke and check
        assertTrue(pom.hasDependencyExcludingVersion(mockOtherDependency));
    }

    @Test
    public void testHasDependencyExcludingVersionWhenDependencyIsNull() {
        // Set up
        final Pom pom = getMinimalPom(JAR);

        // Invoke and check
        assertFalse(pom.hasDependencyExcludingVersion(null));
    }
}
