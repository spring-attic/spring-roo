package org.springframework.roo.addon.tailor.actions;

import org.springframework.roo.addon.tailor.CommandTransformation;

/**
 * Base interface of action hierarchy. Used for dynamic binding of available
 * actions.
 * 
 * <pre>
 * To implement a new Action:
 * - Create a Component Service that extend AbstractAction
 * - Create a static method in there that creates an ActionConfig for the new Action.
 *   This method defines the "interface" for this action: What data does the execute method
 *   need in addition to the data {@link CommandTransformation#getInputCommand()}?
 * - Implement the execute method: Read the attributes created with the static factory
 *   method, execute the action.
 * </pre>
 * 
 * @author Vladimir Tihomirov
 */
public interface Action {

    /**
     * Triggers action execution
     * 
     * @param command - resource to be processed
     * @param config - configuration of action
     */
    void execute(CommandTransformation command, ActionConfig config);

    /**
     * Action info
     * 
     * @param config - configuration of action
     * @return description of actual action
     */
    String getDescription(ActionConfig config);

    /**
     * Checks if an ActionConfig is valid for an action execution.
     * 
     * @param config will be checked
     * @return true if valid, otherwise false
     */
    boolean isValid(ActionConfig config);
}
