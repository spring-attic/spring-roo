package org.springframework.roo.addon.dbre;

import org.springframework.roo.addon.entity.IdentifierService;

/**
 * Interface for {@link DbreDatabaseListenerImpl} required for OSGi SCR.
 *
 * @author Alan Stewart
 * @since 1.1
 */
public interface DbreDatabaseListener extends DatabaseListener, IdentifierService {
	
	/**
	 * Indicates whether to create integration tests for new entities.
	 * 
	 * @param testAutomatically true if integration tests are to be created, otherwise false
	 */
	void setTestAutomatically(boolean testAutomatically);
}
