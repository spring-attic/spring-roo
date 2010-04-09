package org.springframework.roo.addon.entity;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.roo.addon.serializable.RooSerializable;

/**
 * Provides services related to JPA.
 * 
 * <p>
 * Using this annotation also triggers {@link RooSerializable}.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface RooIdentifier {
}
