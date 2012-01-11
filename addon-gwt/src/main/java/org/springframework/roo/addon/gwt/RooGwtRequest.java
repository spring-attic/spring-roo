package org.springframework.roo.addon.gwt;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface RooGwtRequest {

    boolean dontIncludeProxyMethods() default true;

    /**
     * Entity methods to exclude from the request interface.
     * 
     * @return
     * @deprecated ignored by the GWT addon
     */
    @Deprecated
    String[] exclude() default {};

    boolean ignoreProxyExclusions() default false;

    boolean ignoreProxyReadOnly() default false;

    /**
     * @return the fully-qualified type name this key instance was mirrored from
     */
    String value();
}
