package org.springframework.roo.project;

import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Element;

/**
 * Simplified immutable representation of a maven resource.
 * <p>
 * Structured after the model used by Maven.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class Resource implements Comparable<Resource> {
	private Path directory;
	private Boolean filtering;
	private List<String> includes = new ArrayList<String>();

	/**
	 * Creates an immutable {@link Resource}.
	 * 
	 * @param directory the {@link Path directory}
	 * @param filtering whether filtering should occur
	 */
	public Resource(Path directory, Boolean filtering) {
		this.directory = directory;
		this.filtering = filtering;
	}

	/**
	 * Creates an immutable {@link Resource}.
	 * 
	 * @param directory the {@link Path directory}
	 * @param filtering whether filtering should occur
	 * @param includes the list of includes
	 */
	public Resource(Path directory, Boolean filtering, List<String> includes) {
		Assert.notNull(directory, "Directory required");
		this.directory = directory;
		this.filtering = filtering;
		this.includes = includes;
	}

	/**
	 * Convenience constructor when an XML element is available that represents a Maven <resource>.
	 * 
	 * @param resource to parse (required)
	 */
	public Resource(Element resource) {
		Element directoryElement = XmlUtils.findFirstElement("directory", resource);
		if (directoryElement != null) {
			this.directory = new Path(directoryElement.getTextContent());
		}
		Element filteringElement = XmlUtils.findFirstElement("filtering", resource);
		if (filteringElement != null) {
			this.filtering = Boolean.valueOf(filteringElement.getTextContent());
		}

		// Parsing for includes
		List<Element> includeList = XmlUtils.findElements("includes/include", resource);
		if (includeList != null && !includeList.isEmpty()) {
			for (Element include : includeList) {
				includes.add(include.getTextContent());
			}
		}
	}

	public Path getDirectory() {
		return directory;
	}

	public Boolean getFiltering() {
		return filtering;
	}

	public List<String> getIncludes() {
		return includes;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((directory == null) ? 0 : directory.hashCode());
		result = prime * result + ((filtering == null) ? 0 : filtering.hashCode());
		result = prime * result + ((includes == null) ? 0 : includes.hashCode());
		return result;
	}

	
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Resource other = (Resource) obj;
		if (directory == null) {
			if (other.directory != null) return false;
		} else if (!directory.equals(other.directory)) return false;
		if (filtering == null) {
			if (other.filtering != null) return false;
		} else if (!filtering.equals(other.filtering)) return false;
		if (includes == null) {
			if (other.includes != null) return false;
		} else if (!includes.equals(other.includes)) return false;
		return true;
	}

	public int compareTo(Resource o) {
		if (o == null) {
			throw new NullPointerException();
		}
		int result = this.directory.compareTo(o.directory);
		if (result == 0) {
			result = this.filtering.compareTo(o.filtering);
		}
		return result;
	}

	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("directory", directory);
		tsc.append("filtering", filtering);
		tsc.append("includes", includes);
		return tsc.toString();
	}
}
