package org.springframework.roo.addon.web.mvc.controller.addon.config;

import org.springframework.roo.classpath.itd.ItdTriggerBasedMetadataProvider;

/**
 * Metadata provider for {@link WebMvcConfigurationMetadata}. Monitors
 * notifications for {@link RooWebMvcConfiguration}
 * annotated types.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public interface WebMvcConfigurationMetadataProvider extends ItdTriggerBasedMetadataProvider {
}
