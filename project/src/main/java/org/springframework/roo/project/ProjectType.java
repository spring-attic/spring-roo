package org.springframework.roo.project;

import org.springframework.roo.support.util.Assert;

/**
 * Provides available project types for the project. Currently only war and jar
 * types are supported, but other types can be added in future.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 */
public class ProjectType {
	public static final ProjectType JAR = new ProjectType("jar");
	public static final ProjectType WAR = new ProjectType("war");
	// public static final ProjectType POM = new ProjectType("pom");
	// public static final ProjectType OSGI_BUNDLE = new ProjectType("osgi-bundle");
	private String type;

	private ProjectType(String type) {
		Assert.hasText(type, "Type required");
		this.type = type;
	}

	public String getType() {
		return type;
	}
}
