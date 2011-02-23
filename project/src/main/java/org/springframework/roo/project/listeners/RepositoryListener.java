package org.springframework.roo.project.listeners;

import org.springframework.roo.project.Repository;

/**
 * Repository listener interface that clients can implement in order
 * to be notified of changes to project repositories
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
public interface RepositoryListener {

	void repositoryAdded(Repository repositiory);

	void repositoryRemoved(Repository reypository);
}
