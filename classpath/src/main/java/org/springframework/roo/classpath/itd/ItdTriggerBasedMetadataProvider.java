package org.springframework.roo.classpath.itd;

import org.springframework.roo.classpath.TriggerBasedMetadataProvider;
import org.springframework.roo.model.JavaType;

/**
 * An {@link ItdMetadataProvider} that permits registration of different
 * {@link JavaType}s as metadata trigger annotations. See
 * {@link AbstractItdMetadataProvider} for more information about triggers.
 * 
 * @author Ben Alex
 * @since 1.1
 */
public interface ItdTriggerBasedMetadataProvider extends ItdMetadataProvider,
        TriggerBasedMetadataProvider {
}