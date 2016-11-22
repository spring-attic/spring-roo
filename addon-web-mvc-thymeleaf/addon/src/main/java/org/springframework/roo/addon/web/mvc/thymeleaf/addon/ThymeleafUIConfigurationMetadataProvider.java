package org.springframework.roo.addon.web.mvc.thymeleaf.addon;

import org.springframework.roo.addon.web.mvc.thymeleaf.annotations.RooWebMvcThymeleafUIConfiguration;
import org.springframework.roo.classpath.itd.ItdTriggerBasedMetadataProvider;

/**
 * Metadata provider for {@link ThymeleafUIConfigurationMetadataProvider}. Monitors
 * notifications for {@link RooWebMvcThymeleafUIConfiguration} annotated types.
 *
 * @author Jose Manuel Vivó
 * @since 2.0
 */
public interface ThymeleafUIConfigurationMetadataProvider extends ItdTriggerBasedMetadataProvider {
}
