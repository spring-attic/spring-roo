package org.springframework.roo.addon.tailor.actions;

/**
 * A set of static methods to create ActionConfig objects for each of the
 * actions delivered with tailor-core.
 * 
 * @author Birgitta Boeckeler
 * @author Vladimir Tihomirov
 * @since 1.2.0
 */
public class ActionConfigFactory {

    /**
     * @see Focus
     */
    public static ActionConfig focusModuleAction(String module) {
        ActionConfig config = new ActionConfig(ActionType.FOCUS.getActionId());
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
            String defaultValue, boolean force) {
        ActionConfig config = new ActionConfig(
                ActionType.DEFAULTVALUE.getActionId());
        config.setArgument(argument);
        config.setValue(defaultValue);
        config.setForce(force);
        return config;
    }

    /**
     * Creates an empty execute commmand (if left unchanged, this action will
     * lead to execution of the original input command)
     * 
     * @return new ActionConfig
     */
    public static ActionConfig executeAction() {
        ActionConfig config = new ActionConfig(ActionType.EXECUTE.getActionId());
        return config;
    }

    public static ActionConfig executeAction(String command) {
        ActionConfig config = new ActionConfig(ActionType.EXECUTE.getActionId());
        config.setCommand(command);
        return config;
    }

}
