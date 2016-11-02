package org.springframework.roo.addon.web.mvc.controller.addon.config;

import org.springframework.roo.addon.web.mvc.controller.annotations.config.RooDeserializer;
import org.springframework.roo.classpath.itd.ItdTriggerBasedMetadataProvider;

/**
 * Metadata provider for {@link EntityDeserializerMetadata}. Monitors
 * notifications for {@link RooDeserializer}
 * annotated types.
 *
 * @author Jose Manuel Vivó
 * @since 2.0
 */
public interface EntityDeserializerMetadataProvider extends ItdTriggerBasedMetadataProvider {
}
