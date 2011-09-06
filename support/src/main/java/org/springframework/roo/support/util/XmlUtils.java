package org.springframework.roo.support.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSException;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.SAXException;

/**
 * Utilities related to DOM and XML usage.
 * 
 * @author Stefan Schmidt
 * @author Ben Alex
 * @author Alan Stewart
 * @author Andrew Swan
 * @since 1.0
 */
public final class XmlUtils {
	
	// Constants
	private static final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	private static final Map<String, XPathExpression> compiledExpressionCache = new HashMap<String, XPathExpression>();
	private static final TransformerFactory transformerFactory = TransformerFactory.newInstance();
	private static final XPath xpath = XPathFactory.newInstance().newXPath();
	
	/**
	 * Returns the given XML as the root {@link Element} of a new {@link Document}
	 * 
	 * @param xml the XML to convert; can be blank
	 * @return <code>null</code> if the given XML is blank
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @since 1.2.0
	 */
	public static Element stringToElement(final String xml) throws IOException, ParserConfigurationException, SAXException {
		if (StringUtils.hasText(xml)) {
			return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes())).getDocumentElement();
		}
		return null;
	}
	
	/**
	 * Creates an {@link Element} containing the given text
	 * 
	 * @param document the document to contain the new element
	 * @param tagName the element's tag name (required)
	 * @param text the text to set; can be <code>null</code> for none
	 * @return a non-<code>null</code> element
	 * @since 1.2.0
	 */
	public static final Element createTextElement(final Document document, final String tagName, final String text) {
		final Element element = document.createElement(tagName);
		element.setTextContent(text);
		return element;
	}

	/**
	 * Read an XML document from the supplied input stream and return a document.
	 *  
	 * @param inputStream the input stream to read from (required).  The stream is closed upon completion.
	 * @return a document.
	 */
	public static final Document readXml(InputStream inputStream) {
		Assert.notNull(inputStream, "InputStream required");
		try {
			if (!(inputStream instanceof BufferedInputStream)) {
				inputStream = new BufferedInputStream(inputStream);
			}
			return factory.newDocumentBuilder().parse(inputStream);
		} catch (final Exception e) {
			throw new IllegalStateException("Could not open input stream", e);
		} finally {
			try {
				inputStream.close();
			} catch (final IOException ignored) {}
		}
	}
	
	/**
	 * Write an XML document to the OutputStream provided. This will use the pre-configured Roo provided Transformer.
	 * 
	 * @param outputStream the output stream to write to. The stream is closed upon completion.
	 * @param document the document to write.
	 */
	public static final void writeXml(final OutputStream outputStream, final Document document) {
		writeXml(createIndentingTransformer(), outputStream, document);
	}

	/**
	 * Write an XML document to the OutputStream provided. This will use the provided Transformer.
	 * 
	 * @param transformer the transformer (can be obtained from XmlUtils.createIndentingTransformer())
	 * @param outputStream the output stream to write to. The stream is closed upon completion.
	 * @param document the document to write.
	 */
	public static final void writeXml(final Transformer transformer, OutputStream outputStream, final Document document) {
		Assert.notNull(transformer, "Transformer required");
		Assert.notNull(outputStream, "OutputStream required");
		Assert.notNull(document, "Document required");
		
		transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		try {
			if (!(outputStream instanceof BufferedOutputStream)) {
				outputStream = new BufferedOutputStream(outputStream);
			}
			final StreamResult streamResult = createUnixStreamResultForEntry(outputStream);
			transformer.transform(new DOMSource(document), streamResult);
		} catch (final Exception e) {
			throw new IllegalStateException(e);
		} finally {
			try {
				outputStream.close();
			} catch (final IOException ignored) {
				// Do nothing
			}
		}
	}
	
	/**
	 * Write an XML document to the OutputStream provided. This method will detect if the JDK supports the
	 * DOM Level 3 "format-pretty-print" configuration and make use of it. If not found it will fall back to 
	 * using formatting offered by TrAX. 
	 * 
	 * @param outputStream the output stream to write to. The stream is closed upon completion.
	 * @param document the document to write.
	 */
	public static void writeFormattedXml(final OutputStream outputStream, final Document document) {
		// Note that the "format-pretty-print" DOM configuration parameter can only be set in JDK 1.6+.
		final DOMImplementation domImplementation = document.getImplementation();
		if (domImplementation.hasFeature("LS", "3.0") && domImplementation.hasFeature("Core", "2.0")) {
			DOMImplementationLS domImplementationLS = null;
			try {
				domImplementationLS = (DOMImplementationLS) domImplementation.getFeature("LS", "3.0");
			} catch (final NoSuchMethodError nsme) {
				// Fall back to default LS
				DOMImplementationRegistry registry = null;
				try {
					registry = DOMImplementationRegistry.newInstance();
				} catch (final Exception e) {
					// DOMImplementationRegistry not available. Falling back to TrAX.
					writeXml(outputStream, document);
					return;
				}
				if (registry != null) {
					domImplementationLS = (DOMImplementationLS) registry.getDOMImplementation("LS");
				} else {
					// DOMImplementationRegistry not available. Falling back to TrAX.
					writeXml(outputStream, document);
				}
			}
			if (domImplementationLS != null) {
				final LSSerializer lsSerializer = domImplementationLS.createLSSerializer();
				final DOMConfiguration domConfiguration = lsSerializer.getDomConfig();
				if (domConfiguration.canSetParameter("format-pretty-print", Boolean.TRUE)) {
					lsSerializer.getDomConfig().setParameter("format-pretty-print", Boolean.TRUE);
					final LSOutput lsOutput = domImplementationLS.createLSOutput();
					lsOutput.setEncoding("UTF-8");
					lsOutput.setByteStream(outputStream);
					try {
					lsSerializer.write(document, lsOutput);
					} catch (final LSException lse) {
						throw new IllegalStateException(lse);
					} finally {
						try {
							outputStream.close();
						} catch (final IOException ignored) {
							// Do nothing
						}
					}
				} else {
					// DOMConfiguration 'format-pretty-print' parameter not available. Falling back to TrAX.
					writeXml(outputStream, document);
				}
			} else {
				// DOMImplementationLS not available. Falling back to TrAX.
				writeXml(outputStream, document);
			}
		} else {
			// DOM 3.0 LS and/or DOM 2.0 Core not supported. Falling back to TrAX.
			writeXml(outputStream, document);
		}
	}
	
	/**
	 * Compares two DOM {@link Node nodes} by comparing the representations of the nodes as XML strings
	 *
	 * @param node1 the first node
	 * @param node2 the second node
	 * @return true if the XML representation node1 is the same as the XML representation of node2, otherwise false
	 */
	public static boolean compareNodes(Node node1, Node node2) {
		Assert.notNull(node1, "First node required");
		Assert.notNull(node2, "Second node required");
		// The documents need to be cloned as normalization has side-effects
		node1 = node1.cloneNode(true);
		node2 = node2.cloneNode(true);
		// The documents need to be normalized before comparison takes place to remove any formatting that interfere with comparison
		if (node1 instanceof Document && node2 instanceof Document) {
			((Document) node1).normalizeDocument();
			((Document) node2).normalizeDocument();
		} else {
			node1.normalize();
			node2.normalize();
		}
		return nodeToString(node1).equals(nodeToString(node2));
	}

	/**
	 * Converts a {@link Node node} to an XML string
	 *
	 * @param node the first element
	 * @return the XML String representation of the node, never null
	 */
	public static String nodeToString(final Node node) {
		try {
			final StringWriter writer = new StringWriter();
			createIndentingTransformer().transform(new DOMSource(node), new StreamResult(writer));
			return writer.toString();
		} catch (final TransformerException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Creates a {@link StreamResult} by wrapping the given outputStream in an
	 * {@link OutputStreamWriter} that transforms Windows line endings (\r\n) 
	 * into Unix line endings (\n) on Windows for consistency with Roo's templates.  
	 * 
	 * @param outputStream
	 * @return StreamResult 
	 * @throws UnsupportedEncodingException 
	 */
	private static StreamResult createUnixStreamResultForEntry(final OutputStream outputStream) throws UnsupportedEncodingException {
		final Writer writer;
		if (System.getProperty("line.separator").equals("\r\n")) {
			writer = new OutputStreamWriter(outputStream, "ISO-8859-1") {
				@Override
				public void write(final char[] cbuf, final int off, final int len) throws IOException {
					for (int i = off; i < off + len; i++) {
						if (cbuf[i] != '\r' || (i < cbuf.length - 1 && cbuf[i + 1] != '\n')) {
							super.write(cbuf[i]);
						}
					}
				}

				@Override
				public void write(final int c) throws IOException {
					if (c != '\r') super.write(c);
				}
				
				@Override
				public void write(final String str, final int off, final int len) throws IOException {
					final String orig = str.substring(off, off + len);
					final String filtered = orig.replace("\r\n", "\n");
					final int lengthDiff = orig.length() - filtered.length();
					if (filtered.endsWith("\r")) {
						super.write(filtered.substring(0, filtered.length() - 1), 0, len - lengthDiff - 1);
					} else {
						super.write(filtered, 0, len - lengthDiff);
					}
				}
			};
		} else {
			writer = new OutputStreamWriter(outputStream, "ISO-8859-1");
		}
		return new StreamResult(writer);
	}

	/**
	 * Searches the given parent element for a child element matching the given
	 * XPath expression.
	 * 
	 * Please note that the XPath parser used is NOT namespace aware. So if you
	 * want to find an element <code>&lt;beans&gt;&lt;sec:http&gt;</code>, you
	 * need to use the following XPath expression '/beans/http'.
	 * 
	 * @param xPathExpression the xPathExpression (required)
	 * @param parent the parent DOM element (required)
	 * @return the Element if discovered (null if no such {@link Element} found)
	 */
	public static Element findFirstElement(final String xPathExpression, final Node parent) {
		final Node node = findNode(xPathExpression, parent);
		if (node instanceof Element) {
			return (Element) node;
		}
		return null;
	}
	
	/**
	 * Checks in under a given root element whether it can find a child node
	 * which matches the XPath expression supplied. Returns {@link Node} if
	 * exists.
	 * 
	 * Please note that the XPath parser used is NOT namespace aware. So if you
	 * want to find a element <code>&lt;beans&gt;&lt;sec:http&gt;</code>, you
	 * need to use the XPath expression '<code>/beans/http</code>'.
	 * 
	 * @param xPathExpression the XPath expression (required)
	 * @param root the parent DOM element (required)
	 * @return the Node if discovered (null if not found)
	 */
	public static Node findNode(final String xPathExpression, final Node root) {
		Assert.hasText(xPathExpression, "XPath expression required");
		Assert.notNull(root, "Root element required");
		Node node = null;
		try {
			XPathExpression expr = compiledExpressionCache.get(xPathExpression);
			if (expr == null) {
				expr = xpath.compile(xPathExpression);
				compiledExpressionCache.put(xPathExpression, expr);
			}
			node = (Node) expr.evaluate(root, XPathConstants.NODE);
		} catch (final XPathExpressionException e) {
			throw new IllegalArgumentException("Unable evaluate XPath expression '" + xPathExpression + "'", e);
		}
		return node;
	}

	/**
	 * Checks in under a given root element whether it can find a child element
	 * which matches the name supplied. Returns {@link Element} if exists.
	 * 
	 * @param name the Element name (required)
	 * @param root the parent DOM element (required)
	 * @return the Element if discovered
	 */
	public static Element findFirstElementByName(final String name, final Element root) {
		Assert.hasText(name, "Element name required");
		Assert.notNull(root, "Root element required");
		return (Element) root.getElementsByTagName(name).item(0);
	}

	/**
	 * Checks in under a given root element whether it can find a child element
	 * which matches the XPath expression supplied. The {@link Element} must
	 * exist. Returns {@link Element} if exists.
	 * 
	 * Please note that the XPath parser used is NOT namespace aware. So if you
	 * want to find a element <beans><sec:http> you need to use the following
	 * XPath expression '/beans/http'.
	 * 
	 * @param xPathExpression the XPath expression (required)
	 * @param root the parent DOM element (required)
	 * @return the Element if discovered (never null; an exception is thrown if cannot be found)
	 */
	public static Element findRequiredElement(final String xPathExpression, final Element root) {
		Assert.hasText(xPathExpression, "XPath expression required");
		Assert.notNull(root, "Root element required");
		final Element element = findFirstElement(xPathExpression, root);
		Assert.notNull(element, "Unable to obtain required element '" + xPathExpression + "' from element '" + root + "'");
		return element;
	}

	/**
	 * Checks in under a given root element whether it can find a child elements
	 * which match the XPath expression supplied. Returns a {@link List} of
	 * {@link Element} if they exist.
	 * 
	 * Please note that the XPath parser used is NOT namespace aware. So if you
	 * want to find a element <beans><sec:http> you need to use the following
	 * XPath expression '/beans/http'.
	 * 
	 * @param xPathExpression the xPathExpression
	 * @param root the parent DOM element
	 * @return a {@link List} of type {@link Element} if discovered, otherwise an empty list (never null)
	 */
	public static List<Element> findElements(final String xPathExpression, final Element root) {
		final List<Element> elements = new ArrayList<Element>();
		NodeList nodes = null;

		try {
			XPathExpression expr = compiledExpressionCache.get(xPathExpression);
			if (expr == null) {
				expr = xpath.compile(xPathExpression);
				compiledExpressionCache.put(xPathExpression, expr);
			}
			nodes = (NodeList) expr.evaluate(root, XPathConstants.NODESET);
		} catch (final XPathExpressionException e) {
			throw new IllegalArgumentException("Unable evaluate xpath expression", e);
		}

		for (int i = 0, n = nodes.getLength(); i < n; i++) {
			elements.add((Element) nodes.item(i));
		}
		return elements;
	}
	
	/**
	 * Checks for a given element whether it can find an attribute which matches the 
	 * XPath expression supplied. Returns {@link Node} if exists.
	 * 
	 * @param xPathExpression the xPathExpression (required)
	 * @param element (required)
	 * @return the Node if discovered (null if not found)
	 */
	public static Node findFirstAttribute(final String xPathExpression, final Element element) {
		Node attr = null;
		try {
			XPathExpression expr = compiledExpressionCache.get(xPathExpression);
			if (expr == null) {
				expr = xpath.compile(xPathExpression);
				compiledExpressionCache.put(xPathExpression, expr);
			}
			attr = (Node) expr.evaluate(element, XPathConstants.NODE);
		} catch (final XPathExpressionException e) {
			throw new IllegalArgumentException("Unable evaluate xpath expression", e);
		}
		return attr;
	}

	/**
	 * @return a transformer that indents entries by 4 characters (never null)
	 */
	public static final Transformer createIndentingTransformer() {
		Transformer transformer;
		try {
			transformerFactory.setAttribute("indent-number", 4);
			transformer = transformerFactory.newTransformer();
		} catch (final Exception e) {
			throw new IllegalStateException(e);
		}
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
		return transformer;
	}

	/**
	 * @return a new document builder (never null)
	 */
	public static final DocumentBuilder getDocumentBuilder() {
		// factory.setNamespaceAware(true);
		try {
			return factory.newDocumentBuilder();
		} catch (final ParserConfigurationException e) {
			throw new IllegalStateException(e);
		}
	}
	
	/**
	 * Removes empty text nodes from the specified node
	 * 
	 * @param node the element where empty text nodes will be removed
	 */
	public static void removeTextNodes(final Node node) {
		if (node == null) {
			return;
		}
		
		final NodeList children = node.getChildNodes();
		for (int i = children.getLength() - 1; i >= 0; i--) {
			final Node child = children.item(i);
			switch (child.getNodeType()) {
				case Node.ELEMENT_NODE:
					removeTextNodes(child);
					break;
				case Node.CDATA_SECTION_NODE:
				case Node.TEXT_NODE:
					if (!StringUtils.hasText(child.getNodeValue())) {
						node.removeChild(child);
					}
					break;
			}
		}
	}
	
	/**
	 * Returns the root element of an addon's configuration file.
	 * 
	 * @param clazz which owns the configuration
	 * @param configurationPath the path of the configuration file.
	 * @return the configuration root element 
	 */
	public static Element getConfiguration(final Class<?> clazz, final String configurationPath) {
		final InputStream templateInputStream = TemplateUtils.getTemplate(clazz, configurationPath);
		Assert.notNull(templateInputStream, "Could not acquire " + configurationPath + " file");
		Document configurationDocument;
		try {
			configurationDocument = getDocumentBuilder().parse(templateInputStream);
		} catch (final Exception e) {
			throw new IllegalStateException(e);
		}
		return configurationDocument.getDocumentElement();
	}
	
	/**
	 * Returns the root element of an addon's configuration file.
	 * 
	 * @param clazz which owns the configuration
	 * @return the configuration root element 
	 */
	public static Element getConfiguration(final Class<?> clazz) {
		return getConfiguration(clazz, "configuration.xml");
	}
	
	/**
	 * Converts a XHTML compliant id (used in jspx) to a CSS3 selector spec compliant id. In that
	 * it will replace all '.,:,-' to '_'
	 * 
	 * @param proposed Id
	 * @return cleaned up Id
	 */
	public static String convertId(final String proposed) {
		return proposed.replaceAll("[:\\.-]", "_");
	}
	
	/**
	 * Checks the presented element for illegal characters that could cause malformed XML.
	 * 
	 * @param element the content of the XML element
	 * @throws IllegalArgumentException if the element is null, has no text or contains illegal characters 
	 */
	public static void assertElementLegal(final String element) {
		if (!StringUtils.hasText(element)) {
			throw new IllegalArgumentException("Element required");
		}
		
		// Note regular expression for legal characters found to be x5 slower in profiling than this approach
		final char[] value = element.toCharArray();
		for (int i = 0; i < value.length; i++) {
			final char c = value[i];
			if (' ' == c || '*' == c || '>' == c || '<' == c || '!' == c || '@' == c || '%' == c || '^' == c ||
				'?' == c || '(' == c || ')' == c || '~' == c || '`' == c || '{' == c || '}' == c || '[' == c || ']' == c ||
				'|' == c || '\\' == c || '\'' == c || '+' == c)  {
				throw new IllegalArgumentException("Illegal name '" + element + "' (illegal character)");
			}
		}
	}
	
	/**
	 * Returns the text content of the given {@link Node}, null safe
	 * 
	 * @param node can be <code>null</code>
	 * @param defaultValue the value to return if the node is <code>null</code>
	 * @return the given default value if the node is <code>null</code>
	 * @see Node#getTextContent()
	 * @since 1.2.0
	 */
	public static String getTextContent(final Node node, final String defaultValue) {
		if (node == null) {
			return defaultValue;
		}
		return node.getTextContent();
	}
	
	/**
	 * Creates a child element with the given name and parent. Avoids the type
	 * of bug whereby the developer calls {@link Document#createElement(String)}
	 * but forgets to append it to the relevant parent.
	 * 
	 * @param tagName the name of the new child (required)
	 * @param parent the parent node (required)
	 * @param document the document to which the parent and child belong (required)
	 * @return the created element
	 * @since 1.2.0
	 */
	public static Element createChildElement(final String tagName, final Node parent, final Document document) {
		final Element child = document.createElement(tagName);
		parent.appendChild(child);
		return child;
	}
	
	/**
	 * Returns the child node with the given tag name, creating it if it does
	 * not exist
	 * 
	 * @param tagName the child tag to look for and possibly create (required)
	 * @param parent the parent in which to look for the child (required)
	 * @param document the document containing the parent (required)
	 * @return the existing or created child (never <code>null</code>)
	 * @since 1.2.0
	 */
	public static Element createChildIfNotExists(final String tagName, final Node parent, final Document document) {
		final Element existingChild = findFirstElement(tagName, parent);
		if (existingChild != null) {
			return existingChild;
		}
		// No such child; add it
		return createChildElement(tagName, parent, document);
	}
	
	/**
	 * Returns the text content of the first child of the given parent that has
	 * the given tag name, if any
	 * 
	 * @param parent the parent in which to search (required)
	 * @param child the child name for which to search (required)
	 * @return <code>null</code> if there is no such child, otherwise the first
	 * such child's text content
	 */
	public static String getChildTextContent(final Element parent, final String child) {
		final List<Element> children = findElements(child, parent);
		if (children.isEmpty()) {
			return null;
		}
		return getTextContent(children.get(0), null);
	}
	
	/**
	 * Constructor is private to prevent instantiation
	 */
	private XmlUtils() {}
}