package org.springframework.roo.project.listeners;

import org.springframework.roo.project.Resource;

/**
 * Resource listener interface that clients can implement in order
 * to be notified of changes to project resources
 * 
 * @author Alan Stewart
 * @since 1.1
 */
@Deprecated
public interface ResourceListener {

	void resourceAdded(Resource resource);

	void resourceRemoved(Resource resource);
}
