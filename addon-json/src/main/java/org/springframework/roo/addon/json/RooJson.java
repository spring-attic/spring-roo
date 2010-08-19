package org.springframework.roo.addon.json;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Trigger annotation for addon-json
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface RooJson {
	
	/**
	 * Specify name of the "toJson" method to generate. Use a value of "" to avoid the generation 
	 * of this method.
	 * 
	 * @return the name of the "toJson" method to generate (defaults to "toJson"; mandatory)
	 */
	String toJsonMethod() default "toJson";
	
	/**
	 * Specify name of the "fromJson" method to generate. Use a value of "" to avoid the generation 
	 * of this method.
	 * 
	 * @return the name of the "fromJson" method to generate (defaults to "fromJson"; mandatory)
	 */
	String fromJsonMethod() default "fromJson";
	
	/**
	 * Specify name of the "fromJsonArray" method to generate. Use a value of "" to avoid the generation 
	 * of this method.
	 * 
	 * @return the name of the "fromJsonArray" method to generate (defaults to "fromJsonArray"; mandatory)
	 */
	String fromJsonArrayMethod() default "fromJsonArray";
	
	/**
	 * Specify name of the "toJsonArray" method to generate. Use a value of "" to avoid the generation 
	 * of this method.
	 * 
	 * @return the name of the "toJsonArray" method to generate (defaults to "toJsonArray"; mandatory)
	 */
	String toJsonArrayMethod() default "toJsonArray";
}

