package org.springframework.roo.addon.gwt;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface RooGwtMirroredFrom {

    boolean dontIncludeProxyMethods() default true;

    String[] exclude() default {};

    boolean ignoreProxyExclusions() default false;

    boolean ignoreProxyReadOnly() default false;

    String[] readOnly() default {};

    boolean scaffold() default false;

    String value();
}
