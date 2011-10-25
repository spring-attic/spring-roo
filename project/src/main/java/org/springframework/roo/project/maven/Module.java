package org.springframework.roo.project.maven;

import org.springframework.roo.support.util.Assert;

/**
 * A module of a Maven multi-module project.
 *
 * @author James Tyrrell
 * @since 1.2.0
 */
public class Module {

	// Fields
	private final String name;
	private final String pomPath;

	/**
	 * Constructor
	 *
	 * @param name the module's name (can't be blank)
	 * @param pomPath the canonical path of the module's POM file (can't be blank)
	 */
	public Module(final String name, final String pomPath) {
		Assert.hasText(name, "Invalid module name '" + name + "'");
		Assert.hasText(pomPath, "Invalid path '" + pomPath + "'");
		this.name = name;
		this.pomPath = pomPath;
	}

	public String getName() {
		return name;
	}

	public String getPomPath() {
		return pomPath;
	}
}
