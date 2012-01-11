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
     * @see ActionConfig#setArgument(String)
     * @see ActionConfig#setValue(String)
     */
    public static ActionConfig defaultArgumentAction(final String argument,
            final String defaultValue) {
        return defaultArgumentAction(argument, defaultValue, false);
    }

    public static ActionConfig defaultArgumentAction(final String argument,
            final String defaultValue, final boolean mandatory) {
        final ActionConfig config = new ActionConfig(
                ActionType.DEFAULTVALUE.getActionId());
        config.setArgument(argument);
        config.setValue(defaultValue);
        config.setMandatory(mandatory);
        return config;
    }

    public static ActionConfig executeCommandAction(final String command) {
        final ActionConfig config = new ActionConfig(
                ActionType.EXECUTECOMMAND.getActionId());
        config.setCommand(command);
        return config;
    }

    public static ActionConfig executeSelfAction() {
        final ActionConfig config = new ActionConfig(
                ActionType.EXECUTESELF.getActionId());
        return config;
    }

    /**
     * @see FocusModule
     */
    public static ActionConfig focusModuleAction(final String module) {
        final ActionConfig config = new ActionConfig(
                ActionType.FOCUSMODULE.getActionId());
        config.setModule(module);
        return config;
    }

}
