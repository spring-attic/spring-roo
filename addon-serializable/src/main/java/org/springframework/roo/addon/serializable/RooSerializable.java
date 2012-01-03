package org.springframework.roo.addon.serializable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates a class should implement the {@link java.io.Serializable}
 * interface. Generates and maintains a static final long serialVersionUID.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface RooSerializable {
}
