package org.springframework.roo.addon.tailor.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.tailor.config.TailorConfiguration;
import org.springframework.roo.addon.tailor.config.TailorConfigurationFactory;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.CollectionUtils;

/**
 * Default implementation of {@link ConfigurationLocator}
 * 
 * @author Birgitta Boeckeler
 * @since 1.2.0
 */
@Component(immediate = true)
@Service
@Reference(name = "config", strategy = ReferenceStrategy.EVENT, policy = ReferencePolicy.DYNAMIC, referenceInterface = TailorConfigurationFactory.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE)
public class DefaultConfigurationLocator implements ConfigurationLocator {

    private static final Logger LOGGER = HandlerUtils
            .getLogger(DefaultConfigurationLocator.class);

    /**
     * Name of currently activated configuration
     */
    private String activatedTailorConfigName = null;

    /**
     * Map of all available configurations - dynamically bound by Felix on
     * startup
     */
    private Map<String, TailorConfiguration> configurations = new LinkedHashMap<String, TailorConfiguration>();

    protected void bindConfig(TailorConfigurationFactory factory) {
        List<TailorConfiguration> configs = factory.createTailorConfiguration();
        if (CollectionUtils.isEmpty(configs)) {
            return;
        }
        for (TailorConfiguration config : configs) {
            if (configurations.get(config.getName()) != null) {
                LOGGER.warning("TailorConfiguration duplicate '"
                        + config.getName() + "', not binding again: "
                        + config.toString());
            }
            if (config.isActive()) {
                this.activatedTailorConfigName = config.getName();
            }
            configurations.put(config.getName(), config);
        }
    }

    protected void unbindConfig(TailorConfigurationFactory factory) {
        // TODO It's a little unelegant to call "create" method here again, but
        // we need the name...
        List<TailorConfiguration> configs = factory.createTailorConfiguration();
        if (CollectionUtils.isEmpty(configs)) {
            return;
        }
        for (TailorConfiguration config : configs) {
            configurations.remove(config.getName());
        }
    }

    public void setActiveTailorConfiguration(String name) {
        if (name == null) {
            this.activatedTailorConfigName = null;
            LOGGER.info("Tailor deactivated");
            return;
        }
        if (this.configurations.get(name) != null) {
            this.activatedTailorConfigName = name;
        }
        else {
            LOGGER.severe("Couldn't activate tailor configuration '" + name
                    + "', not available.");
        }
    }

    public TailorConfiguration getActiveTailorConfiguration() {

        return this.configurations.get(this.activatedTailorConfigName);
    }

    public Map<String, TailorConfiguration> getAvailableConfigurations() {

        return this.configurations;
    }

}
