package org.springframework.roo.addon.web.mvc.controller.annotations.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This class provides annotation to indicate that some class
 * is a DomainModelModule which registers all Jackson Mixin classes
 * in project
 *
 * @author Jose Manuel Vivó
 * @since 2.0
 * @see RooJsonMixin
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface RooDomainModelModule {

}
