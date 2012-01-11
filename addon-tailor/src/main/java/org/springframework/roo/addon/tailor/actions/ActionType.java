package org.springframework.roo.addon.tailor.actions;

/**
 * Predefined action types. Can be used to avoid spelling mistakes for the
 * action types when creating a TailorConfiguration with Java.
 * 
 * @author birgitta.boeckeler
 */
public enum ActionType {

    DEFAULTVALUE(DefaultValue.class), EXECUTECOMMAND(ExecuteCommand.class), EXECUTESELF(
            ExecuteSelf.class), FOCUSMODULE(FocusModule.class);

    Class<?> actionClass;

    ActionType(final Class<?> actionClass) {
        this.actionClass = actionClass;
    }

    public String getActionId() {
        return actionClass.getSimpleName();
    }

}
