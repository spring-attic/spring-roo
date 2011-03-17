package org.springframework.roo.project.listeners;

import org.springframework.roo.project.Dependency;

/**
 * Dependency listener interface that clients can implement in order
 * to be notified of changes to project dependencies
 * 
 * @author Adrian Colyer
 * @since 1.0
 *
 */
public interface DependencyListener {

	void dependencyAdded(Dependency d);

	void dependencyRemoved(Dependency d);

}
