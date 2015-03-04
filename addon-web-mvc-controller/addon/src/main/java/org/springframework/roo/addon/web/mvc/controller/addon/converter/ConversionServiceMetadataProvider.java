package org.springframework.roo.addon.web.mvc.controller.addon.converter;

import org.springframework.roo.addon.web.mvc.controller.annotations.converter.RooConversionService;
import org.springframework.roo.addon.web.mvc.controller.annotations.scaffold.RooWebScaffold;
import org.springframework.roo.classpath.itd.ItdTriggerBasedMetadataProvider;

/**
 * Metadata provider for {@link ConversionServiceMetadata}. Monitors
 * notifications for {@link RooConversionService} and {@link RooWebScaffold}
 * annotated types. Also listens for changes to the scaffolded domain types and
 * their associated domain types.
 * 
 * @author Rossen Stoyanchev
 * @author Stefan Schmidt
 * @since 1.1.1
 */
public interface ConversionServiceMetadataProvider extends
        ItdTriggerBasedMetadataProvider {
}