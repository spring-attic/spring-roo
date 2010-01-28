package org.springframework.roo.addon.configurable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates a class should be annotated with Spring's
 * {@link org.springframework.beans.factory.annotation.Configurable} annotation.
 *  
 * <p>
 * Obviously you should just use @Configurable in normal Java code if you would like
 * {@link org.springframework.beans.factory.annotation.Configurable} functionality 
 * (ie there is no use case for using {@link RooConfigurable} given it
 * is more complex with the involvement of ITDs etc). This annotation exists solely for 
 * consistency with other ITD providers.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface RooConfigurable {}
