package org.springframework.roo.addon.tailor.actions;

/**
 * Predefined action types. Can be used to avoid spelling mistakes for the
 * action types when creating a TailorConfiguration with Java.
 * 
 * @author birgitta.boeckeler
 */
public enum ActionType {

    DEFAULTVALUE(DefaultValue.class), EXECUTESELF(ExecuteSelf.class), FOCUSMODULE(
            FocusModule.class), EXECUTECOMMAND(ExecuteCommand.class);

    Class<?> actionClass;

    ActionType(Class<?> actionClass) {
        this.actionClass = actionClass;
    }

    public String getActionId() {
        return this.actionClass.getSimpleName();
    }

}
