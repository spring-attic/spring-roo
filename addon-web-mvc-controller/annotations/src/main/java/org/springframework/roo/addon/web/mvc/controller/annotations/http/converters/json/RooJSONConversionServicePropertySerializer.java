package org.springframework.roo.addon.web.mvc.controller.annotations.http.converters.json;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indentifies a JSON converter type.
 * <p>
 * This annotation doesn't produces any code by the moment.
 * 
 * @author Sergio Clares
 * @since 2.0
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface RooJSONConversionServicePropertySerializer {
}
