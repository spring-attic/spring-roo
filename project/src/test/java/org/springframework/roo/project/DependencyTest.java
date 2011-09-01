package org.springframework.roo.project;

import static org.junit.Assert.assertEquals;

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
}
