package org.springframework.roo.addon.tailor.actions;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.tailor.CommandTransformation;
import org.springframework.roo.support.util.StringUtils;

/**
 * Adds default argument to the command If default argument is mandatory it will
 * be always replaced
 * 
 * @author vladimir.tihomirov
 */
@Component
@Service
public class DefaultValue extends AbstractAction {

    @Override
    public void executeImpl(final CommandTransformation arg,
            final ActionConfig config) {
        // Allow argument name with and without "--" in config
        String argumentName = config.getArgument();
        if (argumentName.startsWith("--")) {
            argumentName = argumentName.substring(2);
        }
        // Change both the command string and update the arguments
        if (!arg.getInputCommand().contains("--" + argumentName)) {
            arg.setInputCommand(arg.getInputCommand().concat(" --")
                    .concat(argumentName).concat(" ")
                    .concat(config.getDefaultValue()));
            // Update the arguments, so that subsequent actions will be based on
            // this default value
            arg.getArguments().put(argumentName, config.getDefaultValue());
        }
        else if (config.isMandatory()) {
            // TODO replace value instead of appending it
        }

    }

    public String getDescription(final ActionConfig config) {
        return "Setting default argument: " + config.getArgument() + " = "
                + config.getDefaultValue();
    }

    public boolean isValid(final ActionConfig config) {
        return config != null && StringUtils.hasText(config.getArgument())
                && StringUtils.hasText(config.getDefaultValue());
    }
}
