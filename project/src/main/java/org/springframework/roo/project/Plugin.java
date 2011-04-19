package org.springframework.roo.project;

import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Simplified immutable representation of a maven build plugin.
 * 
 * <p>
 * Structured after the model used by Maven.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class Plugin implements Comparable<Plugin> {
	private String groupId;
	private String artifactId;
	private String version;
	private Configuration configuration;
	private List<Dependency> dependencies = new ArrayList<Dependency>();
	private List<Execution> executions = new ArrayList<Execution>();

	/**
	 * Creates an immutable {@link Plugin}.
	 * 
	 * @param groupId the group ID (required)
	 * @param artifactId the artifact ID (required)
	 * @param version the version (required)
	 */
	public Plugin(String groupId, String artifactId, String version) {
		Assert.notNull(groupId, "Group ID required");
		Assert.notNull(artifactId, "Artifact ID required");
		Assert.notNull(version, "Version required");
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
	}
	/**
	 * Convenience constructor.
	 * 
	 * @param groupId the group ID (required)
	 * @param artifactId the artifact ID (required)
	 * @param version the version (required)
	 * @param configuration the configuration for this plugin (optional)
	 * @param dependencies the dependencies for this plugin (optional)
	 * @param executions the executions for this plugin (optional)
	 */
	public Plugin(String groupId, String artifactId, String version, Configuration configuration, List<Dependency> dependencies, List<Execution> executions) {
		Assert.hasText(groupId, "Group ID required");
		Assert.hasText(artifactId, "Artifact ID required");
		Assert.hasText(version, "Version required");
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
		this.configuration = configuration;
		this.dependencies = dependencies;
		this.executions = executions;
	}

	/**
	 * Convenience constructor when an XML element is available that represents a Maven <plugin>.
	 * 
	 * @param plugin to parse (required)
	 */
	public Plugin(Element plugin) {
		this.groupId = "org.apache.maven.plugins";
		if (plugin.getElementsByTagName("groupId").getLength() > 0) {
			this.groupId = plugin.getElementsByTagName("groupId").item(0).getTextContent();
		}

		this.artifactId = plugin.getElementsByTagName("artifactId").item(0).getTextContent();

		NodeList versionElements = plugin.getElementsByTagName("version");
		if (versionElements.getLength() > 0) {
			this.version = plugin.getElementsByTagName("version").item(0).getTextContent();
		} else {
			this.version = "";
		}

		// Parsing for configuration
		Element configuration = XmlUtils.findFirstElement("configuration", plugin);
		if (configuration != null) {
			this.configuration = new Configuration(configuration);
		}

		// Parsing for executions
		List<Element> executionList = XmlUtils.findElements("executions/execution", plugin);
		if (executionList.size() > 0) {
			for (Element execution : executionList) {
				Element executionId = XmlUtils.findFirstElement("id", execution);
				String id = "";
				if (executionId != null) {
					id = executionId.getTextContent();
				}
				Element executionPhase = XmlUtils.findFirstElement("phase", execution);
				String phase = "";
				if (executionPhase != null) {
					phase = executionPhase.getTextContent();
				}
				List<String> goals = new ArrayList<String>();
				List<Element> goalList = XmlUtils.findElements("goals/goal", execution);
				if (goalList.size() > 0) {
					for (Element goal : goalList) {
						goals.add(goal.getTextContent());
					}
				}
				executions.add(new Execution(id, phase, goals.toArray(new String[] {})));
			}
		}

		// Parsing for dependencies
		List<Element> dependencyList = XmlUtils.findElements("dependencies/dependency", plugin);
		if (dependencyList.size() > 0) {
			for (Element dependency : dependencyList) {
				Element dependencyGroupId = XmlUtils.findFirstElement("groupId", dependency);
				String groupId = "";
				if (dependencyGroupId != null) {
					groupId = dependencyGroupId.getTextContent();
				}

				Element dependencyArtifactId = XmlUtils.findFirstElement("artifactId", dependency);
				String artifactId = "";
				if (dependencyArtifactId != null) {
					artifactId = dependencyArtifactId.getTextContent();
				}

				Element dependencyVersion = XmlUtils.findFirstElement("version", dependency);
				String version = "";
				if (dependencyVersion != null) {
					version = dependencyVersion.getTextContent();
				}

				Dependency dependencyElement = new Dependency(groupId, artifactId, version);

				// Parsing for exclusions
				List<Element> exclusionList = XmlUtils.findElements("exclusions/exclusion", dependency);
				if (exclusionList.size() > 0) {
					for (Element exclusion : exclusionList) {
						Element exclusionElement = XmlUtils.findFirstElement("groupId", exclusion);
						String exclusionId = "";
						if (exclusionElement != null) {
							exclusionId = exclusionElement.getTextContent();
						}
						Element exclusionArtifactE = XmlUtils.findFirstElement("artifactId", exclusion);
						String exclusionArtifactId = "";
						if (exclusionArtifactE != null) {
							exclusionArtifactId = exclusionArtifactE.getTextContent();
						}
						if (!(exclusionArtifactId.length() < 1) && !(exclusionId.length() < 1)) {
							dependencyElement.getExclusions().add(new Dependency(exclusionId, exclusionArtifactId, "ignored"));
						}
					}
				}

				this.dependencies.add(dependencyElement);
			}
		}
	}

	public String getGroupId() {
		return groupId;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public String getVersion() {
		return version;
	}

	public Configuration getConfiguration() {
		return configuration;
	}

	public List<Dependency> getDependencies() {
		return dependencies;
	}

	public List<Execution> getExecutions() {
		return executions;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((groupId == null) ? 0 : groupId.hashCode());
		result = prime * result + ((artifactId == null) ? 0 : artifactId.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		result = prime * result + ((configuration == null) ? 0 : configuration.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		return obj != null && obj instanceof Plugin && this.compareTo((Plugin) obj) == 0;
	}

 	public int compareTo(Plugin o) {
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
		if (result == 0 && configuration != null && o.configuration != null) {
			result = configuration.compareTo(o.configuration);
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
		if (configuration != null) {
			tsc.append("configuration", configuration);
		}
		return tsc.toString();
	}
}
