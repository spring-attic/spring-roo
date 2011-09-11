package org.springframework.roo.addon.gwt;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface RooGwtRequest {

	/**
	 * @return the fully-qualified type name this key instance was mirrored from
	 */
	String value();

	String[] exclude() default {};

	boolean ignoreProxyExclusions() default false;

	boolean ignoreProxyReadOnly() default false;

	boolean dontIncludeProxyMethods() default true;
}
