package org.springframework.roo.project;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.w3c.dom.Element;

/**
 * Unit test of the {@link Dependency} class
 *
 * @author Andrew Swan
 * @since 1.2.0
 */
public class DependencyTest extends XmlTestCase {
	
	// Constants
	private static final String DEPENDENCY_GROUP_ID = "com.bar";
	private static final String DEPENDENCY_ARTIFACT_ID = "foo-api";
	private static final String DEPENDENCY_VERSION = "6.6.6";
	
	private static final String EXCLUSION_GROUP_ID = "com.ugliness";
	private static final String EXCLUSION_ARTIFACT_ID = "ugly-api";
	
	@Test
	public void testAddExclusion() {
		// Set up
		final Dependency dependency = new Dependency(DEPENDENCY_GROUP_ID, DEPENDENCY_ARTIFACT_ID, DEPENDENCY_VERSION);
		final int originalExclusionCount = dependency.getExclusions().size();
		
		// Invoke
		dependency.addExclusion(EXCLUSION_GROUP_ID, EXCLUSION_ARTIFACT_ID);
		
		// Check
		assertEquals(originalExclusionCount + 1, dependency.getExclusions().size());
	}
	
	private static final String EXPECTED_ELEMENT_FOR_MINIMAL_DEPENDENCY =
		"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
		"<dependency>\n" +
		"    <groupId>" + DEPENDENCY_GROUP_ID + "</groupId>\n" +
		"    <artifactId>" + DEPENDENCY_ARTIFACT_ID + "</artifactId>\n" +
		"    <version>" + DEPENDENCY_VERSION + "</version>\n" +
		"</dependency>";
	
	@Test
	public void testGetElementForMinimalDependency() {
		// Set up
		final Dependency dependency = new Dependency(DEPENDENCY_GROUP_ID, DEPENDENCY_ARTIFACT_ID, DEPENDENCY_VERSION);
		
		// Invoke
		final Element element = dependency.getElement(DOCUMENT_BUILDER.newDocument());
		
		// Check
		assertXmlEquals(EXPECTED_ELEMENT_FOR_MINIMAL_DEPENDENCY, element);
	}
	
	@Test
	public void testDependenciesWithDifferentVersionsAreNotEqual() {
		// Set up
		final Dependency dependency1 = new Dependency(DEPENDENCY_GROUP_ID, DEPENDENCY_ARTIFACT_ID, DEPENDENCY_VERSION);
		final Dependency dependency2 = new Dependency(dependency1.getGroupId(), dependency1.getArtifactId(), dependency1.getVersion() + "x");
		
		// Invoke
		final boolean equal = dependency1.equals(dependency2);
		
		// Check
		assertFalse(equal);
	}
	
	@Test
	public void testDependenciesWithSameVersionAreEqual() {
		// Set up
		final Dependency dependency1 = new Dependency(DEPENDENCY_GROUP_ID, DEPENDENCY_ARTIFACT_ID, DEPENDENCY_VERSION);
		final Dependency dependency2 = new Dependency(dependency1.getGroupId(), dependency1.getArtifactId(), dependency1.getVersion());
		
		// Invoke
		final boolean equal = dependency1.equals(dependency2);
		
		// Check
		assertTrue(equal);
	}
	
	@Test
	public void testDependenciesWithDifferentVersionsHaveSameCoordinates() {
		// Set up
		final Dependency dependency1 = new Dependency(DEPENDENCY_GROUP_ID, DEPENDENCY_ARTIFACT_ID, DEPENDENCY_VERSION);
		final Dependency dependency2 = new Dependency(dependency1.getGroupId(), dependency1.getArtifactId(), dependency1.getVersion() + "x");
		
		// Invoke
		final boolean same = dependency1.hasSameCoordinates(dependency2);
		
		// Check
		assertTrue(same);
	}
	
	@Test
	public void testDependenciesWithSameVersionHaveSameCoordinates() {
		// Set up
		final Dependency dependency1 = new Dependency(DEPENDENCY_GROUP_ID, DEPENDENCY_ARTIFACT_ID, DEPENDENCY_VERSION);
		final Dependency dependency2 = new Dependency(dependency1.getGroupId(), dependency1.getArtifactId(), dependency1.getVersion());
		
		// Invoke
		final boolean same = dependency1.hasSameCoordinates(dependency2);
		
		// Check
		assertTrue(same);
	}
	
	@Test
	public void testNullDependencyDoesNotHaveSameCoordinates() {
		// Set up
		final Dependency dependency = new Dependency(DEPENDENCY_GROUP_ID, DEPENDENCY_ARTIFACT_ID, DEPENDENCY_VERSION);
		
		// Invoke
		final boolean same = dependency.hasSameCoordinates(null);
		
		// Check
		assertFalse(same);
	}
}
