package org.springframework.roo.project;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.support.util.DomUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Simplified immutable representation of a dependency.
 * <p>
 * Structured after the model used by Maven and Ivy. This may be replaced in a
 * future release with a more OSGi-centric model.
 * <p>
 * According to the Maven docs, "the minimal set of information for matching a
 * dependency reference against a dependencyManagement section is actually
 * {groupId, artifactId, type, classifier}"; see
 * http://maven.apache.org/guides/introduction
 * /introduction-to-dependency-mechanism.html#Dependency_Scope
 * 
 * @author Ben Alex
 * @author Stefan Schmidt
 * @author Alan Stewart
 * @author Andrew Swan
 * @since 1.0
 */
public class Dependency implements Comparable<Dependency> {

    // Known dependency types in increasing containment order
    private static final List<String> TYPE_HIERARCHY = Arrays.asList("jar",
            "war", "ear", "pom");

    /**
     * Indicates whether one dependency type is at a higher logical level than
     * another.
     * 
     * @param type1 the first dependency type to compare (required)
     * @param type2 the second dependency type to compare (required)
     * @return <code>false</code> if they are at the same level or the first is
     *         at a lower level
     * @since 1.2.1
     */
    public static boolean isHigherLevel(final String type1, final String type2) {
        final int type1Index = TYPE_HIERARCHY.indexOf(type1.toLowerCase());
        final int type2Index = TYPE_HIERARCHY.indexOf(type2.toLowerCase());
        return type2Index >= 0 && type1Index > type2Index;
    }

    private final String artifactId;
    private final String classifier;
    private final List<Dependency> exclusions = new ArrayList<Dependency>();
    // -- Identifying
    private final String groupId;
    // -- Non-identifying
    private final DependencyScope scope;
    private final String systemPath;
    private final DependencyType type;
    private final String version;

    /**
     * Constructs a {@link Dependency} from a Maven-style &lt;dependency&gt;
     * element.
     * 
     * @param dependency to parse (required)
     */
    public Dependency(final Element dependency) {
        // Test if it has Maven format
        if (dependency.hasChildNodes()
                && dependency.getElementsByTagName("artifactId").getLength() > 0) {
            groupId = dependency.getElementsByTagName("groupId").item(0)
                    .getTextContent().trim();
            artifactId = dependency.getElementsByTagName("artifactId").item(0)
                    .getTextContent().trim();

            final NodeList versionElements = dependency
                    .getElementsByTagName("version");
            if (versionElements.getLength() > 0) {
                version = versionElements.item(0).getTextContent();
            }
            else {
                version = "";
            }

            // POM attributes supported in Maven 3.1
            type = DependencyType.getType(dependency);

            // POM attributes supported in Maven 3.1
            scope = DependencyScope.getScope(dependency);
            if (scope == DependencyScope.SYSTEM) {
                if (XmlUtils.findFirstElement("systemPath", dependency) != null) {
                    systemPath = XmlUtils
                            .findFirstElement("systemPath", dependency)
                            .getTextContent().trim();
                }
                else {
                    throw new IllegalArgumentException(
                            "Missing <systemPath> declaration for system scope");
                }
            }
            else {
                systemPath = null;
            }

            classifier = DomUtils.getChildTextContent(dependency, "classifier");

            // Parsing for exclusions
            final List<Element> exclusionList = XmlUtils.findElements(
                    "exclusions/exclusion", dependency);
            if (exclusionList.size() > 0) {
                for (final Element exclusion : exclusionList) {
                    final Element exclusionE = XmlUtils.findFirstElement(
                            "groupId", exclusion);
                    String exclusionId = "";
                    if (exclusionE != null) {
                        exclusionId = exclusionE.getTextContent();
                    }
                    final Element exclusionArtifactE = XmlUtils
                            .findFirstElement("artifactId", exclusion);
                    String exclusionArtifactId = "";
                    if (exclusionArtifactE != null) {
                        exclusionArtifactId = exclusionArtifactE
                                .getTextContent();
                    }
                    if (!(exclusionArtifactId.length() < 1)
                            && !(exclusionId.length() < 1)) {
                        exclusions.add(new Dependency(exclusionId,
                                exclusionArtifactId, "ignored"));
                    }
                }
            }
        }
        // Otherwise test for Ivy format
        else if (dependency.hasAttribute("org")
                && dependency.hasAttribute("name")
                && dependency.hasAttribute("rev")) {
            artifactId = dependency.getAttribute("name");
            classifier = dependency.getAttribute("classifier");
            groupId = dependency.getAttribute("org");
            scope = DependencyScope.COMPILE;
            systemPath = null;
            type = DependencyType.JAR;
            version = dependency.getAttribute("rev");
            // TODO: Implement exclusions parser for IVY format
        }
        else {
            throw new IllegalStateException(
                    "Dependency XML format not supported or is missing a mandatory node ('"
                            + dependency + "')");
        }
    }

