package org.springframework.roo.addon.cloud.providers;

/**
 * 
 * Cloud Provider Interface
 * 
 * @author Juan Carlos García del Canto
 * @since 1.2.6
 */
public interface CloudProvider {

    /**
     * Gets provider name
     * 
     * @return
     */
    String getName();

    /**
     * 
     */
    String getDescription();

    /**
     * This method installs the provider that implements the interface
     * 
     * @param configuration 
     */
    void setup(String configuration);

}
