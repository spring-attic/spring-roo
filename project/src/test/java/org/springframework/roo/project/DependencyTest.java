package org.springframework.roo.project;

import static org.junit.Assert.assertEquals;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Element;

/**
 * Unit test of the {@link Dependency} class
 *
 * @author Andrew Swan
 * @since 1.2.0
 */
public class DependencyTest {
	
	// Constants
	private static final String DEPENDENCY_GROUP_ID = "com.bar";
	private static final String DEPENDENCY_ARTIFACT_ID = "foo-api";
	private static final String DEPENDENCY_VERSION = "6.6.6";
	
	private static final String EXCLUSION_GROUP_ID = "com.ugliness";
	private static final String EXCLUSION_ARTIFACT_ID = "ugly-api";
	
	private static final DocumentBuilder DOCUMENT_BUILDER;
	static {
		try {
			DOCUMENT_BUILDER = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (final ParserConfigurationException e) {
			throw new IllegalStateException(e);
		}
	}
	
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
		"</dependency>\n";
	
	@Test
	public void testGetElementForMinimalDependency() {
		// Set up
		final Dependency dependency = new Dependency(DEPENDENCY_GROUP_ID, DEPENDENCY_ARTIFACT_ID, DEPENDENCY_VERSION);
		
		// Invoke
		final Element element = dependency.getElement(DOCUMENT_BUILDER.newDocument());
		
		// Check
		assertEquals(EXPECTED_ELEMENT_FOR_MINIMAL_DEPENDENCY, XmlUtils.nodeToString(element));
	}
}
