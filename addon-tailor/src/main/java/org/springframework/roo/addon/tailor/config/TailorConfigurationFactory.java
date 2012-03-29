package org.springframework.roo.addon.tailor.config;

import java.util.List;

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
    List<TailorConfiguration> createTailorConfiguration();

}
