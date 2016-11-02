package org.springframework.roo.addon.web.mvc.controller.addon.config;

import org.springframework.roo.addon.web.mvc.controller.annotations.config.RooJsonMixin;
import org.springframework.roo.classpath.itd.ItdTriggerBasedMetadataProvider;

/**
 * Metadata provider for {@link JSONMixinMetadata}. Monitors
 * notifications for {@link RooJsonMixin}
 * annotated types.
 *
 * @author Jose Manuel Vivó
 * @since 2.0
 */
public interface JSONMixinMetadataProvider extends ItdTriggerBasedMetadataProvider {
}
