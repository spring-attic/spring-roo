package org.springframework.roo.addon.tailor.service;

import java.util.Map;

import org.springframework.roo.addon.tailor.config.TailorConfiguration;

/**
 * Locates and binds all {@link TailorConfiguration} implementations in the
 * container. Holds the information which of these configurations is currently
 * activated.
 * 
 * @author Birgitta Boeckeler
 */
public interface ConfigurationLocator {

    /**
     * @return the currently active TailorConfiguration
     */
    TailorConfiguration getActiveTailorConfiguration();

    /**
     * @return all available {@link TailorConfiguration} instances
     */
    Map<String, TailorConfiguration> getAvailableConfigurations();

    /**
     * Activate Tailor Configuration with certain name
     * 
     * @param name Name of configuration to be activated
     */
    void setActiveTailorConfiguration(String name);

}
