package org.springframework.roo.addon.tailor.actions;

import java.util.Iterator;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.tailor.CommandTransformation;
import org.springframework.roo.support.util.StringUtils;

/**
 * Schedules the original input command for execution. The purpose of this
 * action is to have the possibility to determine the order of the execution of
 * the original command in the list of commands added by the tailor.
 * 
 * @author Vladimir Tihomirov
 * @author Birgitta Boeckeler
 * @since 1.2.0
 */
@Component
@Service
public class ExecuteSelf extends AbstractAction {

    private static final String ACTIONATTR_REMOVEARGS = "without";

    /**
     * {@inheritDoc}
     */
    @Override
    public void executeImpl(final CommandTransformation trafo,
            final ActionConfig config) {
        final String processedCommand = removeArguments(trafo, config);
        trafo.addOutputCommand(processedCommand);
    }

    public String getDescription(final ActionConfig config) {
        return "Executing original command";
    }

    public boolean isValid(final ActionConfig config) {
        return config != null;
    }

    private String removeArguments(final CommandTransformation trafo,
            final ActionConfig config) {
        final String removeArgumentsAttribute = config
                .getAttribute(ACTIONATTR_REMOVEARGS);
        if (!StringUtils.hasText(removeArgumentsAttribute)) {
            return trafo.getInputCommand();
        }

        String inputCommandString = trafo.getInputCommand();

        final String[] removeArgumentsList = removeArgumentsAttribute
                .split(",");
        for (final String element : removeArgumentsList) {
            String argToRemove = element;

            if (argToRemove.startsWith("--")) {
                argToRemove = argToRemove.substring(2);
            }

            final Map<String, String> cmdArguments = trafo.getArguments();
            final Iterator<String> keyIterator = cmdArguments.keySet()
                    .iterator();
            while (keyIterator.hasNext()) {
                final String argName = keyIterator.next();
                if (argName.equals(argToRemove)) {
                    inputCommandString = inputCommandString.replace("--"
                            + argName + " " + cmdArguments.get(argName), "");
                }
            }
        }

        return inputCommandString;
    }

}
