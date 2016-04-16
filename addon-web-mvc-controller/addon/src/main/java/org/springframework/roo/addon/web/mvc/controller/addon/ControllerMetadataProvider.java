package org.springframework.roo.addon.web.mvc.controller.addon;

import org.springframework.roo.classpath.itd.ItdTriggerBasedMetadataProvider;

/**
 * Metadata provider for {@link ControllerMetadataProvider}. Monitors
 * notifications for {@link RooController} annotated types.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public interface ControllerMetadataProvider extends ItdTriggerBasedMetadataProvider {
}
