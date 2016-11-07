package org.springframework.roo.addon.web.mvc.exceptions.addon;

import org.springframework.roo.addon.web.mvc.exceptions.annotations.RooExceptionHandlers;
import org.springframework.roo.classpath.itd.ItdTriggerBasedMetadataProvider;

/**
 * Metadata provider for {@link ExceptionMetadataProvider}. Monitors
 * notifications for {@link RooExceptionHandlers} annotated types.
 *
 * @author Fran Cardoso
 * @since 2.0
 */
public interface ExceptionsMetadataProvider extends ItdTriggerBasedMetadataProvider {

}
