package org.springframework.roo.project;

import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Simplified immutable representation of a dependency.
 * 
 * <p>
 * Structured after the model used by Maven and Ivy. This may be replaced in a future release with a more OSGi-centric model.
 * 
 * @author Ben Alex
 * @author Stefan Schmidt
 * @author Alan Stewart
 * @since 1.0
 */
public class Dependency implements Comparable<Dependency> {
	
	// Fields
	private String groupId;
	private String artifactId;
	private String version;
	private DependencyType type;
	private DependencyScope scope;
	private String classifier;
	private List<Dependency> exclusions = new ArrayList<Dependency>();
	private String systemPath;

	/**
	 * Creates an immutable {@link Dependency}.
	 * 
	 * @param groupId the group ID (required)
	 * @param artifactId the artifact ID (required)
	 * @param version the version ID (required)
	 * @param type the dependency type (required)
	 * @param scope the dependency scope (required)
	 * @param classifier the dependency classifier (required)
	 */
	public Dependency(final String groupId, final String artifactId, final String version, final DependencyType type, final DependencyScope scope, final String classifier) {
		XmlUtils.assertElementLegal(groupId);
		XmlUtils.assertElementLegal(artifactId);
		Assert.notNull(version, "Version required");
		Assert.notNull(type, "Dependency type required");
		Assert.notNull(scope, "Dependency scope required");
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
		this.type = type;
		this.scope = scope;
		this.classifier = classifier;
	}

	/**
	 * Creates an immutable {@link Dependency}.
	 * 
	 * @param groupId the group ID (required)
	 * @param artifactId the artifact ID (required)
	 * @param version the version ID (required)
	 * @param type the dependency type (required)
	 * @param scope the dependency scope (required)
	 */
	public Dependency(final String groupId, final String artifactId, final String version, final DependencyType type, final DependencyScope scope) {
		this(groupId, artifactId, version, DependencyType.JAR, scope, "");
	}

	/**
	 * Convenience constructor for producing a JAR dependency.
	 * 
	 * @param groupId the group ID (required)
	 * @param artifactId the artifact ID (required)
	 * @param version the version (required)
	 */
	public Dependency(final String groupId, final String artifactId, final String version) {
		this(groupId, artifactId, version, DependencyType.JAR, DependencyScope.COMPILE);
	}

	/**
	 * Convenience constructor for producing a JAR dependency.
	 * 
	 * @param groupId the group ID (required)
	 * @param artifactId the artifact ID (required)
	 * @param version the version ID (required)
	 * @param exclusions the exclusions for this dependency
	 */
	public Dependency(final String groupId, final String artifactId, final String version, final List<Dependency> exclusions) {
		this(groupId, artifactId, version, DependencyType.JAR, DependencyScope.COMPILE);
		this.exclusions = exclusions;
	}

	/**
	 * Convenience constructor when an XML element is available that represents a Maven <dependency>.
	 * 
	 * @param dependency to parse (required)
	 */
	public Dependency(final Element dependency) {
		// Test if it has Maven format
		if (dependency.hasChildNodes() && dependency.getElementsByTagName("artifactId").getLength() > 0) {
			groupId = "org.apache.maven.plugins";
			if (dependency.getElementsByTagName("groupId").getLength() > 0) {
				groupId = dependency.getElementsByTagName("groupId").item(0).getTextContent();
			}

			this.artifactId = dependency.getElementsByTagName("artifactId").item(0).getTextContent();

			final NodeList versionElements = dependency.getElementsByTagName("version");
			if (versionElements.getLength() > 0) {
				version = dependency.getElementsByTagName("version").item(0).getTextContent();
			} else {
				version = "";
			}

			// POM attributes supported in Maven 3.1
			type = DependencyType.JAR;
			if (XmlUtils.findFirstElement("type", dependency) != null || dependency.hasAttribute("type")) {
				String t;
				if (dependency.hasAttribute("type")) {
					t = dependency.getAttribute("type");
				} else {
					t = XmlUtils.findFirstElement("type", dependency).getTextContent().trim().toUpperCase();
				}
				if (t.equals("JAR")) {
					// Already a JAR, so no need to reassign
				} else if (t.equals("ZIP")) {
					type = DependencyType.ZIP;
				} else {
					type = DependencyType.OTHER;
				}
			}

			// POM attributes supported in Maven 3.1
			scope = DependencyScope.COMPILE;
			if (XmlUtils.findFirstElement("scope", dependency) != null || dependency.hasAttribute("scope")) {
				String s;
				if (dependency.hasAttribute("scope")) {
					s = dependency.getAttribute("scope");
				} else {
					s = XmlUtils.findFirstElement("scope", dependency).getTextContent().trim().toUpperCase();
				}
				try {
					scope = DependencyScope.valueOf(s);
				} catch (final IllegalArgumentException e) {
					throw new IllegalArgumentException("Invalid dependency scope: " + s);
				}
			}
			if (scope == DependencyScope.SYSTEM) {
				if (XmlUtils.findFirstElement("systemPath", dependency) != null) {
					final String path = XmlUtils.findFirstElement("systemPath", dependency).getTextContent().trim();
					systemPath = path;
				} else {
					throw new IllegalArgumentException("Missing <systemPath> declaraton for system scope");
				}
			}
			final NodeList classifierElements = dependency.getElementsByTagName("classifier");
			if (classifierElements.getLength() > 0) {
				classifier = dependency.getElementsByTagName("classifier").item(0).getTextContent();
			} else {
				classifier = "";
			}
			// Parsing for exclusions
			final List<Element> exclusionList = XmlUtils.findElements("exclusions/exclusion", dependency);
			if (exclusionList.size() > 0) {
				for (final Element exclusion : exclusionList) {
					final Element exclusionE = XmlUtils.findFirstElement("groupId", exclusion);
					String exclusionId = "";
					if (exclusionE != null) {
						exclusionId = exclusionE.getTextContent();
					}
					final Element exclusionArtifactE = XmlUtils.findFirstElement("artifactId", exclusion);
					String exclusionArtifactId = "";
					if (exclusionArtifactE != null) {
						exclusionArtifactId = exclusionArtifactE.getTextContent();
					}
					if (!(exclusionArtifactId.length() < 1) && !(exclusionId.length() < 1)) {
						this.exclusions.add(new Dependency(exclusionId, exclusionArtifactId, "ignored"));
					}
				}
			}
		}
		// Otherwise test for Ivy format
		else if (dependency.hasAttribute("org") && dependency.hasAttribute("name") && dependency.hasAttribute("rev")) {
			groupId = dependency.getAttribute("org");
			artifactId = dependency.getAttribute("name");
			version = dependency.getAttribute("rev");
			type = DependencyType.JAR;
			scope = DependencyScope.COMPILE;
			// TODO: implement exclusions parser for IVY format
		} else {
			throw new IllegalStateException("Dependency XML format not supported or is missing a mandatory node ('" + dependency + "')");
		}
	}

