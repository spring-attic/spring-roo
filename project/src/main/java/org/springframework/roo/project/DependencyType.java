package org.springframework.roo.project;

import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Element;

/**
 * The type of a {@link Dependency}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public enum DependencyType {
	
	JAR,
	
	ZIP,
	
	OTHER;
	
	/**
	 * Returns the type of the dependency represented by the given XML element
	 * 
	 * @param dependency the element from which to parse the type (required)
	 * @return a non-<code>null</code> type
	 * @since 1.2.0
	 */
	public static DependencyType getType(final Element dependency) {
		// Find the type code, if any
		final String type;
		if (dependency.hasAttribute("type")) {
			type = dependency.getAttribute("type");
		} else {
			// Read it from the "type" child element, if any
			type = XmlUtils.getTextContent(XmlUtils.findFirstElement("type", dependency), "").trim();
		}
		
		// Resolve this to a DependencyType
		if ("".equals(type) || "jar".equalsIgnoreCase(type)) {
			return JAR;
		}
		if ("zip".equalsIgnoreCase(type)) {
			return ZIP;
		} 
		return OTHER;
	}
}
