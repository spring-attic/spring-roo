package org.springframework.roo.addon.web.mvc.thymeleaf.addon;

import org.springframework.roo.addon.web.mvc.thymeleaf.annotations.RooThymeleaf;
import org.springframework.roo.classpath.itd.ItdTriggerBasedMetadataProvider;

/**
 * Metadata provider for {@link ThymeleafMetadataProvider}. Monitors
 * notifications for {@link RooThymeleaf} annotated types.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public interface ThymeleafMetadataProvider extends ItdTriggerBasedMetadataProvider {
}
