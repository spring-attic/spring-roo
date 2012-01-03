package org.springframework.roo.addon.web.mvc.controller.converter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * Designates a class as the application-wide conversion service for registering
 * application Converters and Formatters. This conversion service is typically
 * automatically installed by Spring ROO at the same time and in the same
 * package as the first controller created through the "controller" command.
 * </p>
 * <p>
 * The installed conversion service is a sub-type of
 * FormattingConversionServiceFactoryBean. The installFormatters method can be
 * used to manually install application converters and formatters. In additional
 * ROO will generate methods to register converters for all application domain
 * types that may need to be displayed as Strings in drop-downs as well as in
 * various places in the UI.
 * </p>
 * 
 * @author Rossen Stoyanchev
 * @since 1.1.1
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface RooConversionService {
}
