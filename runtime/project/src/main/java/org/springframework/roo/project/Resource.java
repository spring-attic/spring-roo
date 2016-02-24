package org.springframework.roo.project;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.support.util.DomUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Simplified immutable representation of a Maven resource.
 * <p>
 * Structured after the model used by Maven.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class Resource implements Comparable<Resource> {

    private final String directory;
    private final Boolean filtering;
    private final List<String> includes = new ArrayList<String>();

    /**
     * Convenience constructor when an XML element is available that represents
     * a Maven <resource>.
     * 
     * @param resource to parse (required)
     */
    public Resource(final Element resource) {
        final Element directoryElement = XmlUtils.findFirstElement("directory",
                resource);
        Validate.notNull(directoryElement, "directory element required");
        directory = directoryElement.getTextContent();

        final Element filteringElement = XmlUtils.findFirstElement("filtering",
                resource);
        filtering = filteringElement == null ? null : Boolean
                .valueOf(filteringElement.getTextContent());

        // Parsing for includes
        for (final Element include : XmlUtils.findElements("includes/include",
                resource)) {
            includes.add(include.getTextContent());
        }
    }

    /**
     * Creates an immutable {@link Resource} with no "includes".
     * 
     * @param directory the {@link Path directory} (required)
     * @param filtering whether filtering should occur
     */
    public Resource(final String directory, final Boolean filtering) {
        this(directory, filtering, null);
    }

    /**
     * Creates an immutable {@link Resource} with optional "includes".
     * 
     * @param directory the {@link Path directory} (required)
     * @param filtering whether filtering should occur
     * @param includes the list of includes; can be <code>null</code>
     */
    public Resource(final String directory, final Boolean filtering,
            final Collection<String> includes) {
        Validate.notNull(directory, "Directory required");
        this.directory = directory;
        this.filtering = filtering;
        if (includes != null) {
            this.includes.addAll(includes);
        }
    }

    public int compareTo(final Resource o) {
        if (o == null) {
            throw new NullPointerException();
        }
        return getSimpleDescription().compareTo(o.getSimpleDescription());
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Resource other = (Resource) obj;
        return getSimpleDescription().equals(other.getSimpleDescription());
    }

    public String getDirectory() {
        return directory;
    }

    /**
     * Returns the Maven POM element for this resource
     * 
     * @param document the POM document (required)
     * @return a non-<code>null</code> element
     */
    public Element getElement(final Document document) {
        final Element resourceElement = document.createElement("resource");
        resourceElement.appendChild(XmlUtils.createTextElement(document,
                "directory", directory));

        if (filtering != null) {
            resourceElement.appendChild(XmlUtils.createTextElement(document,
                    "filtering", filtering.toString()));
        }

        if (!includes.isEmpty()) {
            final Element includes = DomUtils.createChildElement("includes",
                    resourceElement, document);
            for (final String include : this.includes) {
                includes.appendChild(XmlUtils.createTextElement(document,
                        "include", include));
            }
        }

        return resourceElement;
    }

    public Boolean getFiltering() {
        return filtering;
    }

    public List<String> getIncludes() {
        return includes;
    }

    public String getSimpleDescription() {
        final StringBuilder builder = new StringBuilder();
        builder.append("directory ").append(directory);
        if (filtering != null) {
            builder.append(", filtering ").append(filtering.toString());
        }
        if (!includes.isEmpty()) {
            builder.append(", includes ").append(
                    StringUtils.join(includes, ","));
        }
        return builder.toString();
    }

    @Override
    public int hashCode() {
        return getSimpleDescription().hashCode();
    }

    @Override
    public String toString() {
        final ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("directory", directory);
        builder.append("filtering", filtering);
        builder.append("includes", includes);
        return builder.toString();
    }
}
