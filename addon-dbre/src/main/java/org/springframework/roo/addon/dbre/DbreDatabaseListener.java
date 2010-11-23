package org.springframework.roo.addon.dbre;

import org.springframework.roo.addon.entity.IdentifierService;
import org.springframework.roo.model.JavaPackage;

/**
 * Interface for {@link DbreDatabaseListenerImpl} required for OSGi SCR.
 *
 * @author Alan Stewart
 * @since 1.1
 */
public interface DbreDatabaseListener extends DatabaseListener, IdentifierService {
	
	/**
	 * Specifies the {@link JavaPackage} to create entities in.
	 * 
	 * @param destinationPackage the JavaPackage
	 */
	void setDestinationPackage(JavaPackage destinationPackage);
}
