package org.springframework.roo.addon.tailor.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Data container for a tailor configuration. Defines a set of
 * {@link CommandConfiguration} objects that define which actions should be
 * triggered by which commands when this configuration is activated
 * 
 * @author Birgitta Boeckeler
 * @since 1.2.0
 */
public class TailorConfiguration {

    private final List<CommandConfiguration> commandConfigs = new ArrayList<CommandConfiguration>();

    private final String name;

    private String description;

    private boolean isActive = false;

    /**
     * Constructor
     * 
     * @param name Name of the configuration. Should be unique over all
     *            TailorConfiguration instances in the container
     */
    public TailorConfiguration(final String name) {
        this.name = name;
    }

    public TailorConfiguration(final String name, final String description) {
        this.name = name;
        this.description = description;
    }

    public void addCommandConfig(final CommandConfiguration newConfig) {
        commandConfigs.add(newConfig);
    }

    /**
     * Looks up the CommandConfiguration for a specific command.
     * 
     * @param fullCommandString The command string to check
     * @return CommandConfiguration for the command in this TailorConfiguration;
     *         null if no configuration present for the command
     */
    public CommandConfiguration getCommandConfigFor(
            final String fullCommandString) {
        for (final CommandConfiguration config : commandConfigs) {
            if (fullCommandString.startsWith(config.getCommandName())) {
                return config;
            }
        }
        return null;
    }

    public List<CommandConfiguration> getCommandConfigs() {
        return commandConfigs;
    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(final boolean isActive) {
        this.isActive = isActive;
    }

}
