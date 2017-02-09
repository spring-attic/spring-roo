package org.springframework.roo.addon.jpa.annotations.dod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates to produce a "data on demand" configuration class, which is 
 * required for automated integration testing. This class will load all 
 * specific "data on demand" classes as a @Bean only when required.
 * 
 * @author Sergio Clares
 * @since 2.0
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface RooJpaDataOnDemandConfiguration {
}
