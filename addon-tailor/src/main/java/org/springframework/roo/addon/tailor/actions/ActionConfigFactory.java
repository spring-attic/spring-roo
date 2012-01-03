package org.springframework.roo.addon.tailor.actions;


/**
 * A set of static methods to create ActionConfig objects for
 * each of the actions delivered with tailor-core.
 * 
 * @author Birgitta Boeckeler
 * @author Vladimir Tihomirov
 * @since 1.2.0
 */
public class ActionConfigFactory {
	
	/**
	 * @see FocusModule
	 */
	public static ActionConfig focusModuleAction(String module) {
		ActionConfig config = new ActionConfig(
				ActionType.FOCUSMODULE.getActionId());
		config.setModule(module);
		return config;
	}

	/**
	 * @see ActionConfig#setArgument(String)
	 * @see ActionConfig#setValue(String)
	 */
	public static ActionConfig defaultArgumentAction(String argument,
			String defaultValue) {
		return defaultArgumentAction(argument, defaultValue, false);
	}

	public static ActionConfig defaultArgumentAction(String argument,
			String defaultValue, boolean mandatory) {
		ActionConfig config = new ActionConfig(
				ActionType.DEFAULTVALUE.getActionId());
		config.setArgument(argument);
		config.setValue(defaultValue);
		config.setMandatory(mandatory);
		return config;
	}

	public static ActionConfig executeSelfAction() {
		ActionConfig config = new ActionConfig(
				ActionType.EXECUTESELF.getActionId());
		return config;
	}

	public static ActionConfig executeCommandAction(String command) {
		ActionConfig config = new ActionConfig(
				ActionType.EXECUTECOMMAND.getActionId());
		config.setCommand(command);
		return config;
	}

}