    /**
     * Constructor for a dependency with the given attributes.
     * 
     * @param gav the coordinates to use (required)
     * @param type the dependency type (required)
     * @param scope the dependency scope (required)
     * @since 1.2.1
     */
    public Dependency(final GAV gav, final DependencyType type,
            final DependencyScope scope) {
        this(gav.getGroupId(), gav.getArtifactId(), gav.getVersion(), type,
                scope);
    }

    /**
     * Constructs a compile-scoped JAR dependency.
     * 
     * @param groupId the group ID (required)
     * @param artifactId the artifact ID (required)
     * @param version the version (required)
     */
    public Dependency(final String groupId, final String artifactId,
            final String version) {
        this(groupId, artifactId, version, DependencyType.JAR,
                DependencyScope.COMPILE);
    }

    /**
     * Constructs a compile-scoped JAR dependency with optional exclusions.
     * 
     * @param groupId the group ID (required)
     * @param artifactId the artifact ID (required)
     * @param version the version ID (required)
     * @param exclusions the exclusions for this dependency (can be null)
     */
    public Dependency(final String groupId, final String artifactId,
            final String version,
            final Collection<? extends Dependency> exclusions) {
        this(groupId, artifactId, version, DependencyType.JAR,
                DependencyScope.COMPILE);
        if (exclusions != null) {
            this.exclusions.addAll(exclusions);
        }
    }

    /**
     * Constructs a dependency with the given type and scope.
     * 
     * @param groupId the group ID (required)
     * @param artifactId the artifact ID (required)
     * @param version the version ID (required)
     * @param type the dependency type (required)
     * @param scope the dependency scope (required)
     */
    public Dependency(final String groupId, final String artifactId,
            final String version, final DependencyType type,
            final DependencyScope scope) {
        this(groupId, artifactId, version, type, scope, "");
    }

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
    public Dependency(final String groupId, final String artifactId,
            final String version, final DependencyType type,
            final DependencyScope scope, final String classifier) {
        XmlUtils.assertElementLegal(groupId);
        XmlUtils.assertElementLegal(artifactId);
        Validate.notBlank(version, "Version required");
        Validate.notNull(scope, "Dependency scope required");
        Validate.notNull(type, "Dependency type required");
        this.artifactId = artifactId;
        this.classifier = classifier;
        this.groupId = groupId;
        this.scope = scope;
        systemPath = null;
        this.type = type;
        this.version = version;
    }

    /**
     * Adds the given exclusion to this dependency
     * 
     * @param exclusionGroupId the groupId of the dependency to exclude
     *            (required)
     * @param exclusionArtifactId the artifactId of the dependency to exclude
     *            (required)
     */
    public void addExclusion(final String exclusionGroupId,
            final String exclusionArtifactId) {
        Validate.notBlank(exclusionGroupId, "Excluded groupId required");
        Validate.notBlank(exclusionArtifactId, "Excluded artifactId required");
        exclusions.add(new Dependency(exclusionGroupId, exclusionArtifactId,
                "ignored"));
    }

