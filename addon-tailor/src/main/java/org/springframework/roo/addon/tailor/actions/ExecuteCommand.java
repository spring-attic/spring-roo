package org.springframework.roo.addon.tailor.actions;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.tailor.CommandTransformation;
import org.springframework.roo.addon.tailor.utils.TailorHelper;
import org.springframework.roo.support.util.StringUtils;

/**
 * Schedules command for execution
 * 
 * @author vladimir.tihomirov
 */
@Component
@Service
public class ExecuteCommand extends AbstractAction {

    @Override
    public void executeImpl(CommandTransformation trafo, ActionConfig config) {
        trafo.addOutputCommand(TailorHelper.replaceVars(trafo,
                config.getCommand()));
    }

    public String getDescription(ActionConfig config) {
        return "Executing command: " + config.getCommand();
    }

    public boolean isValid(ActionConfig config) {
        return config != null && StringUtils.hasText(config.getCommand());
    }

}
