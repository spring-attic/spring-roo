package org.springframework.roo.project;

/**
 * Repository listener interface that clients can implement in order
 * to be notified of changes to project repositories
 * 
 * @author Stefan Schmidt
 * @since 1.1
 *
 */
public interface RepositoryListener {

	void repositoryAdded(Repository r);

	void repositoryRemoved(Repository r);

}
