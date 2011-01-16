package org.springframework.roo.support.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
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
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Utilities related to DOM and XML usage.
 * 
 * @author Stefan Schmidt
 * @author Ben Alex
 * @author Alan Stewart
 * @since 1.0
 */
public final class XmlUtils {
	private static final Map<String, XPathExpression> compiledExpressionCache = new HashMap<String, XPathExpression>();
	private static final XPath xpath = XPathFactory.newInstance().newXPath();
	private static final TransformerFactory transformerFactory = TransformerFactory.newInstance();
	private static final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	
	private XmlUtils() {
	}

	/**
	 * Write an XML document to the OutputStream provided. This will use the pre-configured Roo provided Transformer.
	 * 
	 * @param outputStream the output stream to write to. The stream is closed upon completion.
	 * @param document the document to write.
	 */
	public static final void writeXml(OutputStream outputStream, Document document) {
		writeXml(createIndentingTransformer(), outputStream, document);
	}

	/**
	 * Write an XML document to the OutputStream provided. This will use the provided Transformer.
	 * 
	 * @param transformer the transformer (can be obtained from XmlUtils.createIndentingTransformer())
	 * @param outputStream the output stream to write to. The stream is closed upon completion.
	 * @param document the document to write.
	 */
	public static final void writeXml(Transformer transformer, OutputStream outputStream, Document document) {
		Assert.notNull(transformer, "Transformer required");
		Assert.notNull(outputStream, "OutputStream required");
		Assert.notNull(document, "Document required");
		
		transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		try {
			StreamResult streamResult = createUnixStreamResultForEntry(outputStream);
			transformer.transform(new DOMSource(document), streamResult);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		} finally {
			try {
				outputStream.close();
			} catch (IOException ignored) {
				// Do nothing
			}
		}
	}
	
