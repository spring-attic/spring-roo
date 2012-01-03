package org.springframework.roo.addon.dbre;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates the lifecycle of the entity is managed by the database reverse
 * engineering process.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface RooDbManaged {

    /**
     * @return whether to delete the database-managed entity (defaults to true).
     */
    boolean automaticallyDelete() default true;
}
