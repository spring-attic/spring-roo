package org.springframework.roo.support.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

/**
 * Utilities related to DOM and XML usage.
 * 
 * @author Stefan Schmidt
 * @author Ben Alex
 * @since 1.0
 * 
 */
public abstract class XmlUtils {

	private static final Map<String, XPathExpression> compiledExpressionCache = new HashMap<String, XPathExpression>();
	private static final XPath xpath = XPathFactory.newInstance().newXPath();
	private static final TransformerFactory transformerFactory = TransformerFactory.newInstance();
	private static final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	private static MessageDigest digest;
	static {
		try {
		  digest = MessageDigest.getInstance("sha-1");
		} catch (NoSuchAlgorithmException e) {
			new IllegalStateException("Could not create hash key for identifier");
		}
	}
	
	public static final void writeXml(OutputStream outputEntry, Document document) {
		writeXml(createIndentingTransformer(), outputEntry, document);
	}

	public static final void writeMalformedXml(OutputStream outputEntry, NodeList nodes) {
		writeMalformedXml(createIndentingTransformer(), outputEntry, nodes);
	}

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

	public static final void writeMalformedXml(Transformer transformer, OutputStream outputEntry, NodeList nodes) {
		Assert.notNull(transformer, "Transformer required");
		Assert.notNull(outputEntry, "Output entry required");
		Assert.notNull(nodes, "NodeList required");

		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

		try {
			for (int i = 0; i < nodes.getLength(); i++) {
				transformer.transform(new DOMSource(nodes.item(i)), createUnixStreamResultForEntry(outputEntry));
			}
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
		if (xPathExpression == null || root == null || xPathExpression.length() == 0) {
			throw new IllegalArgumentException("Xpath expression and root element required");
		}

		Element rootElement = null;
		try {
			XPathExpression expr = compiledExpressionCache.get(xPathExpression);
			if (expr == null) {
				expr = xpath.compile(xPathExpression);
				compiledExpressionCache.put(xPathExpression, expr);
			}
			rootElement = (Element) expr.evaluate(root, XPathConstants.NODE);
		} catch (XPathExpressionException e) {
			throw new IllegalArgumentException("Unable evaluate xpath expression", e);
		}
		return rootElement;
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
	 * XPath expression supplied. Returns {@link Attr} if exists.
	 * 
	 * @param xPathExpression the xPathExpression (required)
	 * @param element (required)
	 * 
	 * @return the Element if discovered (null if not found)
	 */
	public static Attr findFirstAttribute(String xPathExpression, Element element) {
		Attr attr = null;
		try {
			XPathExpression expr = compiledExpressionCache.get(xPathExpression);
			if (expr == null) {
				expr = xpath.compile(xPathExpression);
				compiledExpressionCache.put(xPathExpression, expr);
			}
			attr = (Attr) expr.evaluate(element, XPathConstants.NODE);
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
//		factory.setNamespaceAware(true);
		try {
			return factory.newDocumentBuilder();
		} catch (ParserConfigurationException ex) {
			throw new IllegalStateException(ex);
		}
	}
	
	/**
	 * Convenience method which will call data.getBytes() and create a sha-1 hash for the
	 * provided data.
	 * 
	 * @param data to hash
	 * @return byte[] hash of the input data
	 */
	public static byte[] sha1(String data) {
		return sha1(data.getBytes());
	}
	
	/**
	 * Create a base 64 encoded hash key for a given XML element. The key is based on the 
	 * element name, the attribute names and their values. Child elements are ignored. 
	 * Attributes named 'z' are not concluded since they contain the hash key itself.
	 * 
	 * @param element The element to create the base 64 encoded hash key for
	 * @return the hash key
	 */
	public static byte[] sha1Element(Element element) {
		StringBuilder sb = new StringBuilder(); 
		sb.append(element.getTagName());
		NamedNodeMap attributes = element.getAttributes();
		for (int i = 0; i < attributes.getLength(); i++) {
			if (!"z".equals(attributes.item(i).getNodeName())) {
				sb.append(attributes.item(i).getNodeName()).append(attributes.item(i).getNodeValue());
			}
		}
		return sha1(sb.toString().getBytes());
	}
	
	/**
	 * Creates a sha-1 hash value for the given data byte array.
	 * 
	 * @param data to hash
	 * @return byte[] hash of the input data
	 */
	public static byte[] sha1(byte[] data) {
		Assert.notNull(digest, "Could not create hash key for identifier");
		return digest.digest(data);
	}
	
	public static String base64(String data) {
		return base64(data.getBytes());
	}
	
	public static String base64(byte[] data) {
		return Base64.encodeBytes(data);
	}
}
