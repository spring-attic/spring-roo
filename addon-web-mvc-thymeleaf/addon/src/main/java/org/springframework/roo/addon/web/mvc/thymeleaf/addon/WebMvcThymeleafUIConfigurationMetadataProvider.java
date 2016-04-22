package org.springframework.roo.addon.web.mvc.thymeleaf.addon;

import org.springframework.roo.addon.web.mvc.thymeleaf.annotations.RooWebMvcThymeleafUIConfiguration;
import org.springframework.roo.classpath.itd.ItdTriggerBasedMetadataProvider;

/**
 * Metadata provider for {@link WebMvcThymeleafUIConfigurationMetadataProvider}. Monitors
 * notifications for {@link RooWebMvcThymeleafUIConfiguration} annotated types.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public interface WebMvcThymeleafUIConfigurationMetadataProvider extends
    ItdTriggerBasedMetadataProvider {
}
