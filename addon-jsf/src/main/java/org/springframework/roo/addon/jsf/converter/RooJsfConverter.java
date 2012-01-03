package org.springframework.roo.addon.jsf.converter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates a type that requires a ROO JSF converter.
 * 
 * @author Alan Stewart
 * @since 1.2.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface RooJsfConverter {

    Class<?> entity();
}
