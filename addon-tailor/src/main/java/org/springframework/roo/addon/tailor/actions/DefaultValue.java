package org.springframework.roo.addon.tailor.actions;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.tailor.CommandTransformation;

/**
 * Adds default argument to the command If default argument is forced it will be
 * always replaced
 * 
 * @author Vladimir Tihomirov
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
        else if (config.isForced()) {
            final String oldValue = arg.getArguments().get(argumentName);
            if (StringUtils.isNotBlank(oldValue)) {
                // Replace the old value with the default one
                arg.setInputCommand(arg.getInputCommand().replace(
                        "--" + argumentName + " " + oldValue,
                        "--" + argumentName + " " + config.getDefaultValue()));
                arg.getArguments().put(argumentName, config.getDefaultValue());
            }
        }

    }

    public String getDescription(final ActionConfig config) {
        return "Setting default argument: " + config.getArgument() + " = "
                + config.getDefaultValue();
    }

    public boolean isValid(final ActionConfig config) {
        return config != null && StringUtils.isNotBlank(config.getArgument())
                && StringUtils.isNotBlank(config.getDefaultValue());
    }
}
