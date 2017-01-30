package org.springframework.roo.addon.layers.repository.jpa.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * = RooJpaRepositoryConfiguration
 * 
 * Marks the annotated type as a Spring Data Jpa Repository Configuration class.
 *
 * @author Sergio Clares
 * @since 2.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface RooJpaRepositoryConfiguration {
}
