package org.springframework.roo.addon.tailor.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.addon.tailor.actions.ActionConfig;

/**
 * Contains configuration (list of actions) for certain roo command
 * 
 * @author birgitta.boeckeler
 */
public class CommandConfiguration {

    private final List<ActionConfig> actions = new ArrayList<ActionConfig>();

    /**
     * Name of the command that will trigger this configuration
     */
    private String commandName;

    public void addAction(final ActionConfig actionConfig) {
        actions.add(actionConfig);
    }

    public List<ActionConfig> getActions() {
        return actions;
    }

    public String getCommandName() {
        return commandName;
    }

    public void setCommandName(final String commandName) {
        this.commandName = commandName;
    }
}
