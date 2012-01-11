package org.springframework.roo.addon.tailor.service;

import java.util.LinkedHashMap;
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
    private final Map<String, TailorConfiguration> configurations = new LinkedHashMap<String, TailorConfiguration>();

    protected void bindConfig(final TailorConfigurationFactory factory) {
        final TailorConfiguration config = factory.createTailorConfiguration();
        if (config != null) {
            if (configurations.get(config.getName()) != null) {
                LOGGER.warning("TailorConfiguration duplicate '"
                        + config.getName() + "', not binding again: "
                        + config.toString());
            }
            configurations.put(config.getName(), config);
        }
    }

    public TailorConfiguration getActiveTailorConfiguration() {
        return configurations.get(activatedTailorConfigName);
    }

    public Map<String, TailorConfiguration> getAvailableConfigurations() {
        return configurations;
    }

    public void setActiveTailorConfiguration(final String name) {
        if (name == null) {
            activatedTailorConfigName = null;
            LOGGER.info("Tailor deactivated");
            return;
        }
        if (configurations.get(name) != null) {
            activatedTailorConfigName = name;
        }
        else {
            LOGGER.severe("Couldn't activate tailor configuration '" + name
                    + "', not available.");
        }
    }

    protected void unbindConfig(final TailorConfigurationFactory factory) {
        // TODO: It's a little unelegant to call "create" method here again, but
        // we need the name...
        final TailorConfiguration config = factory.createTailorConfiguration();
        if (config != null) {
            configurations.remove(config.getName());
        }
    }
}
