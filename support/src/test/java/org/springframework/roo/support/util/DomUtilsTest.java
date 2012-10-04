package org.springframework.roo.support.util;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Unit test of {@link DomUtils}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class DomUtilsTest {

    private static final String DEFAULT_TEXT = "foo";
    private static final String NODE_TEXT = "bar";
    private static final String XML_AFTER_REMOVAL = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><top><middle/></top>";
    private static final String XML_BEFORE_REMOVAL = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><top><middle><bottom id=\"1\" /><bottom id=\"2\" /></middle></top>";

    @Test
    public void testGetTextContentOfNonNullNode() {
        // Set up
        final Node mockNode = mock(Node.class);
        when(mockNode.getTextContent()).thenReturn(NODE_TEXT);

        assertEquals(NODE_TEXT, DomUtils.getTextContent(mockNode, DEFAULT_TEXT));
    }

    @Test
    public void testGetTextContentOfNullNode() {
        assertEquals(DEFAULT_TEXT, DomUtils.getTextContent(null, DEFAULT_TEXT));
    }

    @Test
    public void testRemoveElements() throws Exception {
        // Set up
        final Element root = XmlUtils.stringToElement(XML_BEFORE_REMOVAL);
        final Element middle = DomUtils
                .getChildElementByTagName(root, "middle");

        // Invoke
        DomUtils.removeElements("bottom", middle);

        // Check
        assertEquals(XmlUtils.nodeToString(XmlUtils
                .stringToElement(XML_AFTER_REMOVAL)),
                XmlUtils.nodeToString(root));
    }
}
