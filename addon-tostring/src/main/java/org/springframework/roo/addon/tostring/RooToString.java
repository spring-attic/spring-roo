package org.springframework.roo.addon.tostring;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;

/**
 * Provides a {@link Object#toString()} method if requested.
 * <p>
 * Whilst it is possible to apply this annotation to any class that you'd like a
 * {@link Object#toString()} method produced for, it is generally triggered
 * automatically via the use of most other annotations in the system. The
 * created method is conservative and only includes public accessor methods
 * within the produced string. Further, any accessor which returns a common JDK
 * {@link Collection} type is restricted to displaying its size only.
 * 
 * @author Ben Alex
 * @since 1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface RooToString {

    /**
     * @return an array of fields to exclude in the toString method
     */
    String[] excludeFields() default "";

    /**
     * @return the name of the {@link Object#toString()} method to generate
     *         (defaults to "toString"; if empty, does not create)
     */
    String toStringMethod() default "toString";
}
