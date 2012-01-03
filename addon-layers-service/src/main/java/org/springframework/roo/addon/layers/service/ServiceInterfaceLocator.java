package org.springframework.roo.addon.layers.service;

import java.util.Collection;

import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.model.JavaType;

/**
 * Locates service interfaces within the user's project.
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public interface ServiceInterfaceLocator {

    /**
     * Returns the details of any interfaces annotated with {@link RooService}
     * that claim to support the given type of entity.
     * 
     * @param entityType can't be <code>null</code>
     * @return a non-<code>null</code> collection; empty if there's no such
     *         services or the given entity is <code>null</code>
     */
    Collection<ClassOrInterfaceTypeDetails> getServiceInterfaces(
            JavaType entityType);
}