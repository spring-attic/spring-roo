package org.springframework.roo.addon.jsf;

/**
 * The JSF component library.
 * 
 * @author Alan Stewart
 * @since 1.2.0
 */
public enum JsfLibrary {
    PRIMEFACES;

    public String getConfigPrefix() {
        return "/configuration/jsf-libraries/jsf-library[@id='" + name() + "']";
    }
}