	public String getGroupId() {
		return groupId;
	}

	public String getArtifactId() {
		return artifactId;
	}

	@Deprecated
	public String getVersionId() {
		return version;
	}

	public String getVersion() {
		return version;
	}

	public DependencyType getType() {
		return type;
	}

	public DependencyScope getScope() {
		return scope;
	}

	public String getClassifier() {
		return classifier;
	}

	public String getSystemPath() {
		return systemPath;
	}

	/**
	 * @return list of exclusions (never null)
	 */
	public List<Dependency> getExclusions() {
		return exclusions;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((artifactId == null) ? 0 : artifactId.hashCode());
		result = prime * result + ((groupId == null) ? 0 : groupId.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		result = prime * result + ((classifier == null) ? 0 : classifier.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		return obj != null && obj instanceof Dependency && this.compareTo((Dependency) obj) == 0;
	}

	public int compareTo(final Dependency o) {
		if (o == null) {
			throw new NullPointerException();
		}
		int result = groupId.compareTo(o.groupId);
		if (result == 0) {
			result = artifactId.compareTo(o.artifactId);
		}
		if (result == 0) {
			result = version.compareTo(o.version);
		}
		if (result == 0 && classifier != null) {
			result = classifier.compareTo(o.classifier);
		}
		return result;
	}

	/**
	 * @return a simple description, as would be used for console output
	 */
	public String getSimpleDescription() {
		return groupId + ":" + artifactId + ":" + version + (StringUtils.hasText(classifier) ? ":" + classifier : "");
	}

	@Override
	public String toString() {
		final ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("groupId", groupId);
		tsc.append("artifactId", artifactId);
		tsc.append("version", version);
		tsc.append("type", type);
		tsc.append("scope", scope);
		if (classifier != null) {
			tsc.append("classifier", classifier);
		}
		return tsc.toString();
	}

	/**
	 * Adds the given exclusion to this dependency
	 * 
	 * @param exclusionGroupId the groupId of the dependency to exclude (required)
	 * @param exclusionArtifactId the artifactId of the dependency to exclude (required)
	 */
	public void addExclusion(final String exclusionGroupId, final String exclusionArtifactId) {
		Assert.hasText(exclusionGroupId, "Excluded groupId required");
		Assert.hasText(exclusionArtifactId, "Excluded artifactId required");
		this.exclusions.add(new Dependency(exclusionGroupId, exclusionArtifactId, "ignored"));
	}
	
	/**
	 * Returns the XML element for this dependency
	 * 
	 * @param document the parent XML document
	 * @return a non-<code>null</code> element
	 * @since 1.2.0
	 */
	public Element getElement(final Document document) {
		final Element dependencyElement = document.createElement("dependency");
		dependencyElement.appendChild(XmlUtils.createTextElement(document, "groupId", this.groupId));
		dependencyElement.appendChild(XmlUtils.createTextElement(document, "artifactId", this.artifactId));
		dependencyElement.appendChild(XmlUtils.createTextElement(document, "version", this.version));

		if (this.type != null && this.type != DependencyType.JAR) {
			// Keep the XML short, we don't need "JAR" given it's the default
			final Element typeElement = XmlUtils.createTextElement(document, "type", this.type.toString().toLowerCase());
			dependencyElement.appendChild(typeElement);
		}

		if (this.scope != null) {
			// Keep the XML short, we don't need "compile" given it's the default
			if (!DependencyScope.COMPILE.equals(this.scope)) {
				dependencyElement.appendChild(XmlUtils.createTextElement(document, "scope", this.scope.toString().toLowerCase()));
			}
			if (DependencyScope.SYSTEM.equals(this.scope) && this.systemPath != null) {
				dependencyElement.appendChild(XmlUtils.createTextElement(document, "systemPath", this.systemPath));
			}
		}

		if (StringUtils.hasText(this.classifier)) {
			dependencyElement.appendChild(XmlUtils.createTextElement(document, "classifier", this.classifier));
		}

		// Add exclusions if any
		if (!this.exclusions.isEmpty()) {
			final Element exclusionsElement = XmlUtils.createChildElement("exclusions", dependencyElement, document);
			for (final Dependency exclusion : this.exclusions) {
				final Element exclusionElement = XmlUtils.createChildElement("exclusion", exclusionsElement, document);
				exclusionElement.appendChild(XmlUtils.createTextElement(document, "groupId", exclusion.getGroupId()));
				exclusionElement.appendChild(XmlUtils.createTextElement(document, "artifactId", exclusion.getArtifactId()));
			}
		}
		
		return dependencyElement;
	}
}
