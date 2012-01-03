package org.springframework.roo.addon.layers.service;

import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.project.Path;

/**
 * A factory for {@link ServiceAnnotationValues} instances.
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public interface ServiceAnnotationValuesFactory {

    /**
     * Returns the values of the {@link RooService} annotation on the given
     * service interface (assumed to be in {@link Path#SRC_MAIN_JAVA}).
     * 
     * @param serviceInterface (required)
     * @return <code>null</code> if the values aren't available, e.g. because
     *         the interface's physical details are unknown
     */
    ServiceAnnotationValues getInstance(
            ClassOrInterfaceTypeDetails serviceInterface);
}