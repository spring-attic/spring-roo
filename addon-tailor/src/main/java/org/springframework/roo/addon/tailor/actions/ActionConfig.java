package org.springframework.roo.addon.tailor.actions;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.roo.addon.tailor.config.CommandConfiguration;
import org.springframework.roo.addon.tailor.service.ActionLocator;

/**
 * Configuration object for an action that can be executed by the a
 * {@link CommandConfiguration}. Provides some default attributes with getters
 * and setters that are used by the default actions provided by tailor-core.
 * Additional custom attributes for new actions can be added via
 * {@link #setAttribute(String, String)}.
 * 
 * @author Birgitta Boeckeler
 * @author Vladimir Tihomirov
 * @since 1.2.0
 */
public class ActionConfig {

    private final String actionTypeId;

    /* A set of attributes for the default actions delivered with tailor.core */
    private static final String ATTR_MODULE = "module";
    private static final String ATTR_ARGUMENT = "argument";
    private static final String ATTR_VALUE = "value";
    private static final String ATTR_FORCE = "force";
    private static final String ATTR_COMMAND = "command";

    /**
     * A map to flexibly define additional attributes for actions not delivered
     * with tailor-core.
     */
    private final Map<String, String> attributes = new LinkedHashMap<String, String>();

    /**
     * Constructor
     * 
     * @param actionClass Action class to be executed
     */
    public ActionConfig(final Class<?> actionClass) {
        actionTypeId = actionClass.getSimpleName();
    }

    /**
     * Constructor
     * 
     * @param actionTypeId ID of the action's type This should be
     *            actionClass#getSimpleName(), as the actions are bound to the
     *            {@link ActionLocator} by class name.
     */
    public ActionConfig(final String actionTypeId) {
        this.actionTypeId = actionTypeId;
    }

    public String getActionTypeId() {
        return actionTypeId;
    }

    public String getArgument() {
        return attributes.get(ATTR_ARGUMENT);
    }

    /**
     * Get an attribute from this configuration
     * 
     * @param key Key of the attribute
     * @return Value of the attribute
     */
    public String getAttribute(final String key) {
        return attributes.get(key);
    }

    /**
     * @return A map to flexibly define additional attributes for actions not
     *         delivered with tailor-core.
     */
    public Map<String, String> getAttributes() {
        return attributes;
    }

    public String getCommand() {
        return attributes.get(ATTR_COMMAND);
    }

    public String getDefaultValue() {
        return attributes.get(ATTR_VALUE);
    }

    public String getModule() {
        return attributes.get(ATTR_MODULE);
    }

    public boolean isForced() {
        final String isForced = attributes.get(ATTR_FORCE);
        return "true".equals(isForced) || "yes".equals(isForced);
    }

    /**
     * Sets an argument value. This method will throw an exception if a value
     * with that key is already set, thus making them immutable. This is to
     * avoid unexpected behaviour, because ActionConfigs in a
     * TailorConfiguration must stay the same once that configuration is
     * initiated at Roo startup.
     * 
     * @param argument Argument name (e.g. used for DefaultValue action to
     *            define a default value for an argument with this name)
     * @see org.springframework.roo.addon.tailor.actions.DefaultValue
     */
    public void setArgument(final String argument) {
        // Don't allow overriding of arguments!
        // The ActionConfig will be reused for all action executions,
        // so it should stay the same after instantiation.
        if (StringUtils.isNotBlank(attributes.get(argument))) {
            throw new IllegalStateException(
                    "ActionConfig.setArgument: ActionConfig attributes are immutable once instantiated!");
        }
        attributes.put(ATTR_ARGUMENT, argument);
    }

    /**
     * Add an additional attribute to this configuration
     * 
     * @param key Key for the attribute
     * @param value Value to set for the attribute
     */
    public void setAttribute(final String key, final String value) {
        attributes.put(key, value);
    }

    /**
     * @param command An additional command to be executed. String can contain
     *            placeholders in the format ${placeholder}, referencing
     *            arguments of the original command.
     * @see org.springframework.roo.addon.tailor.actions.Execute
     */
    public void setCommand(final String command) {
        attributes.put(ATTR_COMMAND, command);
    }

    /**
     * @param isForced Sets an attribute called force, e.g. used for
     *            DefaultValue action to determine if a default value is forced
     *            or only applies when not specified. Default is false.
     * @see org.springframework.roo.addon.tailor.actions.DefaultValue
     */
    public void setForce(final boolean isForced) {
        if (isForced) {
            attributes.put(ATTR_FORCE, "true");
        }
        else {
            attributes.put(ATTR_FORCE, "false");
        }
    }

    /**
     * @param module Name of a module (e.g. used for Focus)
     * @see org.springframework.roo.addon.tailor.actions.Focus
     */
    public void setModule(final String module) {
        attributes.put(ATTR_MODULE, module);
    }

    /**
     * @param value Sets an attribute called value, e.g. used for DefaultValue
     *            action as default value
     * @see org.springframework.roo.addon.tailor.actions.DefaultValue
     */
    public void setValue(final String value) {
        attributes.put(ATTR_VALUE, value);
    }

    @Override
    public String toString() {
        final StringBuffer result = new StringBuffer();
        result.append("Type: " + actionTypeId);
        final Iterator<String> iterator = attributes.keySet().iterator();
        while (iterator.hasNext()) {
            final String attribute = iterator.next();
            result.append(" | ").append(attribute).append(" = ")
                    .append(attributes.get(attribute));
        }
        return result.toString();
    }

}
