package org.springframework.roo.addon.tailor.actions;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.tailor.CommandTransformation;
import org.springframework.roo.addon.tailor.util.TailorHelper;

/**
 * Schedules command for execution.
 * 
 * @author Vladimir Tihomirov
 * @since 1.3.0
 */
@Component
@Service
public class ExecuteCommand extends AbstractAction {

    @Override
    public void executeImpl(final CommandTransformation trafo,
            final ActionConfig config) {
        trafo.addOutputCommand(TailorHelper.replaceVars(trafo,
                config.getCommand()));
    }

    public String getDescription(final ActionConfig config) {
        return "Executing command: " + config.getCommand();
    }

    public boolean isValid(final ActionConfig config) {
        return config != null && StringUtils.isNotBlank(config.getCommand());
    }

}
