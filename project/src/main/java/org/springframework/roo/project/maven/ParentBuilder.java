package org.springframework.roo.project.maven;

import org.springframework.roo.model.Builder;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Element;

public class ParentBuilder implements Builder<Parent>{

	private final String groupId;
	private final String artifactId;
	private final String version;
	private final String relativePath;
	private final String pomPath;

	public ParentBuilder(final Element parentElement, final String pomPath) {
		this.groupId = XmlUtils.getTextContent("/groupId", parentElement);
		this.artifactId = XmlUtils.getTextContent("/artifactId", parentElement);
		this.version = XmlUtils.getTextContent("/version", parentElement);
		this.relativePath = XmlUtils.getTextContent("/relativePath", parentElement);
		this.pomPath = pomPath;
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

	public String getRelativePath() {
		return relativePath;
	}

	public String getPomPath() {
		return pomPath;
	}

	public Parent build() {
		return new Parent(groupId, artifactId, version, relativePath, pomPath);
	}
}
