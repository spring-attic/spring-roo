package org.springframework.roo.addon.ws.addon;

import org.springframework.roo.addon.ws.annotations.RooSeiImpl;
import org.springframework.roo.classpath.itd.ItdTriggerBasedMetadataProvider;

/**
 * Metadata provider for {@link SeiImplMetadata}. Monitors
 * notifications for {@link RooSeiImpl} annotated types.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public interface SeiImplMetadataProvider extends ItdTriggerBasedMetadataProvider {
}
