package org.springframework.roo.project;

import static org.junit.Assert.assertEquals;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Node;

/**
 * Convenient superclass for XML-based JUnit 4 test cases.
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public abstract class XmlTestCase {

    /**
     * A builder for XML DOM documents.
     */
    protected static final DocumentBuilder DOCUMENT_BUILDER;

    static {
        try {
            DOCUMENT_BUILDER = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder();
        }
        catch (final ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Asserts that the given XML node contains the expected content
     * 
     * @param expectedLines the expected lines of XML (required); separate each
     *            line with "\n" regardless of the platform
     * @param actualNode the actual XML node (required)
     * @throws AssertionError if they are not equal
     */
    protected final void assertXmlEquals(final String expectedXml,
            final Node actualNode) {
        // Replace the dummy line terminator with the platform-specific one that
        // will be applied by XmlUtils.nodeToString.
        final String normalisedXml = expectedXml.replace("\n",
                IOUtils.LINE_SEPARATOR);
        // Trim trailing whitespace as XmlUtils.nodeToString appends an extra
        // newline.
        final String actualXml = StringUtils.stripEnd(
                XmlUtils.nodeToString(actualNode), null);
        assertEquals(normalisedXml, actualXml);
    }
}
