package org.springframework.roo.addon.dbre;

import org.springframework.roo.addon.entity.IdentifierService;

/**
 * Interface for {@link DbreDatabaseListenerImpl} required for OSGi SCR.
 *
 * @author Alan Stewart
 * @since 1.1
 */
public interface DbreDatabaseListener extends DatabaseListener, IdentifierService {
}
