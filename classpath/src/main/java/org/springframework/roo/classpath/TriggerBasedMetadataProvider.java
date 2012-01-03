package org.springframework.roo.classpath;

import org.springframework.roo.metadata.MetadataProvider;
import org.springframework.roo.model.JavaType;

/**
 * A {@link MetadataProvider} that produces metadata when any of various trigger
 * annotations are present on a user project type (known as the governor).
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public interface TriggerBasedMetadataProvider extends MetadataProvider {

    /**
     * Causes this provider to generate metadata if the given annotation is
     * present.
     * 
     * @param trigger the trigger to register (can be <code>null</code>)
     */
    void addMetadataTrigger(JavaType trigger);

    /**
     * Stops this provider generating metadata if the given annotation is
     * present.
     * 
     * @param trigger the trigger to deregister (can be <code>null</code>)
     */
    void removeMetadataTrigger(JavaType trigger);
}
