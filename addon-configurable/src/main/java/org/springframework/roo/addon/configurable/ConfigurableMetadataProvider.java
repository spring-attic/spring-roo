package org.springframework.roo.addon.configurable;

import org.springframework.roo.classpath.itd.ItdTriggerBasedMetadataProvider;

/**
 * Provides {@link ConfigurableMetadata}.
 * 
 * <p>
 * Generally other add-ons which depend on Spring's @Configurable annotation being present will add their
 * annotation as a trigger annotation to instances of {@link ConfigurableMetadataProvider}. This action will
 * guarantee any class with the added trigger annotation will made @Configurable.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public interface ConfigurableMetadataProvider extends ItdTriggerBasedMetadataProvider {}