package org.springframework.roo.addon.tailor.actions;

import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.tailor.CommandTransformation;

/**
 * Schedules command for execution.
 * 
 * @author Vladimir Tihomirov
 * @author Birgitta Boeckeler
 * @since 1.3.0
 */
@Component
@Service
public class Execute extends AbstractAction {

    public static final String ACTIONATTR_REMOVEARGS = "exclude";

    @Override
    public void executeImpl(CommandTransformation trafo, ActionConfig config) {
        if (StringUtils.isBlank(config.getCommand())) {
            // If no command specified, this will execute the original command
            String processedCommand = removeArgumentsFromInputCmd(trafo, config);
            trafo.addOutputCommand(processedCommand);
        }
        else {
            trafo.addOutputCommand(config.getCommand());
        }
    }

    /**
     * Based on the value of ({@value #ACTIONATTR_REMOVEARGS}) in the action
     * configuration, this method removes arguments from the input command in
     * trafo.
     * 
     * @param trafo Transformation object
     * @param config Action configuration
     * @return Processed input command
     */
    private String removeArgumentsFromInputCmd(CommandTransformation trafo,
            ActionConfig config) {
        String removeArgumentsAttribute = config
                .getAttribute(ACTIONATTR_REMOVEARGS);
        if (StringUtils.isBlank(removeArgumentsAttribute)) {
            return trafo.getInputCommand();
        }

        String inputCommandString = trafo.getInputCommand();

        String[] removeArgumentsList = removeArgumentsAttribute.split(",");
        for (int i = 0; i < removeArgumentsList.length; i++) {
            String argToRemove = removeArgumentsList[i];

            if (argToRemove.startsWith("--")) {
                argToRemove = argToRemove.substring(2);
            }

            Map<String, String> cmdArguments = trafo.getArguments();
            Iterator<String> keyIterator = cmdArguments.keySet().iterator();
            while (keyIterator.hasNext()) {
                String argName = keyIterator.next();
                if (argName.equals(argToRemove)) {
                    inputCommandString = inputCommandString.replace("--"
                            + argName + " " + cmdArguments.get(argName), "");
                }
            }
        }

        return inputCommandString;
    }

    public String getDescription(ActionConfig config) {
        if (StringUtils.isEmpty(config.getCommand())) {
            return "Executing original command";
        }
        return "Executing command: " + config.getCommand();
    }

    public boolean isValid(ActionConfig config) {
        return config != null
                // "excludes" option only valid if "command" is empty
                && !(StringUtils.isNotBlank(config
                        .getAttribute(ACTIONATTR_REMOVEARGS)) && StringUtils
                        .isNotBlank(config.getCommand()));
    }
}
