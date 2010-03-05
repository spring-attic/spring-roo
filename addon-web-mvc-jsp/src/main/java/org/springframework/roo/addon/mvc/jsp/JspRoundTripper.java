package org.springframework.roo.addon.mvc.jsp;

import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

public class JspRoundTripper {
	
	/**
	 * This method will compare the original document with the proposed document and return 
	 * an adjusted document if necessary. Adjustments are only made if new elements or 
	 * attributes are proposed. Changes to the order of attributes or elements in the 
	 * original document will not result in an adjustment.
	 * 
	 * @param original document as read from the file system
	 * @param proposed document as determined by the JspViewManager
	 * @return the new document if changes are necessary, null if no changes are necessary
	 */
	public Document analyseDocument(Document original, Document proposed) {
		boolean originalDocumentAdjusted = false;
		originalDocumentAdjusted = checkNamespaces(original, proposed);
		originalDocumentAdjusted = compareElements(original.getDocumentElement(), proposed.getDocumentElement(), false);
		if (originalDocumentAdjusted) {
			return original;
		}
		return null;
	}
	
	/**
	 * Compare necessary namespace declarations between original and proposed document, if 
	 * namespaces in the original are missing compared to the proposed, we add them to the 
	 * original.
	 * 
	 * @param original document as read from the file system
	 * @param proposed document as determined by the JspViewManager
	 * @return the new document if changes are necessary, null if no changes are necessary
	 */
	private boolean checkNamespaces(Document original, Document proposed) {
	    boolean originalDocumentChanged = false;
		NamedNodeMap nsNodes = proposed.getDocumentElement().getAttributes();
		for (int i = 0; i < nsNodes.getLength(); i++) {
			Attr el = XmlUtils.findFirstAttribute("//@" + nsNodes.item(i).getNodeName(), original.getDocumentElement());
			if (el == null) {
				original.getDocumentElement().setAttribute(nsNodes.item(i).getNodeName(), nsNodes.item(i).getNodeValue());
				originalDocumentChanged = true;
			}
		}
		return originalDocumentChanged;
	}
	
	
	private boolean compareElements(Element original, Element proposed, boolean originalDocumentChanged) {
		NodeList proposedChildren = proposed.getChildNodes();
		for (int i = 0; i < proposedChildren.getLength(); i++) {
			Element proposedElement = (Element) proposedChildren.item(i);
			String proposedHashCode = proposedElement.getAttribute("z");
			if (proposedHashCode.length() != 0) {
				String[] proposedHashCodes = proposedHashCode.split("-");
				if (proposedHashCodes.length == 2) {
					Element originalElement = XmlUtils.findFirstElement("//*[starts-with(@z,'" + proposedHashCodes[0] + "')]", original);
					if (null != originalElement) {
						String originalHashCode = originalElement.getAttribute("z");
						if (originalHashCode.length() != 0) {
							String[] originalHashCodes = proposedHashCode.split("-");
							if (originalHashCodes.length == 2) {
								if (originalHashCodes[1].equals(proposedHashCodes[1])) {
									//the original element has not been changed so Roo can make adjustments if necessary
									if (!proposedElement.getTagName().equals(originalElement.getTagName())) {
										//Roo proposes to change the tag name, we need to rename the element
										original.getOwnerDocument().renameNode(originalElement, "", proposedElement.getTagName());
										originalElement.setAttribute("z", proposedHashCode);
										originalDocumentChanged = true;
									}
								}
							}
						}
					}
				}
			}
		}
		return originalDocumentChanged;
	}
}
