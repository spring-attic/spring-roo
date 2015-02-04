package org.springframework.roo.project;

import org.apache.commons.lang3.Validate;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Element;

/**
 * Immutable representation of an configuration specification for a (Maven)
 * build plugin
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class Configuration implements Comparable<Configuration> {

    /**
     * Factory method
     * 
     * @param configurationElement the XML node from which to parse the instance
     *            (can be <code>null</code>)
     * @return <code>null</code> if a <code>null</code> element is given
     * @since 1.2.0
     */
    public static Configuration getInstance(final Element configurationElement) {
        if (configurationElement == null) {
            return null;
        }
        return new Configuration(configurationElement);
    }

    private final Element configuration;

    /**
     * Constructor from an XML element. Consider using
     * {@link #getInstance(Element)} instead for null-safety.
     * 
     * @param configuration the XML element specifying the configuration
     *            (required)
     */
    public Configuration(final Element configuration) {
        Validate.notNull(configuration, "configuration must be specified");
        this.configuration = configuration;
    }

    public int compareTo(final Configuration o) {
        if (o == null) {
            throw new NullPointerException();
        }
        return XmlUtils.compareNodes(configuration, o.configuration) ? 0 : 1;
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof Configuration
                && compareTo((Configuration) obj) == 0;
    }

    /**
     * Returns the XML element that defines this configuration
     * 
     * @return a non-<code>null</code> element
     */
    public Element getConfiguration() {
        return configuration;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + (configuration == null ? 0 : configuration.hashCode());
        return result;
    }
}
