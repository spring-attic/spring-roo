package org.springframework.roo.project.maven;

import org.springframework.roo.model.Builder;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Element;

public class ParentBuilder implements Builder<Parent> {

    private final String artifactId;
    private final String groupId;
    private final String pomPath;
    private final String relativePath;
    private final String version;

    public ParentBuilder(final Element parentElement, final String pomPath) {
        groupId = XmlUtils.getTextContent("/project/groupId", parentElement);
        artifactId = XmlUtils.getTextContent("/project/artifactId", parentElement);
        version = XmlUtils.getTextContent("/project/version", parentElement);
        relativePath = XmlUtils.getTextContent("/project/relativePath", parentElement);
        this.pomPath = pomPath;
    }

    public Parent build() {
        return new Parent(groupId, artifactId, version, relativePath, pomPath);
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
