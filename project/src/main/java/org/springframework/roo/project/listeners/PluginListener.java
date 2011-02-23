package org.springframework.roo.project.listeners;

import org.springframework.roo.project.Plugin;

/**
 * Plugin listener interface that clients can implement in order
 * to be notified of changes to project build plugins
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public interface PluginListener {

	void pluginAdded(Plugin plugin);

	void pluginRemoved(Plugin plugin);
}
