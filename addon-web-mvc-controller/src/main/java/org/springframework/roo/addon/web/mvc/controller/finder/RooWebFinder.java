package org.springframework.roo.addon.web.mvc.controller.finder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates a type that requires ROO controller support.
 * <p>
 * This annotation will cause ROO to produce code that will expose dynamic
 * finders to the Web UI.
 * 
 * @author Stefan Schmidt
 * @since 1.2.0
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface RooWebFinder {
}
