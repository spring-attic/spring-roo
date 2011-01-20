package org.springframework.roo.addon.javabean;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Creates JavaBean accessors and mutators for fields declared against this type.
 * 
 * @author Ben Alex
 * @since 1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface RooJavaBean {

	/**
	 * @return whether to generate getters for each non-transient field declared in this class (defaults to true)
	 */
	boolean gettersByDefault() default true;

	/**
	 * @return whether to generate setters for each non-transient field declared in this class (defaults to true)
	 */
	boolean settersByDefault() default true;
}
