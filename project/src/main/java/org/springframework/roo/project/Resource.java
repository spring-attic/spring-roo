package org.springframework.roo.project;

import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;
import org.springframework.roo.support.util.XmlUtils;
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
	private Path directory;
	private Boolean filtering;
	private List<String> includes = new ArrayList<String>();

	/**
	 * Creates an immutable {@link Resource}.
	 * 
	 * @param directory the {@link Path directory} (required)
	 * @param filtering whether filtering should occur
	 */
	public Resource(Path directory, Boolean filtering) {
		Assert.notNull(directory, "Directory required");
		this.directory = directory;
		this.filtering = filtering;
	}

	/**
	 * Creates an immutable {@link Resource}.
	 * 
	 * @param directory the {@link Path directory} (required)
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
		Assert.notNull(directoryElement, "directory element required");
		directory = new Path(directoryElement.getTextContent());

		Element filteringElement = XmlUtils.findFirstElement("filtering", resource);
		if (filteringElement != null) {
			filtering = Boolean.valueOf(filteringElement.getTextContent());
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
		return getSimpleDescription().hashCode();
	}
	
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Resource other = (Resource) obj;
		return getSimpleDescription().equals(other.getSimpleDescription());
	}

	public int compareTo(Resource o) {
		if (o == null) {
			throw new NullPointerException();
		}
		return getSimpleDescription().compareTo(o.getSimpleDescription());
	}
	
	public String getSimpleDescription() {
		StringBuilder builder = new StringBuilder();
		builder.append("directory ").append(directory.getName());
		if (filtering != null) {
			builder.append(", filtering ").append(filtering.toString());
		}
		if (includes != null && !includes.isEmpty()) {
		builder.append(", includes ").append(StringUtils.collectionToCommaDelimitedString(includes));
		}
		return builder.toString();
	}

	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("directory", directory);
		tsc.append("filtering", filtering);
		tsc.append("includes", includes);
		return tsc.toString();
	}
}
