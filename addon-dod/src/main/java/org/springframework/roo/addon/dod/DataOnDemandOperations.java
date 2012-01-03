package org.springframework.roo.addon.dod;

import org.springframework.roo.model.JavaType;

/**
 * Creates a new data-on-demand class for an entity.
 * 
 * @author Alan Stewart
 * @since 1.1.3
 */
public interface DataOnDemandOperations {

    /**
     * Checks for the existence the META-INF/persistence.xml
     * 
     * @return true if the META-INF/persistence.xml exists, otherwise false
     */
    boolean isDataOnDemandInstallationPossible();

    /**
     * Creates a new data-on-demand (DoD) provider for the entity. Silently
     * returns if the DoD class already exists.
     * 
     * @param entity to produce a DoD provider for (required)
     * @param name the name of the new DoD class (required)
     */
    void newDod(JavaType entity, JavaType name);
}
