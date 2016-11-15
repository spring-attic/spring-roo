package org.springframework.roo.addon.security.addon.security;

import org.springframework.roo.addon.security.annotations.RooSecurityFilters;
import org.springframework.roo.classpath.itd.ItdTriggerBasedMetadataProvider;

/**
 * Metadata provider for {@link SecurityFiltersMetadata}. Monitors
 * notifications for {@link RooSecurityFilters} annotated types.
 *
 * @author Manuel Iborra
 * @since 2.0
 */
public interface SecurityFiltersMetadataProvider extends ItdTriggerBasedMetadataProvider {
}
