package org.springframework.roo.project;

import java.util.ArrayList;
import java.util.Collection;
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
 * <p>
 * According to the Maven docs, "the minimal set of information for matching a dependency reference 
 * against a dependencyManagement section is actually {groupId, artifactId, type, classifier}"; see
 * http://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#Dependency_Scope
 * 
 * @author Ben Alex
 * @author Stefan Schmidt
 * @author Alan Stewart
 * @author Andrew Swan
 * @since 1.0
 */
public class Dependency implements Comparable<Dependency> {

	// Fields
	// -- Identifying
	private final String groupId;
	private final String artifactId;
	private final DependencyType type;
	private final String classifier;
	// -- Non-identifying
	private final DependencyScope scope;
	private final List<Dependency> exclusions = new ArrayList<Dependency>();
	private final String version;
	private final String systemPath;

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
		XmlUtils.assertElementLegal(artifactId);
		XmlUtils.assertElementLegal(groupId);
		Assert.notNull(scope, "Dependency scope required");
		Assert.notNull(type, "Dependency type required");
		Assert.notNull(version, "Version required");
		this.artifactId = artifactId;
		this.classifier = classifier;
		this.groupId = groupId;
		this.scope = scope;
		this.systemPath = null;
		this.type = type;
		this.version = version;
	}

	/**
	 * Constructs a JAR dependency with the given scope.
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
	 * Constructs a compile-scoped JAR dependency.
	 * 
	 * @param groupId the group ID (required)
	 * @param artifactId the artifact ID (required)
	 * @param version the version (required)
	 */
	public Dependency(final String groupId, final String artifactId, final String version) {
		this(groupId, artifactId, version, DependencyType.JAR, DependencyScope.COMPILE);
	}

	/**
	 * Constructs a compile-scoped JAR dependency with optional exclusions.
	 * 
	 * @param groupId the group ID (required)
	 * @param artifactId the artifact ID (required)
	 * @param version the version ID (required)
	 * @param exclusions the exclusions for this dependency (can be null)
	 */
	public Dependency(final String groupId, final String artifactId, final String version, final Collection<? extends Dependency> exclusions) {
		this(groupId, artifactId, version, DependencyType.JAR, DependencyScope.COMPILE);
		if (exclusions != null) {
			this.exclusions.addAll(exclusions);
		}
	}

	/**
	 * Constructs a {@link Dependency} from a Maven-style &lt;dependency&gt; element.
	 * 
	 * @param dependency to parse (required)
	 */
	public Dependency(final Element dependency) {
		// Test if it has Maven format
		if (dependency.hasChildNodes() && dependency.getElementsByTagName("artifactId").getLength() > 0) {
			this.groupId = dependency.getElementsByTagName("groupId").item(0).getTextContent().trim();
			this.artifactId = dependency.getElementsByTagName("artifactId").item(0).getTextContent().trim();

			final NodeList versionElements = dependency.getElementsByTagName("version");
			if (versionElements.getLength() > 0) {
				version = versionElements.item(0).getTextContent();
			} else {
				version = "";
			}

			// POM attributes supported in Maven 3.1
			this.type = DependencyType.getType(dependency);

			// POM attributes supported in Maven 3.1
			this.scope = DependencyScope.getScope(dependency);
			if (scope == DependencyScope.SYSTEM) {
				if (XmlUtils.findFirstElement("systemPath", dependency) != null) {
					final String path = XmlUtils.findFirstElement("systemPath", dependency).getTextContent().trim();
					systemPath = path;
				} else {
					throw new IllegalArgumentException("Missing <systemPath> declaration for system scope");
				}
			} else {
				this.systemPath = null;
			}
			
			this.classifier = XmlUtils.getChildTextContent(dependency, "classifier");
			
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
			artifactId = dependency.getAttribute("name");
			classifier = dependency.getAttribute("classifier");
			groupId = dependency.getAttribute("org");
			scope = DependencyScope.COMPILE;
			systemPath = null;
			type = DependencyType.JAR;
			version = dependency.getAttribute("rev");
			// TODO: Implement exclusions parser for IVY format
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
		result = prime * result + ((classifier == null) ? 0 : classifier.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		return obj instanceof Dependency && this.compareTo((Dependency) obj) == 0;
	}

	public int compareTo(final Dependency o) {
		if (o == null) {
			throw new NullPointerException();
		}
		// We omit the version field as it's not part of a Dependency's identity
		int result = groupId.compareTo(o.groupId);
		if (result == 0) {
			result = artifactId.compareTo(o.artifactId);
		}
		if (result == 0) {
			result = StringUtils.trimToEmpty(classifier).compareTo(StringUtils.trimToEmpty(o.classifier));
		}
		if (result == 0 && type != null) {
			result = type.compareTo(o.type);
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

		// Keep the XML short, we don't need "compile" given it's the default
		if (this.scope != null && this.scope != DependencyScope.COMPILE) {
			dependencyElement.appendChild(XmlUtils.createTextElement(document, "scope", this.scope.toString().toLowerCase()));
			if (this.scope == DependencyScope.SYSTEM && StringUtils.hasText(this.systemPath)) {
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

	/**
	 * Indicates whether the given {@link Dependency} has the same Maven
	 * coordinates as this one; this is not necessarily the same as calling
	 * {@link #equals(Object)}, which may compare more fields beyond the basic
	 * coordinates.
	 * 
	 * @param dependency the dependency to check (can be <code>null</code>)
	 * @return <code>false</code> if any coordinates are different
	 */
	public boolean hasSameCoordinates(final Dependency dependency) {
		return dependency != null
			&& dependency.groupId.equals(groupId)
			&& dependency.artifactId.equals(artifactId)
			&& dependency.type == type
			&& StringUtils.trimToEmpty(dependency.classifier).equals(StringUtils.trimToEmpty(classifier));
	}
}
