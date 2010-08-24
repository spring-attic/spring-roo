package org.springframework.roo.shell;

import java.beans.PropertyEditor;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface CliOption {
	/**
	 * @return if true, the user cannot specify this option and it is provided by the shell infrastructure
	 * (defaults to false)
	 */
	boolean systemProvided() default false;
	
	/**
	 * @return the name of the option, which must be unique within this {@link CliCommand} (an empty String may
	 * be given, which would denote this option is the default for the command)
	 */
	String[] key();
	
	/**
	 * @return true if this option must be specified one way or the other by the user (defaults to false)
	 */
	boolean mandatory() default false;
	
	/**
	 * @return the default value to use if this option is unspecified by the user (defaults to __NULL__, which causes null to
	 * be presented to any non-primitive parameter)
	 */
	String unspecifiedDefaultValue() default "__NULL__";
	
	/**
	 * @return the default value to use if this option is included by the user, but they didn't specify an
	 * actual value (most commonly used for flags; defaults to __NULL__, which causes null to
	 * be presented to any non-primitive parameter)
	 */
	String specifiedDefaultValue() default "__NULL__";
	
	/**
	 * @return the name of a context which will be available to the {@link PropertyEditor} and {@link jline.Completor}
	 * when being asked to process the option (defaults to an empty String, meaning no option context is set)
	 */
	String optionContext() default "";
	
	/**
	 * @return a help message for this option (the default is a blank String, which means there is no help)
	 */
	String help() default "";
}
