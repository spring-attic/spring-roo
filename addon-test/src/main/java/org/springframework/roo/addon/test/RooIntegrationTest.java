package org.springframework.roo.addon.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates to produce an integration test class.
 * 
 * @author Ben Alex
 * @since 1.0
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface RooIntegrationTest {

    boolean count() default true;

    /**
     * @return the type of class that will have an entity test created
     *         (required; must offer entity services)
     */
    Class<?> entity();

    boolean find() default true;

    boolean findAll() default true;

    int findAllMaximum() default 250;

    boolean findEntries() default true;

    boolean flush() default true;

    boolean merge() default true;

    boolean persist() default true;

    boolean remove() default true;

    boolean transactional() default true;
}
