package org.springframework.roo.project;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Element;

/**
 * Unit test of the {@link Resource} class
 *
 * @author Andrew Swan
 * @since 1.2.0
 */
public class ResourceTest {

	// Constants
	private static final boolean FILTERING = true;
	private static final String DIRECTORY = "anything";
	private static final String INCLUDE_1 = "include1";
	private static final String INCLUDE_2 = "include2";

	private static final String EXPECTED_XML =
		"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
		"<resource>\n" +
		"    <directory>" + DIRECTORY + "</directory>\n" +
		"    <filtering>" + FILTERING + "</filtering>\n" +
		"    <includes>\n" +
		"        <include>" + INCLUDE_1 + "</include>\n" +
		"        <include>" + INCLUDE_2 + "</include>\n" +
		"    </includes>\n" +
		"</resource>\n";
	
	private static final DocumentBuilder DOCUMENT_BUILDER;
	static {
		try {
			DOCUMENT_BUILDER = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (final ParserConfigurationException e) {
			throw new IllegalStateException(e);
		}
	}
	
	@Test
	public void testGetElement() {
		// Set up
		final Resource resource = new Resource(new Path(DIRECTORY), FILTERING, Arrays.asList(INCLUDE_1, INCLUDE_2));
		
		// Invoke
		final Element element = resource.getElement(DOCUMENT_BUILDER.newDocument());
		
		// Check
		assertEquals(EXPECTED_XML, XmlUtils.nodeToString(element));
	}
}
