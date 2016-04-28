package org.springframework.roo.addon.web.mvc.controller.addon.config;

import org.springframework.roo.classpath.itd.ItdTriggerBasedMetadataProvider;

/**
 * Metadata provider for {@link WebMvcJSONConfigurationMetadata}. Monitors
 * notifications for {@link RooWebMvcJSONConfiguration}
 * annotated types.
 * 
 * @author Sergio Clares
 * @since 2.0
 */
public interface WebMvcJSONConfigurationMetadataProvider extends ItdTriggerBasedMetadataProvider {
}
