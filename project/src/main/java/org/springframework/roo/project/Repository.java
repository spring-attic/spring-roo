package org.springframework.roo.project;

import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Element;

/**
 * Simplified immutable representation of a repository.
 * 
 * <p>
 * Structured after the model used by Maven and Ivy.
 *
 * @author Stefan Schmidt
 * @since 1.1
 *
 */
public class Repository implements Comparable<Repository> {
	private String id;
	private String name;
	private String url;
	private boolean enableSnapshots = false;
	
	/**
	 * Convenience constructor creating a repository instance
	 * 
	 * @param id the repository id (required)
	 * @param name the repository name (optional)
	 * @param url the repository url (required)
	 */
	public Repository(String id, String name, String url) {
		Assert.hasText(id, "Group ID required");
		Assert.notNull(url, "URL required");
		this.id = id;
		this.url = url;
		if (name != null && name.length() > 0) {
			this.name = name;
		}
	}
	
	/**
	 * Convenience constructor for creating a repository instance
	 * 
	 * @param id the repository id (required)
	 * @param name the repository name (required)
	 * @param url the repository url (required)
	 * @param enableSnapshots true if snapshots are allowed, otherwise false
	 */
	public Repository(String id, String name, String url, boolean enableSnapshots) {
		Assert.hasText(id, "Group ID required");
		Assert.hasText(url, "URL required");
		this.id = id;
		if (name != null && name.length() > 0) {
			this.name = name;
		}
		this.url = url;
		this.enableSnapshots = enableSnapshots;
	}
	
	/**
	 * Convenience constructor for creating a repository instance from a 
	 * XML Element
	 * 
	 * @param element containing the repository definition (required)
	 */
	public Repository(Element element) {
		Assert.notNull(element, "Element required");
		this.id = XmlUtils.findRequiredElement("id", element).getTextContent();
		this.url = XmlUtils.findRequiredElement("url", element).getTextContent();
		Element name = XmlUtils.findFirstElement("name", element);
		if (name != null) {
			this.name = name.getTextContent();
		}
		if (null != XmlUtils.findFirstElement("snapshots", element)) {
			this.enableSnapshots = new Boolean(XmlUtils.findRequiredElement("snapshots/enabled", element).getTextContent());
		}
	}

	/**
	 * The id of a repository
	 * 
	 * @return the id (never null)
	 */
	public String getId() {
		return id;
	}

	/**
	 * The name of a repository
	 * 
	 * @return the name of the repository (null if not exists)
	 */
	public String getName() {
		return name;
	}

	/**
	 * The url of a repository
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
	
	public int hashCode() {
		return 11 * this.id.hashCode() * this.url.hashCode();
	}

	public boolean equals(Object obj) {
		return obj != null && obj instanceof Repository && this.compareTo((Repository) obj) == 0;
	}

	public int compareTo(Repository o) {
		if (o == null) {
			throw new NullPointerException();
		}
		int result = this.id.compareTo(o.id);
		if (result == 0) {
			result = this.url.compareTo(o.url);
		}
		return result;
	}
	
	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("id", id);
		tsc.append("name", name);
		tsc.append("url", url);
		return tsc.toString();
	}
}
