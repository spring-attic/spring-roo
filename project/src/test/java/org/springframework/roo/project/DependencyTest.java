package org.springframework.roo.project;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.roo.project.DependencyScope.PROVIDED;
import static org.springframework.roo.project.DependencyType.ZIP;

import org.junit.Test;
import org.w3c.dom.Element;

/**
 * Unit test of the {@link Dependency} class
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class DependencyTest extends XmlTestCase {

    private static final String DEPENDENCY_ARTIFACT_ID = "foo-api";
    private static final String DEPENDENCY_GROUP_ID = "com.bar";
    private static final String DEPENDENCY_VERSION = "6.6.6";

    private static final String EXCLUSION_ARTIFACT_ID = "ugly-api";
    private static final String EXCLUSION_GROUP_ID = "com.ugliness";

    private static final String EXPECTED_ELEMENT_FOR_MINIMAL_DEPENDENCY = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<dependency>\n"
            + "    <groupId>"
            + DEPENDENCY_GROUP_ID
            + "</groupId>\n"
            + "    <artifactId>"
            + DEPENDENCY_ARTIFACT_ID
            + "</artifactId>\n"
            + "    <version>"
            + DEPENDENCY_VERSION
            + "</version>\n" + "</dependency>";

    @Test
    public void testAddExclusion() {
        // Set up
        final Dependency dependency = new Dependency(DEPENDENCY_GROUP_ID,
                DEPENDENCY_ARTIFACT_ID, DEPENDENCY_VERSION);
        final int originalExclusionCount = dependency.getExclusions().size();

        // Invoke
        dependency.addExclusion(EXCLUSION_GROUP_ID, EXCLUSION_ARTIFACT_ID);

        // Check
        assertEquals(originalExclusionCount + 1, dependency.getExclusions()
                .size());
    }

    @Test
    public void testConstructFromGav() {
        // Set up
        final GAV mockGav = mock(GAV.class);
        when(mockGav.getGroupId()).thenReturn(DEPENDENCY_GROUP_ID);
        when(mockGav.getArtifactId()).thenReturn(DEPENDENCY_ARTIFACT_ID);
        when(mockGav.getVersion()).thenReturn(DEPENDENCY_VERSION);

        // Invoke
        final Dependency dependency = new Dependency(mockGav,
                DependencyType.ZIP, DependencyScope.SYSTEM);

        // Check
        assertEquals(DEPENDENCY_GROUP_ID, dependency.getGroupId());
        assertEquals(DEPENDENCY_ARTIFACT_ID, dependency.getArtifactId());
        assertEquals(DEPENDENCY_VERSION, dependency.getVersion());
        assertEquals(DependencyScope.SYSTEM, dependency.getScope());
        assertEquals(DependencyType.ZIP, dependency.getType());
    }

    @Test
    public void testConstructWithCustomTypeAndScope() {
        // Set up
        final Dependency dependency = new Dependency(DEPENDENCY_GROUP_ID,
                DEPENDENCY_ARTIFACT_ID, DEPENDENCY_VERSION, ZIP, PROVIDED);

        // Invoke and check
        assertEquals(ZIP, dependency.getType());
        assertEquals(PROVIDED, dependency.getScope());
    }

    @Test
    public void testDependenciesWithDifferentVersionsAreNotEqual() {
        // Set up
        final Dependency dependency1 = new Dependency(DEPENDENCY_GROUP_ID,
                DEPENDENCY_ARTIFACT_ID, DEPENDENCY_VERSION);
        final Dependency dependency2 = new Dependency(dependency1.getGroupId(),
                dependency1.getArtifactId(), dependency1.getVersion() + "x");

        // Invoke
        final boolean equal = dependency1.equals(dependency2);

        // Check
        assertFalse(equal);
    }

    @Test
    public void testDependenciesWithDifferentVersionsHaveSameCoordinates() {
        // Set up
        final Dependency dependency1 = new Dependency(DEPENDENCY_GROUP_ID,
                DEPENDENCY_ARTIFACT_ID, DEPENDENCY_VERSION);
        final Dependency dependency2 = new Dependency(dependency1.getGroupId(),
                dependency1.getArtifactId(), dependency1.getVersion() + "x");

        // Invoke
        final boolean same = dependency1.hasSameCoordinates(dependency2);

        // Check
        assertTrue(same);
    }

    @Test
    public void testDependenciesWithSameVersionAreEqual() {
        // Set up
        final Dependency dependency1 = new Dependency(DEPENDENCY_GROUP_ID,
                DEPENDENCY_ARTIFACT_ID, DEPENDENCY_VERSION);
        final Dependency dependency2 = new Dependency(dependency1.getGroupId(),
                dependency1.getArtifactId(), dependency1.getVersion());

        // Invoke
        final boolean equal = dependency1.equals(dependency2);

        // Check
        assertTrue(equal);
    }

    @Test
    public void testDependenciesWithSameVersionHaveSameCoordinates() {
        // Set up
        final Dependency dependency1 = new Dependency(DEPENDENCY_GROUP_ID,
                DEPENDENCY_ARTIFACT_ID, DEPENDENCY_VERSION);
        final Dependency dependency2 = new Dependency(dependency1.getGroupId(),
                dependency1.getArtifactId(), dependency1.getVersion());

        // Invoke
        final boolean same = dependency1.hasSameCoordinates(dependency2);

        // Check
        assertTrue(same);
    }

    @Test
    public void testEarIsHigherThanJar() {
        assertTrue(Dependency.isHigherLevel("ear", "jar"));
    }

    @Test
    public void testEarIsHigherThanWar() {
        assertTrue(Dependency.isHigherLevel("ear", "war"));
    }

    @Test
    public void testGetElementForMinimalDependency() {
        // Set up
        final Dependency dependency = new Dependency(DEPENDENCY_GROUP_ID,
                DEPENDENCY_ARTIFACT_ID, DEPENDENCY_VERSION);

        // Invoke
        final Element element = dependency.getElement(DOCUMENT_BUILDER
                .newDocument());

        // Check
        assertXmlEquals(EXPECTED_ELEMENT_FOR_MINIMAL_DEPENDENCY, element);
    }

    @Test
    public void testJarIsNotHigherThanItself() {
        assertFalse(Dependency.isHigherLevel("jar", "jar"));
    }

    @Test
    public void testJarIsNotHigherThanWar() {
        assertFalse(Dependency.isHigherLevel("jar", "war"));
    }

    @Test
    public void testNullDependencyDoesNotHaveSameCoordinates() {
        // Set up
        final Dependency dependency = new Dependency(DEPENDENCY_GROUP_ID,
                DEPENDENCY_ARTIFACT_ID, DEPENDENCY_VERSION);

        // Invoke
        final boolean same = dependency.hasSameCoordinates(null);

        // Check
        assertFalse(same);
    }

    @Test
    public void testPomIsHigherThanWar() {
        assertTrue(Dependency.isHigherLevel("ear", "war"));
    }

    @Test
    public void testWarIsHigherThanJar() {
        assertTrue(Dependency.isHigherLevel("war", "jar"));
    }
}
