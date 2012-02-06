package org.springframework.roo.addon.tailor.config;

/**
 * Creates a Tailor configuration.
 * 
 * @author Birgitta Boeckeler
 */
public interface TailorConfigurationFactory {

    /**
     * Creates a tailor configuration.
     * 
     * @param name - configuration name
     * @return - tailor configuration
     */
    TailorConfiguration createTailorConfiguration();
}
