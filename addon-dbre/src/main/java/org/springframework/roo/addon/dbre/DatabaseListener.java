package org.springframework.roo.addon.dbre;

import org.springframework.roo.addon.dbre.model.Database;

/**
 * Implemented by a class that wishes to be notified whenever a {@link Database} is
 * refreshed. This behaviour is guaranteed by {@link DbreModelService}. Note
 * the notification will only occur when a database change is detected for some
 * reason. It will also publish when an initial startup takes place when the remainder
 * of the Roo components are notified to start processing.
 *
 * @author Alan Stewart
 * @since 1.1
 */
public interface DatabaseListener {
	
	/**
	 * Indicates the database has been loaded or refreshed. It does not necessarily indicate
	 * the database schema has changed, simply that a new updated representation has
	 * become available.
	 * 
	 * @param newDatabase the refreshed database contents (never null)
	 */
	void notifyDatabaseRefreshed(Database newDatabase);
}
