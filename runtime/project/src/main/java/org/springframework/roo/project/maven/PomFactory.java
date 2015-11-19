package org.springframework.roo.project.maven;

import org.w3c.dom.Element;

/**
 * A Factory for {@link Pom}s.
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public interface PomFactory {

    /**
     * Creates a {@link Pom} by reading a <code>pom.xml</code> file
     * 
     * @param root the root element of the XML file (required)
     * @param pomPath the canonical path of the XML file (required)
     * @param moduleName the name of the module to which the POM belongs (blank
     *            means the root or only POM)
     * @return a non-<code>null</code> instance
     */
    Pom getInstance(Element root, String pomPath, String moduleName);
}
