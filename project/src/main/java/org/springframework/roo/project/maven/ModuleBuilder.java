package org.springframework.roo.project.maven;

import org.springframework.roo.model.Builder;

public class ModuleBuilder implements Builder<Module>{

	private final String name;
	private final String pomPath;

	public ModuleBuilder(final String name, final String pomPath) {
		this.name = name;
		this.pomPath = pomPath;
	}

	public String getName() {
		return name;
	}

	public String getPomPath() {
		return pomPath;
	}

	public Module build() {
		return new Module(name, pomPath);
	}
}
