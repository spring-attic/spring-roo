package org.springframework.roo.support.lifecycle;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates a class that should be instantiated when the ROO development Shell is used.
 * 
 * <p>
 * Note that IDEs do not require Shell-related classes, which is why Shell-related classes are annotated
 * with this annotation instead of the more typical {@link ScopeDevelopment}.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ScopeDevelopmentShell {}
