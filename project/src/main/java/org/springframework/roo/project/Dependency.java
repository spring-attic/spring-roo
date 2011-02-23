package org.springframework.roo.project;

import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.XmlUtils;
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
	private String groupId;
	private String artifactId;
	private String version;
	private DependencyType type;
	private DependencyScope scope;
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
	 */
	public Dependency(String groupId, String artifactId, String version, DependencyType type, DependencyScope scope) {
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
	}

	/**
	 * Convenience constructor for producing a JAR dependency.
	 * 
	 * @param groupId the group ID (required)
	 * @param artifactId the artifact ID (required)
	 * @param version the version (required)
	 */
	public Dependency(String groupId, String artifactId, String version) {
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
	public Dependency(String groupId, String artifactId, String version, List<Dependency> exclusions) {
		this(groupId, artifactId, version, DependencyType.JAR, DependencyScope.COMPILE);
		this.exclusions = exclusions;
	}

	/**
	 * Convenience constructor when an XML element is available that represents a Maven <dependency>.
	 * 
	 * @param dependency to parse (required)
	 */
	public Dependency(Element dependency) {
		// Test if it has Maven format
		if (dependency.hasChildNodes() && dependency.getElementsByTagName("artifactId").getLength() > 0) {
			this.groupId = "org.apache.maven.plugins";
			if (dependency.getElementsByTagName("groupId").getLength() > 0) {
				this.groupId = dependency.getElementsByTagName("groupId").item(0).getTextContent();
			}

			this.artifactId = dependency.getElementsByTagName("artifactId").item(0).getTextContent();

			NodeList versionElements = dependency.getElementsByTagName("version");
			if (versionElements.getLength() > 0) {
				this.version = dependency.getElementsByTagName("version").item(0).getTextContent();
			} else {
				this.version = "";
			}

			// POM attributes supported in Maven 3.1
			this.type = DependencyType.JAR;
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
					this.type = DependencyType.ZIP;
				} else {
					this.type = DependencyType.OTHER;
				}
			}

			// POM attributes supported in Maven 3.1
			this.scope = DependencyScope.COMPILE;
			if (XmlUtils.findFirstElement("scope", dependency) != null || dependency.hasAttribute("scope")) {
				String s;
				if (dependency.hasAttribute("scope")) {
					s = dependency.getAttribute("scope");
				} else {
					s = XmlUtils.findFirstElement("scope", dependency).getTextContent().trim().toUpperCase();
				}
				try {
					this.scope = DependencyScope.valueOf(s);
				} catch (IllegalArgumentException e) {
					throw new IllegalArgumentException("Invalid dependency scope: " + s);
				}
			}
			if (this.scope == DependencyScope.SYSTEM) {
				if (XmlUtils.findFirstElement("systemPath", dependency) != null) {
					String path = XmlUtils.findFirstElement("systemPath", dependency).getTextContent().trim();
					this.systemPath = path;
				} else {
					throw new IllegalArgumentException("Missing <systemPath> declaraton for system scope");
				}
			}
			// Parsing for exclusions
			List<Element> exclusionList = XmlUtils.findElements("exclusions/exclusion", dependency);
			if (exclusionList.size() > 0) {
				for (Element exclusion : exclusionList) {
					Element exclusionE = XmlUtils.findFirstElement("groupId", exclusion);
					String exclusionId = "";
					if (exclusionE != null) {
						exclusionId = exclusionE.getTextContent();
					}
					Element exclusionArtifactE = XmlUtils.findFirstElement("artifactId", exclusion);
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
			this.groupId = dependency.getAttribute("org");
			this.artifactId = dependency.getAttribute("name");
			this.version = dependency.getAttribute("rev");
			this.type = DependencyType.JAR;
			this.scope = DependencyScope.COMPILE;
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

	public String getVersionId() {
		return version;
	}

	public DependencyType getType() {
		return type;
	}

	public DependencyScope getScope() {
		return scope;
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

	public int hashCode() {
		return 11 * this.groupId.hashCode() * this.artifactId.hashCode() * this.version.hashCode() * (this.type != null ? this.type.hashCode() : 1) * (this.scope != null ? this.scope.hashCode() : 1);
	}

	public boolean equals(Object obj) {
		return obj != null && obj instanceof Dependency && this.compareTo((Dependency) obj) == 0;
	}

	public int compareTo(Dependency o) {
		if (o == null) {
			throw new NullPointerException();
		}
		int result = this.groupId.compareTo(o.groupId);
		if (result == 0) {
			result = this.artifactId.compareTo(o.artifactId);
		}
		if (result == 0) {
			result = this.version.compareTo(o.version);
		}
		if (result == 0 && this.type != null) {
			result = this.type.compareTo(o.type);
		}
		if (result == 0 && this.scope != null) {
			result = this.scope.compareTo(o.scope);
		}
		return result;
	}

	/**
	 * @return a simple description, as would be used for console output
	 */
	public String getSimpleDescription() {
		return groupId + ":" + artifactId + ":" + version;
	}

	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("groupId", groupId);
		tsc.append("artifactId", artifactId);
		tsc.append("version", version);
		tsc.append("type", type);
		tsc.append("scope", scope);
		return tsc.toString();
	}
}
