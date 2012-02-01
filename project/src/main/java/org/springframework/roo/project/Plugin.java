package org.springframework.roo.project;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.support.util.CollectionUtils;
import org.springframework.roo.support.util.DomUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Simplified immutable representation of a maven build plugin.
 * <p>
 * Structured after the model used by Maven.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class Plugin implements Comparable<Plugin> {

    /**
     * The Maven groupId that will be assigned to a plugin if one is not
     * provided
     */
    public static final String DEFAULT_GROUP_ID = "org.apache.maven.plugins";

    /**
     * Parses the given plugin XML element for the plugin's Maven artifactId
     * 
     * @param plugin the XML element to parse (required)
     * @return a non-blank id
     */
    private static String getArtifactId(final Element plugin) {
        return plugin.getElementsByTagName("artifactId").item(0)
                .getTextContent();
    }

    /**
     * Parses the configuration of the given plugin (global, not
     * execution-scoped)
     * 
     * @param plugin the XML element to parse (required)
     * @return <code>null</code> if there isn't one
     */
    private static Configuration getConfiguration(final Element plugin) {
        return Configuration.getInstance(XmlUtils.findFirstElement(
                "configuration", plugin));
    }

    /**
     * Parses the given XML plugin element for the plugin's dependencies
     * 
     * @param plugin the XML element to parse (required)
     * @return a non-<code>null</code> list
     */
    private static List<Dependency> getDependencies(final Element plugin) {
        final List<Dependency> dependencies = new ArrayList<Dependency>();
        for (final Element dependencyElement : XmlUtils.findElements(
                "dependencies/dependency", plugin)) {
            // groupId
            final Element groupIdElement = XmlUtils.findFirstElement("groupId",
                    dependencyElement);
            final String groupId = DomUtils.getTextContent(groupIdElement, "");

            // artifactId
            final Element artifactIdElement = XmlUtils.findFirstElement(
                    "artifactId", dependencyElement);
            final String artifactId = DomUtils.getTextContent(
                    artifactIdElement, "");

            // version
            final Element versionElement = XmlUtils.findFirstElement("version",
                    dependencyElement);
            final String version = DomUtils.getTextContent(versionElement, "");

            final Dependency dependency = new Dependency(groupId, artifactId,
                    version);

            // Parse any exclusions
            for (final Element exclusion : XmlUtils.findElements(
                    "exclusions/exclusion", dependencyElement)) {
                // groupId
                final Element exclusionGroupIdElement = XmlUtils
                        .findFirstElement("groupId", exclusion);
                final String exclusionGroupId = DomUtils.getTextContent(
                        exclusionGroupIdElement, "");

                // artifactId
                final Element exclusionArtifactIdElement = XmlUtils
                        .findFirstElement("artifactId", exclusion);
                final String exclusionArtifactId = DomUtils.getTextContent(
                        exclusionArtifactIdElement, "");

                if (StringUtils.isNotBlank(exclusionGroupId)
                        && StringUtils.isNotBlank(exclusionArtifactId)) {
                    dependency.addExclusion(exclusionGroupId,
                            exclusionArtifactId);
                }
            }
            dependencies.add(dependency);
        }
        return dependencies;
    }

    /**
     * Parses the given XML plugin element for the plugin's executions
     * 
     * @param plugin the XML element to parse (required)
     * @return a non-<code>null</code> list
     */
    private static List<Execution> getExecutions(final Element plugin) {
        final List<Execution> executions = new ArrayList<Execution>();
        // Loop through the "execution" elements in the plugin element
        for (final Element execution : XmlUtils.findElements(
                "executions/execution", plugin)) {
            final Element idElement = XmlUtils
                    .findFirstElement("id", execution);
            final String id = DomUtils.getTextContent(idElement, "");
            final Element phaseElement = XmlUtils.findFirstElement("phase",
                    execution);
            final String phase = DomUtils.getTextContent(phaseElement, "");
            final List<String> goals = new ArrayList<String>();
            for (final Element goalElement : XmlUtils.findElements(
                    "goals/goal", execution)) {
                goals.add(goalElement.getTextContent());
            }
            final Configuration configuration = Configuration
                    .getInstance(XmlUtils.findFirstElement("configuration",
                            execution));
            executions.add(new Execution(id, phase, configuration, goals
                    .toArray(new String[goals.size()])));
        }
        return executions;
    }

    /**
     * Parses the plugin's Maven groupId from the given element
     * 
     * @param plugin the element to parse (required)
     * @return a non-blank groupId
     */
    public static String getGroupId(final Element plugin) {
        if (plugin.getElementsByTagName("groupId").getLength() > 0) {
            return plugin.getElementsByTagName("groupId").item(0)
                    .getTextContent();
        }
        return DEFAULT_GROUP_ID;
    }

    /**
     * Parses the plugin's version number from the given XML element
     * 
     * @param plugin the element to parse (required)
     * @return a non-<code>null</code> version number (might be empty)
     */
    private static String getVersion(final Element plugin) {
        final NodeList versionElements = plugin.getElementsByTagName("version");
        if (versionElements.getLength() > 0) {
            return versionElements.item(0).getTextContent();
        }
        return "";
    }

    private final Configuration configuration;
    private final List<Dependency> dependencies = new ArrayList<Dependency>();
    private final List<Execution> executions = new ArrayList<Execution>();
    private final GAV gav;

    /**
     * Constructor from a POM-style XML element that defines a Maven <plugin>.
     * 
     * @param plugin the XML element to parse (required)
     */
    public Plugin(final Element plugin) {
        this(getGroupId(plugin), getArtifactId(plugin), getVersion(plugin),
                getConfiguration(plugin), getDependencies(plugin),
                getExecutions(plugin));
    }

    /**
     * Constructor that takes the minimal Maven artifact coordinates.
     * 
     * @param groupId the group ID (required)
     * @param artifactId the artifact ID (required)
     * @param version the version (required)
     */
    public Plugin(final String groupId, final String artifactId,
            final String version) {
        this(groupId, artifactId, version, null, null, null);
    }

    /**
     * Constructor that allows all fields to be set.
     * 
     * @param groupId the group ID (required)
     * @param artifactId the artifact ID (required)
     * @param version the version (required)
     * @param configuration the configuration for this plugin (optional)
     * @param dependencies the dependencies for this plugin (can be
     *            <code>null</code>)
     * @param executions the executions for this plugin (can be
     *            <code>null</code>)
     */
    public Plugin(final String groupId, final String artifactId,
            final String version, final Configuration configuration,
            final Collection<? extends Dependency> dependencies,
            final Collection<? extends Execution> executions) {
        Validate.notNull(groupId, "Group ID required");
        Validate.notNull(artifactId, "Artifact ID required");
        Validate.notNull(version, "Version required");
        gav = new GAV(groupId, artifactId, version);
        this.configuration = configuration;
        // Defensively copy the given nullable collections
        CollectionUtils.populate(this.dependencies, dependencies);
        CollectionUtils.populate(this.executions, executions);
    }

    public int compareTo(final Plugin o) {
        if (o == null) {
            throw new NullPointerException();
        }
        int result = gav.compareTo(o.getGAV());
        if (result == 0 && configuration != null && o.configuration != null) {
            result = configuration.compareTo(o.configuration);
        }
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof Plugin && compareTo((Plugin) obj) == 0;
    }

    public String getArtifactId() {
        return gav.getArtifactId();
    }

    /**
     * Returns the top-level configuration of this plugin, if any. Note that
     * individual {@link Execution}s may have their own {@link Configuration}s
     * instead of or in addition to this configuration.
     * 
     * @return <code>null</code> if none exists
     */
    public Configuration getConfiguration() {
        return configuration;
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }

    /**
     * Returns the {@link Element} to add to the given POM {@link Document} for
     * this plugin
     * 
     * @param plugin the plugin for which to create an XML Element (required)
     * @param document the document to which the element will belong (required)
     * @return a non-<code>null</code> element
     * @since 1.2.0
     */
    public Element getElement(final Document document) {
        final Element pluginElement = document.createElement("plugin");

        // Basic coordinates
        pluginElement.appendChild(XmlUtils.createTextElement(document,
                "groupId", getGroupId()));
        pluginElement.appendChild(XmlUtils.createTextElement(document,
                "artifactId", getArtifactId()));
        pluginElement.appendChild(XmlUtils.createTextElement(document,
                "version", getVersion()));

        // Configuration
        if (configuration != null) {
            final Node configuration = document.importNode(
                    this.configuration.getConfiguration(), true);
            pluginElement.appendChild(configuration);
        }

        // Executions
        if (!executions.isEmpty()) {
            final Element executionsElement = DomUtils.createChildElement(
                    "executions", pluginElement, document);
            for (final Execution execution : executions) {
                executionsElement.appendChild(execution.getElement(document));
            }
        }

        // Dependencies
        if (!dependencies.isEmpty()) {
            final Element dependenciesElement = DomUtils.createChildElement(
                    "dependencies", pluginElement, document);
            for (final Dependency dependency : dependencies) {
                dependenciesElement
                        .appendChild(dependency.getElement(document));
            }
        }

        return pluginElement;
    }

    public List<Execution> getExecutions() {
        return executions;
    }

    /**
     * Returns the Maven-style coordinates of this plugin
     * 
     * @return a non-<code>null</code> set of coordinates
     */
    public GAV getGAV() {
        return gav;
    }

    /**
     * Returns this plugin's groupId.
     * 
     * @return
     */
    public String getGroupId() {
        return gav.getGroupId();
    }

    /**
     * @return a simple description, as would be used for console output
     */
    public String getSimpleDescription() {
        return gav.toString();
    }

    public String getVersion() {
        return gav.getVersion();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        final int result = prime * 1 + gav.hashCode();
        return prime * result
                + (configuration == null ? 0 : configuration.hashCode());
    }

    @Override
    public String toString() {
        final ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("gav", gav);
        if (configuration != null) {
            builder.append("configuration", configuration);
        }
        return builder.toString();
    }
}
