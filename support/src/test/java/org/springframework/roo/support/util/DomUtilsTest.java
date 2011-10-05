package org.springframework.roo.support.util;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.w3c.dom.Node;

/**
 * Unit test of {@link DomUtils}
 *
 * @author Andrew Swan
 * @since 1.2.0
 */
public class DomUtilsTest {

	// Constants
	private static final String DEFAULT_TEXT = "foo";
	private static final String NODE_TEXT = "bar";

	@Test
	public void testGetTextContentOfNullNode() {
		assertEquals(DEFAULT_TEXT, DomUtils.getTextContent(null, DEFAULT_TEXT));
	}

	@Test
	public void testGetTextContentOfNonNullNode() {
		// Set up
		final Node mockNode = mock(Node.class);
		when(mockNode.getTextContent()).thenReturn(NODE_TEXT);

		assertEquals(NODE_TEXT, DomUtils.getTextContent(mockNode, DEFAULT_TEXT));
	}
}
