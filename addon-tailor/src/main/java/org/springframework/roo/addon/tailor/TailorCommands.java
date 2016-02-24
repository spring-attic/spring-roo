package org.springframework.roo.addon.tailor;

import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.tailor.config.TailorConfiguration;
import org.springframework.roo.addon.tailor.service.ConfigurationLocator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Commands to list, activate and deactivate tailor configurations
 * 
 * @author Birgitta Boeckeler
 */
@Component
@Service
public class TailorCommands implements CommandMarker {

    private static final Logger LOGGER = HandlerUtils
            .getLogger(TailorCommands.class);
    @Reference ConfigurationLocator configLocator;

    /**
     * This method activates a tailor configuration by its name (Name needs to
     * be listed with "tailor list" command
     */
    @CliCommand(value = "tailor activate", help = "Activate a tailor configuration.")
    public void tailorActivate(
            @CliOption(key = { "name" }, mandatory = true, help = "The name of the tailor configuration") final String tailorName) {
        configLocator.setActiveTailorConfiguration(tailorName);
    }

    /**
     * This method deactivates the current tailor
     */
    @CliCommand(value = "tailor deactivate", help = "Deactivate the tailor.")
    public void tailorDeactivate() {
        configLocator.setActiveTailorConfiguration(null);
    }

    /**
     * This method lists all available tailor configurations in the the Roo
     * shell.
     * 
     * @param type
     */
    @CliCommand(value = "tailor list", help = "List available tailor configurations.")
    public void tailorList() {
        LOGGER.info("Available tailor configurations: ");
        final Map<String, TailorConfiguration> configs = configLocator
                .getAvailableConfigurations();
        final TailorConfiguration activeConfig = configLocator
                .getActiveTailorConfiguration();
        final Iterator<String> iterator = configs.keySet().iterator();
        while (iterator.hasNext()) {
            final String configName = iterator.next();
            final String isActive = activeConfig != null
                    && configName.equals(activeConfig.getName()) ? " [ ACTIVE ] "
                    : "";
            LOGGER.info("\to " + configName + isActive + " - "
                    + configs.get(configName).getDescription());
        }
    }

}