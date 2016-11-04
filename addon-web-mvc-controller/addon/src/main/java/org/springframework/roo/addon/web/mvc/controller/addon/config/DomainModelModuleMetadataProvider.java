package org.springframework.roo.addon.web.mvc.controller.addon.config;

import org.springframework.roo.addon.web.mvc.controller.annotations.config.RooDomainModelModule;
import org.springframework.roo.classpath.itd.ItdTriggerBasedMetadataProvider;

/**
 * Metadata provider for {@link DomainModelModuleMetadata}. Monitors
 * notifications for {@link RooDomainModelModule}
 * annotated types.
 *
 * @author Jose Manuel Vivó
 * @since 2.0
 */
public interface DomainModelModuleMetadataProvider extends ItdTriggerBasedMetadataProvider {
}
