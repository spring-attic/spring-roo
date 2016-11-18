package org.springframework.roo.addon.security.addon.security;

import org.springframework.roo.addon.security.annotations.RooSecurityAuthorizations;
import org.springframework.roo.classpath.itd.ItdTriggerBasedMetadataProvider;

/**
 * Metadata provider for {@link SecurityAuthorizationsMetadata}. Monitors
 * notifications for {@link RooSecurityAuthorizations} annotated types.
 *
 * @author Manuel Iborra
 * @since 2.0
 */
public interface SecurityAuthorizationsMetadataProvider extends ItdTriggerBasedMetadataProvider {
}
