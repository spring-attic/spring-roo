package org.springframework.roo.addon.tailor.actions;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.tailor.CommandTransformation;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.StringUtils;

/**
 * Focuses on a module with a given name.
 * This action does not check for the EXACT name, but looks for a module that
 * contains that string.
 * 
 * @author vladimir.tihomirov
 */
@Component
@Service
public class FocusModule extends AbstractAction {
	
	@Reference
	ProjectOperations projectOperations;

	private final String baseCommand = "module focus --moduleName ";
	
	@Override
	public void executeImpl(CommandTransformation trafo, ActionConfig config) {
		if ("~".equals(config.getModule())) {
			trafo.addOutputCommand(baseCommand, "~");
			return;
		}
		// If not root: Check if module name actually exists
		for (String moduleName : projectOperations.getModuleNames()) {
			if (moduleName.contains(config.getModule())) {
				trafo.addOutputCommand(baseCommand, moduleName);
				return;
			}
		}
	}

	public String getDescription(ActionConfig config) {
		return "Focusing module: " + config.getModule();
	}
	
	public boolean isValid(ActionConfig config) {
		return config != null 
			&& StringUtils.hasText(config.getModule());
	}
	
}
