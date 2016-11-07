package org.springframework.roo.addon.ws.addon;

import org.springframework.roo.addon.ws.annotations.RooSei;
import org.springframework.roo.classpath.itd.ItdTriggerBasedMetadataProvider;

/**
 * Metadata provider for {@link SeiMetadata}. Monitors
 * notifications for {@link RooSei} annotated types.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public interface SeiMetadataProvider extends ItdTriggerBasedMetadataProvider {
}
