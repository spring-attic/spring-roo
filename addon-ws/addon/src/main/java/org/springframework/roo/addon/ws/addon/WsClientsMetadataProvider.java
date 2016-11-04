package org.springframework.roo.addon.ws.addon;

import org.springframework.roo.classpath.itd.ItdTriggerBasedMetadataProvider;

/**
 * Metadata provider for {@link WsClientsMetadata}. Monitors
 * notifications for {@link RooWsClients} annotated types.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public interface WsClientsMetadataProvider extends ItdTriggerBasedMetadataProvider {
}
