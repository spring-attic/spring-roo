package org.springframework.roo.addon.tailor.actions;

import java.util.Map;
import java.util.logging.Logger;

import org.springframework.roo.addon.tailor.CommandTransformation;
import org.springframework.roo.addon.tailor.util.TailorHelper;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Performs actual action in application logic.
 * 
 * @author Vladimir Tihomirov
 */
public abstract class AbstractAction implements Action {

    private static final Logger LOGGER = HandlerUtils
            .getLogger(AbstractAction.class);

    /**
     * {@inheritDoc}
     */
    public void execute(final CommandTransformation command,
            final ActionConfig config) {
        if (isValid(config)) {
            final ActionConfig processedConfig = processConfigAttributes(
                    command, config);
            executeImpl(command, processedConfig);
        }
        else {
            LOGGER.warning("Invalid configuration for tailor action: " + config);
        }
    }

    /*
     * @see #execute(RooCommand, ActionConfig)
     */
    protected abstract void executeImpl(CommandTransformation command,
            ActionConfig config);

    private ActionConfig processConfigAttributes(
            final CommandTransformation command, final ActionConfig config) {
        // Process variables in config
        final ActionConfig processedConfig = new ActionConfig(
                config.getActionTypeId());
        final Map<String, String> attributes = config.getAttributes();
        for (final Map.Entry<String, String> entry : attributes.entrySet()) {
            final String processedValue = TailorHelper.replaceVars(command,
                    entry.getValue());
            processedConfig.setAttribute(entry.getKey(), processedValue);
        }
        return processedConfig;
    }

}
