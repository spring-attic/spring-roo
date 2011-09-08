package org.springframework.roo.addon.gwt;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface RooGwtProxy {

	/**
	 * @return the fully-qualified type name this key instance was mirrored from
	 */
	String[] readOnly() default {};

	String[] exclude() default {};

	boolean scaffold() default false;
}