	/**
	 * Creates a {@link StreamResult} by wrapping the given outputEntry in an
	 * {@link OutputStreamWriter} that transforms Windows line endings (\r\n) 
	 * into Unix line endings (\n) on Windows for consistency with Roo's templates.  
	 * @param outputStream
	 * @return StreamResult 
	 * @throws UnsupportedEncodingException 
	 */
	private static StreamResult createUnixStreamResultForEntry(OutputStream outputStream) throws UnsupportedEncodingException {
		final Writer writer;
		if (System.getProperty("line.separator").equals("\r\n")) {
			writer = new OutputStreamWriter(outputStream, "ISO-8859-1") {
				public void write(char[] cbuf, int off, int len) throws IOException {
					for (int i = off; i < off + len; i++) {
						if (cbuf[i] != '\r' || (i < cbuf.length - 1 && cbuf[i + 1] != '\n')) {
							super.write(cbuf[i]);
						}
					}
				}

				public void write(int c) throws IOException {
					if (c != '\r') super.write(c);
				}
				public void write(String str, int off, int len) throws IOException {
					String orig = str.substring(off, off + len);
					String filtered = orig.replace("\r\n", "\n");
					int lengthDiff = orig.length() - filtered.length();
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
	 * Checks in under a given root element whether it can find a child element
	 * which matches the XPath expression supplied. Returns {@link Element} if
	 * exists.
	 * 
	 * Please note that the XPath parser used is NOT namespace aware. So if you
	 * want to find a element <beans><sec:http> you need to use the following
	 * XPath expression '/beans/http'.
	 * 
	 * @param xPathExpression the xPathExpression (required)
	 * @param root the parent DOM element (required)
	 * @return the Element if discovered (null if not found)
	 */
	public static Element findFirstElement(String xPathExpression, Element root) {
		Node node = findNode(xPathExpression, root);
		if (node != null && node instanceof Element) {
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
	 * want to find a element <beans><sec:http> you need to use the following
	 * XPath expression '/beans/http'.
	 * 
	 * @param xPathExpression the XPath expression (required)
	 * @param root the parent DOM element (required)
	 * @return the Node if discovered (null if not found)
	 */
	public static Node findNode(String xPathExpression, Element root) {
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
		} catch (XPathExpressionException e) {
			throw new IllegalArgumentException("Unable evaluate XPath expression", e);
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
	public static Element findFirstElementByName(String name, Element root) {
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
	public static Element findRequiredElement(String xPathExpression, Element root) {
		Assert.hasText(xPathExpression, "XPath expression required");
		Assert.notNull(root, "Root element required");
		Element element = findFirstElement(xPathExpression, root);
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
	 * @return a {@link List} of type {@link Element} if discovered, otherwise null
	 */
	public static List<Element> findElements(String xPathExpression, Element root) {
		List<Element> elements = new ArrayList<Element>();
		NodeList nodes = null;

		try {
			XPathExpression expr = compiledExpressionCache.get(xPathExpression);
			if (expr == null) {
				expr = xpath.compile(xPathExpression);
				compiledExpressionCache.put(xPathExpression, expr);
			}
			nodes = (NodeList) expr.evaluate(root, XPathConstants.NODESET);
		} catch (XPathExpressionException e) {
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
	public static Node findFirstAttribute(String xPathExpression, Element element) {
		Node attr = null;
		try {
			XPathExpression expr = compiledExpressionCache.get(xPathExpression);
			if (expr == null) {
				expr = xpath.compile(xPathExpression);
				compiledExpressionCache.put(xPathExpression, expr);
			}
			attr = (Node) expr.evaluate(element, XPathConstants.NODE);
		} catch (XPathExpressionException e) {
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
		} catch (Exception e) {
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
		} catch (ParserConfigurationException ex) {
			throw new IllegalStateException(ex);
		}
	}
	
	/**
	 * Removes empty text nodes from the specified element
	 * 
	 * @param element the element where empty text nodes will be removed
	 */
	public static void removeTextNodes(Element element) {
		if (element == null) {
			return;
		}
		
		NodeList children = element.getChildNodes();
		for (int i = children.getLength() - 1; i >= 0; i--) {
			Node child = children.item(i);
			switch (child.getNodeType()) {
				case Node.ELEMENT_NODE:
					removeTextNodes((Element) child);
					break;
				case Node.CDATA_SECTION_NODE:
				case Node.TEXT_NODE:
					if (!StringUtils.hasText(child.getNodeValue())) {
						element.removeChild(child);
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
	public static Element getConfiguration(Class<?> clazz, String configurationPath) {
		InputStream templateInputStream = TemplateUtils.getTemplate(clazz, configurationPath);
		Assert.notNull(templateInputStream, "Could not acquire " + configurationPath + " file");
		Document configurationDocument;
		try {
			configurationDocument = getDocumentBuilder().parse(templateInputStream);
		} catch (Exception e) {
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
	public static Element getConfiguration(Class<?> clazz) {
		return getConfiguration(clazz, "configuration.xml");
	}
	
	/**
	 * Converts a XHTML compliant id (used in jspx) to a CSS3 selector spec compliant id. In that
	 * it will replace all '.,:,-' to '_'
	 * 
	 * @param proposed Id
	 * @return cleaned up Id
	 */
	public static String convertId(String proposed) {
		return proposed.replaceAll("[:\\.-]", "_");
	}
	
	/**
	 * Checks the presented element for illegal characters that could cause malformed XML.
	 * 
	 * @param element the content of the XML element
	 * @throws IllegalArgumentException if the element is null, has no text or contains illegal characters 
	 */
	public static void assertElementLegal(String element) {
		if (!StringUtils.hasText(element)) {
			throw new IllegalArgumentException("Element required");
		}
		
		// Note regular expression for legal characters found to be x5 slower in profiling than this approach
		char[] value = element.toCharArray();
		for (int i = 0; i < value.length; i++) {
			char c = value[i];
			if (' ' == c || '*' == c || '>' == c || '<' == c || '!' == c || '@' == c || '%' == c || '^' == c ||
				'?' == c || '(' == c || ')' == c || '~' == c || '`' == c || '{' == c || '}' == c || '[' == c || ']' == c ||
				'|' == c || '\\' == c || '\'' == c || '+' == c)  {
				throw new IllegalArgumentException("Illegal name '" + element + "' (illegal character)");
			}
		}
	}
}