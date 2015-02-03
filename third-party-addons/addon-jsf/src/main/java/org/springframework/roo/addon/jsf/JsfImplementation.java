package org.springframework.roo.addon.jsf;

/**
 * The JSF implementation.
 * 
 * @author Alan Stewart
 * @since 1.2.0
 */
public enum JsfImplementation {
    APACHE_MYFACES, ORACLE_MOJARRA;

    public String getConfigPrefix() {
        return "/configuration/jsf-implementations/jsf-implementation[@id='"
                + name() + "']";
    }
}
