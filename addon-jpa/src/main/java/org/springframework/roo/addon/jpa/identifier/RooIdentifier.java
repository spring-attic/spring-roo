package org.springframework.roo.addon.jpa.identifier;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.roo.addon.configurable.RooConfigurable;
import org.springframework.roo.addon.serializable.RooSerializable;

/**
 * Provides identifier services related to JPA.
 * <p>
 * Using this annotation also triggers {@link RooSerializable} and
 * {@link RooConfigurable}.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface RooIdentifier {

    /**
     * @return whether to delete the database-managed identifier (defaults to
     *         false).
     */
    boolean dbManaged() default false;

    /**
     * @return whether to generate getters for each non-transient field declared
     *         in this class (defaults to true)
     */
    boolean gettersByDefault() default true;

    /**
     * Allows disabling the automated creation of a no-arg constructor. This
     * might be appropriate, for example, if another add-on is providing more
     * sophisticated constructor creation facilities.
     * 
     * @return whether to generate a no-argument constructor in this class
     *         (defaults to true)
     */
    boolean noArgConstructor() default true;

    /**
     * @return whether to generate setters for each non-transient field declared
     *         in this class (defaults to false)
     */
    boolean settersByDefault() default false;
}