    /**
     * Compares this dependency's identifying coordinates (i.e. not the version)
     * to those of the given dependency
     * 
     * @param other the dependency being compared to (required)
     * @return see {@link Comparable#compareTo(Object)}
     */
    private int compareCoordinates(final Dependency other) {
        Validate.notNull(other, "Dependency being compared to cannot be null");
        int result = groupId.compareTo(other.getGroupId());
        if (result == 0) {
            result = artifactId.compareTo(other.getArtifactId());
        }
        if (result == 0) {
            result = StringUtils.stripToEmpty(classifier).compareTo(
                    StringUtils.stripToEmpty(other.getClassifier()));
        }
        if (result == 0 && type != null) {
            result = type.compareTo(other.getType());
        }
        return result;
    }

    public int compareTo(final Dependency o) {
        final int result = compareCoordinates(o);
        if (result != 0) {
            return result;
        }
        return version.compareTo(o.getVersion());
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof Dependency && compareTo((Dependency) obj) == 0;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getClassifier() {
        return classifier;
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
        dependencyElement.appendChild(XmlUtils.createTextElement(document,
                "groupId", groupId));
        dependencyElement.appendChild(XmlUtils.createTextElement(document,
                "artifactId", artifactId));
        dependencyElement.appendChild(XmlUtils.createTextElement(document,
                "version", version));

        if (type != null && type != DependencyType.JAR) {
            // Keep the XML short, we don't need "JAR" given it's the default
            final Element typeElement = XmlUtils.createTextElement(document,
                    "type", type.toString().toLowerCase());
            dependencyElement.appendChild(typeElement);
        }

        // Keep the XML short, we don't need "compile" given it's the default
        if (scope != null && scope != DependencyScope.COMPILE) {
            dependencyElement.appendChild(XmlUtils.createTextElement(document,
                    "scope", scope.toString().toLowerCase()));
            if (scope == DependencyScope.SYSTEM
                    && StringUtils.isNotBlank(systemPath)) {
                dependencyElement.appendChild(XmlUtils.createTextElement(
                        document, "systemPath", systemPath));
            }
        }

        if (StringUtils.isNotBlank(classifier)) {
            dependencyElement.appendChild(XmlUtils.createTextElement(document,
                    "classifier", classifier));
        }

        // Add exclusions if any
        if (!exclusions.isEmpty()) {
            final Element exclusionsElement = DomUtils.createChildElement(
                    "exclusions", dependencyElement, document);
            for (final Dependency exclusion : exclusions) {
                final Element exclusionElement = DomUtils.createChildElement(
                        "exclusion", exclusionsElement, document);
                exclusionElement.appendChild(XmlUtils.createTextElement(
                        document, "groupId", exclusion.getGroupId()));
                exclusionElement.appendChild(XmlUtils.createTextElement(
                        document, "artifactId", exclusion.getArtifactId()));
            }
        }

        return dependencyElement;
    }

    /**
     * @return list of exclusions (never null)
     */
    public List<Dependency> getExclusions() {
        return exclusions;
    }

    public String getGroupId() {
        return groupId;
    }

    public DependencyScope getScope() {
        return scope;
    }

    /**
     * @return a simple description, as would be used for console output
     */
    public String getSimpleDescription() {
        return groupId + ":" + artifactId + ":" + version
                + (StringUtils.isNotBlank(classifier) ? ":" + classifier : "");
    }

    public String getSystemPath() {
        return systemPath;
    }

    public DependencyType getType() {
        return type;
    }

    public String getVersion() {
        return version;
    }

    @Deprecated
    public String getVersionId() {
        return version;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + (artifactId == null ? 0 : artifactId.hashCode());
        result = prime * result + (groupId == null ? 0 : groupId.hashCode());
        result = prime * result
                + (classifier == null ? 0 : classifier.hashCode());
        result = prime * result + (type == null ? 0 : type.hashCode());
        return result;
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
        return dependency != null && compareCoordinates(dependency) == 0;
    }

    @Override
    public String toString() {
        final ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("groupId", groupId);
        builder.append("artifactId", artifactId);
        builder.append("version", version);
        builder.append("type", type);
        builder.append("scope", scope);
        if (classifier != null) {
            builder.append("classifier", classifier);
        }
        return builder.toString();
    }
}
