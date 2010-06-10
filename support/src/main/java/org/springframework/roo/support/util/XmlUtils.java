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
 * 
 */
public final class XmlUtils {
	private static final Map<String, XPathExpression> compiledExpressionCache = new HashMap<String, XPathExpression>();
	private static final XPath xpath = XPathFactory.newInstance().newXPath();
	private static final TransformerFactory transformerFactory = TransformerFactory.newInstance();
	private static final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	
	private XmlUtils() {
	}

	/**
	 * Write an XML document to the outputstream provided. This will use the preconfigured Roo provided Transformer.
	 * 
	 * @param outputEntry The output stream to write to.
	 * @param document The document to write
	 */
	public static final void writeXml(OutputStream outputEntry, Document document) {
		writeXml(createIndentingTransformer(), outputEntry, document);
	}

	/**
	 * Write an XML document to the outputstream provided. This will use the provided Transformer.
	 * 
	 * @param transformer The transformer (can be obtained from XmlUtils.createIndentingTransformer())
	 * @param outputEntry The output stream to write to.
	 * @param document The document to write
	 */
	public static final void writeXml(Transformer transformer, OutputStream outputEntry, Document document) {
		Assert.notNull(transformer, "Transformer required");
		Assert.notNull(outputEntry, "Output entry required");
		Assert.notNull(document, "Document required");
		
		transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		try {
			transformer.transform(new DOMSource(document), createUnixStreamResultForEntry(outputEntry));
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}
	
	/**
	 * Creates a {@link StreamResult} by wrapping the given outputEntry in an
	 * {@link OutputStreamWriter} that transforms Windows line endings (\r\n) 
	 * into Unix line endings (\n) on Windows for consistency with Roo's templates.  
	 * @param outputEntry
	 * @return StreamResult 
	 * @throws UnsupportedEncodingException 
	 */
	private static StreamResult createUnixStreamResultForEntry(OutputStream outputEntry) throws UnsupportedEncodingException {
		final Writer writer;
		if (System.getProperty("line.separator").equals("\r\n")) {
			writer = new OutputStreamWriter(outputEntry, "ISO-8859-1") {
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
			writer = new OutputStreamWriter(outputEntry, "ISO-8859-1");
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
	 * 
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
	 * @param xPathExpression the xPathExpression (required)
	 * @param root the parent DOM element (required)
	 * 
	 * @return the Node if discovered (null if not found)
	 */
	public static Node findNode(String xPathExpression, Element root) {
		if (xPathExpression == null || root == null || xPathExpression.length() == 0) {
			throw new IllegalArgumentException("Xpath expression and root element required");
		}
		Node node = null;
		try {
			XPathExpression expr = compiledExpressionCache.get(xPathExpression);
			if (expr == null) {
				expr = xpath.compile(xPathExpression);
				compiledExpressionCache.put(xPathExpression, expr);
			}
			node = (Node) expr.evaluate(root, XPathConstants.NODE);
		} catch (XPathExpressionException e) {
			throw new IllegalArgumentException("Unable evaluate xpath expression", e);
		}
		return node;
	}

	/**
	 * Checks in under a given root element whether it can find a child element
	 * which matches the name supplied. Returns {@link Element} if exists.
	 * 
	 * @param name the Element name (required)
	 * @param root the parent DOM element (required)
	 * 
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
	 * @param xPathExpression the xPathExpression (required)
	 * @param root the parent DOM element (required)
	 * 
	 * @return the Element if discovered (never null; an exception is thrown if
	 *         cannot be found)
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
	 * 
	 * @return a {@link List} of type {@link Element} if discovered, otherwise null
	 */
	public static List<Element> findElements(String xPathExpression, Element root) {
		List<Element> elements = new ArrayList<Element>();
		NodeList nodes = null;

		try {
			XPathExpression expr = xpath.compile(xPathExpression);
			nodes = (NodeList) expr.evaluate(root, XPathConstants.NODESET);
		} catch (XPathExpressionException e) {
			throw new IllegalArgumentException("Unable evaluate xpath expression", e);
		}

		for (int i = 0; i < nodes.getLength(); i++) {
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
	 * 
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
		Transformer xformer;
		try {
			transformerFactory.setAttribute("indent-number", 4);
			xformer = transformerFactory.newTransformer();
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
		xformer.setOutputProperty(OutputKeys.INDENT, "yes");
		xformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
		return xformer;
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
}

