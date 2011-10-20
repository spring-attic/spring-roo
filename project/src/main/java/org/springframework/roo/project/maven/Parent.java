package org.springframework.roo.project.maven;

public class Parent {

	private final String groupId;
	private final String artifactId;
	private final String version;
	private final String relativePath;
	private final String pomPath;

	public Parent(String groupId, String artifactId, String version, String relativePath, String pomPath) {
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
		this.relativePath = relativePath;
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
}
