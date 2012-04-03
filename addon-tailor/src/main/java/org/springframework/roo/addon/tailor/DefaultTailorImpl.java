package org.springframework.roo.addon.tailor;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.tailor.actions.Action;
import org.springframework.roo.addon.tailor.actions.ActionConfig;
import org.springframework.roo.addon.tailor.config.CommandConfiguration;
import org.springframework.roo.addon.tailor.config.TailorConfiguration;
import org.springframework.roo.addon.tailor.service.ActionLocator;
import org.springframework.roo.addon.tailor.service.ConfigurationLocator;
import org.springframework.roo.addon.tailor.util.CommentedLine;
import org.springframework.roo.addon.tailor.util.TailorHelper;
import org.springframework.roo.shell.AbstractShell;
import org.springframework.roo.shell.Shell;
import org.springframework.roo.shell.Tailor;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Executed by {@link AbstractShell}. Triggers execution of configured actions
 * 
 * @author Vladimir Tihomirov
 */
@Service
@Component(immediate = true)
public class DefaultTailorImpl implements Tailor {
    @Reference protected ActionLocator actionLocator;
    @Reference protected ConfigurationLocator configLocator;
    @Reference protected Shell shell;

    private static final Logger LOGGER = HandlerUtils
            .getLogger(DefaultTailorImpl.class);
    protected boolean inBlockComment = false;

    /**
     * @Inheritdoc
     */
    public List<String> sew(String command) {
        if (StringUtils.isBlank(command)) {
            return Collections.emptyList();
        }
        try {
            // validate if it is commented
            final CommentedLine comment = new CommentedLine(command,
                    inBlockComment);
            TailorHelper.removeComment(comment);
            inBlockComment = comment.getInBlockComment();
            command = comment.getLine();
            if (StringUtils.isBlank(command)) {
                return Collections.emptyList();
            }
            // parse and tailor
            final CommandTransformation commandTrafo = new CommandTransformation(
                    command);
            execute(commandTrafo);
            return commandTrafo.getOutputCommands();
        }
        catch (final Exception e) {
            // Do nothing if exception happened
            LOGGER.log(
                    Level.WARNING,
                    "Error tailoring, cancelled command execution: "
                            + e.getMessage());
            return Collections.emptyList();
        }
    }

    // We have to done explicit injection to support API compatibility with STS
    // shell
    protected void activate(final ComponentContext context) {
        if (shell != null) {
            shell.setTailor(this);
        }
    }

    protected void deactivate(final ComponentContext context) {
        if (shell != null) {
            shell.setTailor(null);
        }
    }

    protected void logInDevelopmentMode(final Level level, final String logMsg) {
        if (shell.isDevelopmentMode()) {
            LOGGER.log(level, logMsg);
        }
    }

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
}
