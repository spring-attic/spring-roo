package org.springframework.roo.project;

import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Element;

/**
 * The scope of the dependency.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public enum DependencyScope {
    COMPILE, IMPORT, PROVIDED, RUNTIME, SYSTEM, TEST;

    /**
     * Parses the scope of the given dependency XML element
     * 
     * @param dependency the element to parse (required)
     * @return a non-<code>null</code> scope
     */
    public static DependencyScope getScope(final Element dependency) {
        final String scopeString;
        if (dependency.hasAttribute("scope")) {
            scopeString = dependency.getAttribute("scope");
        }
        else {
            // Check for a child element
            final Element scopeElement = XmlUtils.findFirstElement("scope",
                    dependency);
            if (scopeElement == null) {
                scopeString = COMPILE.name();
            }
            else {
                scopeString = scopeElement.getTextContent();
            }
        }

        try {
            return valueOf(scopeString.toUpperCase().trim());
        }
        catch (final IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid dependency scope '"
                    + scopeString.toUpperCase().trim() + "'", e);
        }
    }
}
