package org.springframework.roo.addon.ws.addon;

import org.springframework.roo.addon.ws.annotations.RooWsEndpoints;
import org.springframework.roo.classpath.itd.ItdTriggerBasedMetadataProvider;

/**
 * Metadata provider for {@link WsEndpointsMetadata}. Monitors
 * notifications for {@link RooWsEndpoints} annotated types.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public interface WsEndpointsMetadataProvider extends ItdTriggerBasedMetadataProvider {
}
