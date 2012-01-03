package org.springframework.roo.project;

import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;
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

    // Fields
    private final boolean enableSnapshots;
    private final String id;
    private final String name;
    private final String url;

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
        Assert.hasText(id, "ID required");
        Assert.hasText(url, "URL required");
        this.enableSnapshots = enableSnapshots;
        this.id = id;
        this.name = StringUtils.trimToNull(name);
        this.url = url;
    }

    /**
     * Convenience constructor for creating a repository instance from an XML
     * Element
     * 
     * @param element containing the repository definition (required)
     */
    public Repository(final Element element) {
        Assert.notNull(element, "Element required");
        final Element name = XmlUtils.findFirstElement("name", element);
        final Element snapshotsElement = XmlUtils.findFirstElement("snapshots",
                element);
        this.enableSnapshots = (snapshotsElement == null ? false : Boolean
                .valueOf(XmlUtils.findRequiredElement("enabled",
                        snapshotsElement).getTextContent()));
        this.id = XmlUtils.findRequiredElement("id", element).getTextContent();
        this.name = (name == null ? null : name.getTextContent());
        this.url = XmlUtils.findRequiredElement("url", element)
                .getTextContent();
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

    /**
     * Indicates if snapshots are enabled
     * 
     * @return enableSnapshots
     */
    public boolean isEnableSnapshots() {
        return enableSnapshots;
    }

    @Override
    public int hashCode() {
        return 11 * this.id.hashCode() * this.url.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof Repository
                && this.compareTo((Repository) obj) == 0;
    }

    public int compareTo(final Repository o) {
        if (o == null) {
            throw new NullPointerException();
        }
        int result = this.id.compareTo(o.id);
        if (result == 0) {
            result = this.url.compareTo(o.url);
        }
        return result;
    }

    @Override
    public String toString() {
        final ToStringCreator tsc = new ToStringCreator(this);
        tsc.append("id", id);
        tsc.append("name", name);
        tsc.append("url", url);
        return tsc.toString();
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
                        new XmlElementBuilder("id", document).setText(this.id)
                                .build())
                .addChild(
                        new XmlElementBuilder("url", document)
                                .setText(this.url).build()).build();
        if (this.name != null) {
            repositoryElement.appendChild(new XmlElementBuilder("name",
                    document).setText(this.name).build());
        }
        if (this.enableSnapshots) {
            repositoryElement.appendChild(new XmlElementBuilder("snapshots",
                    document).addChild(
                    new XmlElementBuilder("enabled", document).setText("true")
                            .build()).build());
        }
        return repositoryElement;
    }
}
