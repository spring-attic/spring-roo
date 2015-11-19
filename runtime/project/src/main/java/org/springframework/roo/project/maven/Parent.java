package org.springframework.roo.project.maven;

/**
 * The parent declaration within a Maven POM.
 * 
 * @author James Tyrrell
 * @since 1.2.0
 */
public class Parent {

    private final String artifactId;
    private final String groupId;
    private final String pomPath;
    private final String relativePath;
    private final String version;

    public Parent(final String groupId, final String artifactId,
            final String version, final String relativePath,
            final String pomPath) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.relativePath = relativePath;
        this.pomPath = pomPath;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getPomPath() {
        return pomPath;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public String getVersion() {
        return version;
    }
}
