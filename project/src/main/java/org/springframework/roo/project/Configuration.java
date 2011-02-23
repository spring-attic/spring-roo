package org.springframework.roo.project;

import org.springframework.roo.support.util.Assert;
import org.w3c.dom.Element;

/**
 * Immutable representation of an configuration specification for a (maven) build plugin
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public final class Configuration {
	private final Element configuration;

	public Configuration(Element configuration) {
		Assert.notNull(configuration, "configuration must be specified");
		this.configuration = configuration;
	}

	public Element getConfiguration() {
		return configuration;
	}
}
