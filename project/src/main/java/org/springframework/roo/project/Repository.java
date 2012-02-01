package org.springframework.roo.project;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.support.util.XmlElementBuilder;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Simplified immutable representation of a repository.
 * <p>
 * Structured after the model used by Maven and Ivy.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
public class Repository implements Comparable<Repository> {

    private final boolean enableSnapshots;
    private final String id;
    private final String name;
    private final String url;

    /**
     * Convenience constructor for creating a repository instance from an XML
     * Element
     * 
     * @param element containing the repository definition (required)
     */
    public Repository(final Element element) {
        Validate.notNull(element, "Element required");
        final Element name = XmlUtils.findFirstElement("name", element);
        final Element snapshotsElement = XmlUtils.findFirstElement("snapshots",
                element);
        enableSnapshots = snapshotsElement == null ? false : Boolean
                .valueOf(XmlUtils.findRequiredElement("enabled",
                        snapshotsElement).getTextContent());
        id = XmlUtils.findRequiredElement("id", element).getTextContent();
        this.name = name == null ? null : name.getTextContent();
        url = XmlUtils.findRequiredElement("url", element).getTextContent();
    }

    /**
     * Constructor for snapshots disabled
     * 
     * @param id the repository id (required)
     * @param name the repository name (optional)
     * @param url the repository url (required)
     */
    public Repository(final String id, final String name, final String url) {
        this(id, name, url, false);
    }

    /**
     * Constructor for snapshots optionally enabled
     * 
     * @param id the repository id (required)
     * @param name the repository name (optional)
     * @param url the repository url (required)
     * @param enableSnapshots true if snapshots are allowed, otherwise false
     */
    public Repository(final String id, final String name, final String url,
            final boolean enableSnapshots) {
        Validate.notBlank(id, "ID required");
        Validate.notBlank(url, "URL required");
        this.enableSnapshots = enableSnapshots;
        this.id = id;
        this.name = StringUtils.trimToNull(name);
        this.url = url;
    }

    public int compareTo(final Repository o) {
        if (o == null) {
            throw new NullPointerException();
        }
        int result = id.compareTo(o.id);
        if (result == 0) {
            result = url.compareTo(o.url);
        }
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof Repository && compareTo((Repository) obj) == 0;
    }

    /**
     * Returns the XML element for this repository
     * 
     * @param document the document in which to create the element (required)
     * @param tagName the name of the element to create (required)
     * @return a non-<code>null</code> element
     * @since 1.2.0
     */
    public Element getElement(final Document document, final String tagName) {
        final Element repositoryElement = new XmlElementBuilder(tagName,
                document)
                .addChild(
                        new XmlElementBuilder("id", document).setText(id)
                                .build())
                .addChild(
                        new XmlElementBuilder("url", document).setText(url)
                                .build()).build();
        if (name != null) {
            repositoryElement.appendChild(new XmlElementBuilder("name",
                    document).setText(name).build());
        }
        if (enableSnapshots) {
            repositoryElement.appendChild(new XmlElementBuilder("snapshots",
                    document).addChild(
                    new XmlElementBuilder("enabled", document).setText("true")
                            .build()).build());
        }
        return repositoryElement;
    }

    /**
     * The id of the repository
     * 
     * @return the id (never null)
     */
    public String getId() {
        return id;
    }

    /**
     * The name of the repository
     * 
     * @return the name of the repository (null if not exists)
     */
    public String getName() {
        return name;
    }

    /**
     * The url of the repository
     * 
     * @return the url (never null)
     */
    public String getUrl() {
        return url;
    }

    @Override
    public int hashCode() {
        return 11 * id.hashCode() * url.hashCode();
    }

    /**
     * Indicates if snapshots are enabled
     * 
     * @return enableSnapshots
     */
    public boolean isEnableSnapshots() {
        return enableSnapshots;
    }

    @Override
    public String toString() {
        final ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("id", id);
        builder.append("name", name);
        builder.append("url", url);
        return builder.toString();
    }
}
