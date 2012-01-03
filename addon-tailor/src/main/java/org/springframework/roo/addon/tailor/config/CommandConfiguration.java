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

    /**
     * Name of the command that will trigger this configuration
     */
    private String commandName;

    private List<ActionConfig> actions = new ArrayList<ActionConfig>();

    public String getCommandName() {
        return commandName;
    }

    public void setCommandName(String commandName) {
        this.commandName = commandName;
    }

    public void addAction(ActionConfig actionConfig) {
        this.actions.add(actionConfig);
    }

    public List<ActionConfig> getActions() {
        return actions;
    }
}
