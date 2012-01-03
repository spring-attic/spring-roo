package org.springframework.roo.addon.tailor.actions;

import java.util.Iterator;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.tailor.CommandTransformation;
import org.springframework.roo.support.util.StringUtils;

/**
 * Schedules the original input command for execution.
 * 
 * The purpose of this action is to have the possibility 
 * to determine the order of the execution of the original 
 * command in the list of commands added by the tailor.  
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
	public void executeImpl(CommandTransformation trafo, ActionConfig config) {
		String processedCommand = removeArguments(trafo, config);
		trafo.addOutputCommand(processedCommand);
	}

	private String removeArguments(CommandTransformation trafo, ActionConfig config) {
		String removeArgumentsAttribute = config.getAttribute(ACTIONATTR_REMOVEARGS);
		if (!StringUtils.hasText(removeArgumentsAttribute)) {
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
		return "Executing original command";
	}

	public boolean isValid(ActionConfig config) {
		return config != null;
	}
	
}
