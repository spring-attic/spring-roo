package org.springframework.roo.classpath.details.annotations.populator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;

/**
 * Identifies a field that can be automatically populated from {@link AnnotationAttributeValue}s. 
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface AutoPopulate {
	/**
	 * @return the name of the annotation value to read (defaults to an empty string, which denotes
	 * the name of the field should be used)
	 */
	String value() default "";
}
