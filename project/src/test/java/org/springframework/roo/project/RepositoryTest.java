package org.springframework.roo.project;

import static org.junit.Assert.assertEquals;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Element;

/**
 * Unit test of the {@link Repository} class
 *
 * @author Andrew Swan
 * @since 1.2.0
 */
public class RepositoryTest {

	// Constants
	private static final boolean ENABLE_SNAPSHOTS = true;
	private static final String ID = "the-id";
	private static final String NAME = "the_name";
	private static final String URL = "the-url";
	private static final String PATH = "pluginRepo";
	
	private static final DocumentBuilder DOCUMENT_BUILDER;
	static {
		try {
			DOCUMENT_BUILDER = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (final ParserConfigurationException e) {
			throw new IllegalStateException(e);
		}
	}
	
	private static final String EXPECTED_XML =
		"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
		"<pluginRepo>\n" +
		"    <id>the-id</id>\n" +
		"    <url>the-url</url>\n" +
		"    <name>the_name</name>\n" +
		"    <snapshots>\n" +
		"        <enabled>true</enabled>\n" +
		"    </snapshots>\n" +
		"</pluginRepo>\n";
	
	@Test
	public void testGetElement() {
		// Set up
		final Repository repository = new Repository(ID, NAME, URL, ENABLE_SNAPSHOTS);
		
		// Invoke
		final Element element = repository.getElement(DOCUMENT_BUILDER.newDocument(), PATH);
		
		// Check
		assertEquals(EXPECTED_XML, XmlUtils.nodeToString(element));
	}
}
