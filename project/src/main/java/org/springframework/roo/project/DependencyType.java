package org.springframework.roo.project;

import org.apache.commons.lang3.StringUtils;
import org.springframework.roo.support.util.DomUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Element;

/**
 * The type of a {@link Dependency}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public enum DependencyType {

    JAR, OTHER, WAR, ZIP;

    /**
     * Returns the type of the dependency represented by the given XML element
     * 
     * @param dependencyElement the element from which to parse the type
     *            (required)
     * @return a non-<code>null</code> type
     * @since 1.2.0
     */
    public static DependencyType getType(final Element dependencyElement) {
        // Find the type code, if any
        final String type = getTypeCode(dependencyElement);

        // Resolve this to a DependencyType
        return valueOfTypeCode(type);
    }

    private static String getTypeCode(final Element dependencyElement) {
        if (dependencyElement.hasAttribute("type")) {
            return dependencyElement.getAttribute("type");
        }
        // Read it from the "type" child element, if any
        return DomUtils.getTextContent(
                XmlUtils.findFirstElement("type", dependencyElement), "")
                .trim();
    }

    /**
     * Returns the {@link DependencyType} with the given code.
     * 
     * @param typeCode the type code to decode (can be anything, case doesn't
     *            matter)
     * @return {@link #OTHER} if the given code is non-blank and unrecognised
     * @since 1.2.0
     */
    public static DependencyType valueOfTypeCode(final String typeCode) {
        if (StringUtils.isBlank(typeCode)) {
            return JAR;
        }
        try {
            return valueOf(typeCode.toUpperCase());
        }
        catch (final IllegalArgumentException invalidCode) {
            return OTHER;
        }
    }
}
