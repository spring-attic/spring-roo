package org.springframework.roo.support.util;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;
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

	public static final void writeXml(OutputStream outputEntry, Document document) {
		writeXml(createIndentingTransformer(), outputEntry, document);
	}

	public static final void writeMalformedXml(OutputStream outputEntry, NodeList nodes) {
		writeMalformedXml(createIndentingTransformer(), outputEntry, nodes);
	}

	// /**
	// * Obtains an element, throwing an {@link IllegalArgumentException} if not
	// * found.
	// *
	// * <p>
	// * Internally delegates to
	// * {@link DomUtils#getChildElementByTagName(Element, String)}.
	// *
	// * @param ele
	// * the DOM element to analyze (required)
	// * @param childEleName
	// * the child element name to look for (required)
	// * @return the Element instance (never null)
	// */
	// public static Element getRequiredChildElementByTagName(Element ele,
	// String childEleName) {
	// Element e = DomUtils.getChildElementByTagName(ele, childEleName);
	// Assert.notNull("Unable to obtain element '" + childEleName
	// + "' from element '" + ele + "'");
	// return e;
	// }

	// /**
	// * Checks in under a given root element whether it can find a child
	// element
	// * with the exact same name and attribute.
	// *
	// * @param root
	// * the parent DOM element
	// * @param target
	// * the child DOM element name to look for
	// * @return the element if discovered, otherwise null
	// */
	// public static Element findElementIfExists(Element root, Element target) {
	// Assert.notNull(root, "Root element not supplied");
	// Assert.notNull(target, "Target element not supplied");
	// Element match = null;
	//
	// NodeList list = root.getChildNodes();
	// for (int i = 0; i < list.getLength(); i++) {
	//
	// Node existingNode = list.item(i);
	//
	// if (!existingNode.getNodeName().equals(target.getNodeName()))
	// continue;
	//
	// NamedNodeMap existingAttributes = existingNode.getAttributes();
	//
	// // we have a match based on element name only (no target attributes
	// // existing)
	// if (existingAttributes == null && target.getAttributes() == null)
	// return (Element) existingNode;
	//
	// if (existingAttributes == null)
	// continue;
	//
	// for (int j = 0; j < existingAttributes.getLength(); j++) {
	//
	// Node targetAttribute = target.getAttributes().getNamedItem(
	// existingAttributes.item(j).getNodeName());
	//
	// // check if the target node has an attribute with the same name
	// // as one of the existing node attributes
	// if (targetAttribute == null)
	// return null;
	//
	// // check if the target node has an attribute with the same value
	// // as one of the existing node attribute values
	// Attr existingAttribute = (Attr) existingAttributes.item(j);
	// existingAttribute.getValue();
	// if (!targetAttribute.getNodeValue().equals(
	// existingAttribute.getValue()))
	// match = null;
	// else
	// // the currently checked attribute matches
	// match = (Element) existingNode;
	// }
	// }
	// return match;
	// }

	// /**
	// * Checks in under a given root element whether it can find a child
	// element
	// * which contains the text supplied. Returns Element if exists.
	// *
	// * @param root
	// * the parent DOM element
	// * @param contents
	// * text contents of the child element to look for
	// * @return Element if discovered, otherwise null
	// */
	// public static Element findMatchingTextNode(Element root, String contents)
	// {
	// Assert.notNull(root, "Root element not supplied");
	// Assert.hasText(contents, "Text contents not supplied");
	//
	// NodeList list = root.getChildNodes();
	// for (int i = 0; i < list.getLength(); i++) {
	// if (list.item(i) instanceof Element) {
	// if (((Element) list.item(i)).getFirstChild().getTextContent()
	// .equals(contents))
	// return (Element) list.item(i);
	// }
	// }
	// return null;
	// }

	public static final void writeXml(Transformer transformer, OutputStream outputEntry, Document document) {
		Assert.notNull(transformer, "Transformer required");
		Assert.notNull(outputEntry, "Output entry required");
		Assert.notNull(document, "Document required");
		
		transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		
		try {
			transformer.transform(new DOMSource(document), new StreamResult(new OutputStreamWriter(outputEntry, "ISO-8859-1"/* "UTF-8" */)));
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
				transformer.transform(new DOMSource(nodes.item(i)), new StreamResult(new OutputStreamWriter(outputEntry, "ISO-8859-1")));
			}
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	/**
	 * Checks in under a given root element whether it can find a child element
	 * which matches the XPath expression supplied. Returns {@link Element} if
	 * exists.
	 * 
	 * @param xPathExpression
	 *            the xPathExpression (required)
	 * @param root
	 *            the parent DOM element (required)
	 * 
	 * @return the Element if discovered (null if not found)
	 */
	public static Element findFirstElement(String xPathExpression, Element root) {
		if (xPathExpression == null || root == null || xPathExpression.length() == 0) {
			throw new IllegalArgumentException("Xpath expression and root element required");
		}
		
		XPathFactory factory = XPathFactory.newInstance();
		XPath xpath = factory.newXPath();
		xpath.setNamespaceContext(getNamespaceContext());

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
	 * @param name
	 *            the Element name (required)
	 * @param root
	 *            the parent DOM element (required)
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
	 * @param xPathExpression
	 *            the xPathExpression (required)
	 * @param root
	 *            the parent DOM element (required)
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
	 * @param xPathExpression
	 *            the xPathExpression
	 * @param root
	 *            the parent DOM element
	 * 
	 * @return a {@link List} of type {@link Element} if discovered, otherwise
	 *         null
	 */
	public static List<Element> findElements(String xPathExpression, Element root) {
		XPathFactory factory = XPathFactory.newInstance();
		XPath xpath = factory.newXPath();
		xpath.setNamespaceContext(getNamespaceContext());
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
	 * @return a transformer that indents entries by 4 characters (never null)
	 */
	public static final Transformer createIndentingTransformer() {
		Transformer xformer;
		try {
			xformer = TransformerFactory.newInstance().newTransformer();
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
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try {
			return factory.newDocumentBuilder();
		} catch (ParserConfigurationException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private static NamespaceContext getNamespaceContext() {
		return new NamespaceContext() {
			public String getNamespaceURI(String prefix) {
				String uri;

				if (prefix.equals("ns1")) {
					uri = "http://www.springframework.org/schema/aop";
				} else if (prefix.equals("tx")) {
					uri = "http://www.springframework.org/schema/tx";
				} else if (prefix.equals("context")) {
					uri = "http://www.springframework.org/schema/context";
				} else if (prefix.equals("beans")) {
					uri = "http://www.springframework.org/schema/beans";
				} else if (prefix.equals("webflow")) {
					uri = "http://www.springframework.org/schema/webflow-config";
				} else if (prefix.equals("p")) {
					uri = "http://www.springframework.org/schema/p";
				} else if (prefix.equals("security")) {
					uri = "http://www.springframework.org/schema/security";
				} else if (prefix.equals("amq")) {
					uri = "http://activemq.apache.org/schema/core";
				} else if (prefix.equals("jms")) {
					uri = "http://www.springframework.org/schema/jms";
				} else {
					uri = null;
				}
				return uri;
			}

			public Iterator getPrefixes(String val) {
				return null;
			}

			public String getPrefix(String uri) {
				return null;
			}
		};
	}
}
