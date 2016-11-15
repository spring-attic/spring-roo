package org.springframework.roo.addon.ws.addon.jaxb;

import org.springframework.roo.addon.ws.addon.jaxb.JaxbEntityMetadata;
import org.springframework.roo.addon.ws.annotations.jaxb.RooJaxbEntity;
import org.springframework.roo.classpath.itd.ItdTriggerBasedMetadataProvider;

/**
 * Metadata provider for {@link JaxbEntityMetadata}. Monitors
 * notifications for {@link RooJaxbEntity} annotated types.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public interface JaxbEntityMetadataProvider extends ItdTriggerBasedMetadataProvider {
}
