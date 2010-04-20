package org.springframework.roo.project;

/**
 * Plugin repository listener interface that clients can implement in order
 * to be notified of changes to project plugin repositories
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public interface PluginRepositoryListener {

	void pluginRepositoryAdded(PluginRepository pluginRepository);

	void pluginRepositoryRemoved(PluginRepository pluginRepository);
}
