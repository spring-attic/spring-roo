package org.springframework.roo.addon.tailor.config;

/**
 * Creates Tailor configuration
 * 
 * @author birgitta.boeckeler
 */
public interface TailorConfigurationFactory {

    /**
     * Creates tailor configuration
     * 
     * @param name - configuration name
     * @return - tailor configuration
     */
    TailorConfiguration createTailorConfiguration();

}
