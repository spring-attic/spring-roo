package org.springframework.roo.addon.tailor;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.tailor.actions.Action;
import org.springframework.roo.addon.tailor.actions.ActionConfig;
import org.springframework.roo.addon.tailor.config.CommandConfiguration;
import org.springframework.roo.addon.tailor.config.TailorConfiguration;
import org.springframework.roo.addon.tailor.service.ActionLocator;
import org.springframework.roo.addon.tailor.service.ConfigurationLocator;
import org.springframework.roo.shell.AbstractShell;
import org.springframework.roo.shell.Tailor;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.StringUtils;

/**
 * Executed by {@link AbstractShell} Triggers action execution
 * 
 * @author vladimir.tihomirov
 */
@Service
@Component
public class DefaultTailorImpl implements Tailor {
    private static final Logger LOGGER = HandlerUtils
            .getLogger(DefaultTailorImpl.class);
    @Reference private ActionLocator actionLocator;

    @Reference private ConfigurationLocator configLocator;

    private void execute(final CommandTransformation commandTrafo) {
        final TailorConfiguration configuration = configLocator
                .getActiveTailorConfiguration();
        if (configuration == null) {
            return;
        }
        final CommandConfiguration commandConfig = configuration
                .getCommandConfigFor(commandTrafo.getInputCommand());
        if (commandConfig == null) {
            return;
        }
        logInDevelopmentMode(Level.INFO,
                "Tailor: detected " + commandTrafo.getInputCommand());

        for (final ActionConfig config : commandConfig.getActions()) {
            final Action component = actionLocator.getAction(config
                    .getActionTypeId());
            if (component != null) {
                logInDevelopmentMode(Level.INFO,
                        "\tTailoring: " + component.getDescription(config));
                component.execute(commandTrafo, config);
            }
            else {
                logInDevelopmentMode(
                        Level.WARNING,
                        "\tTailoring: Couldn't find action '"
                                + config.getActionTypeId());
            }
        }
    }

    protected void logInDevelopmentMode(final Level level, final String logMsg) {
        LOGGER.log(level, logMsg);
    }

    /**
     * @Inheritdoc
     */
    public List<String> sew(final String command) {
        if (StringUtils.hasText(command)) {
            final CommandTransformation commandTrafo = new CommandTransformation(
                    command);
            try {
                execute(commandTrafo);
            }
            catch (final RuntimeException e) {
                commandTrafo.clearCommands();
                commandTrafo
                        .addOutputCommand("/* Unable to tailor this command. Please check the command syntax */");
                logInDevelopmentMode(Level.WARNING, e.toString());
            }
            return commandTrafo.getOutputCommands();
        }
        return Collections.emptyList();
    }

}
