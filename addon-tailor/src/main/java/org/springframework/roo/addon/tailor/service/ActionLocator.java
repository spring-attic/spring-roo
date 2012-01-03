package org.springframework.roo.addon.tailor.service;

import java.util.Map;

import org.springframework.roo.addon.tailor.actions.Action;

/**
 * Locates all actions
 * 
 * @author vladimir.tihomirov
 */
public interface ActionLocator {

    /**
     * Get all available actions
     * 
     * @return map of actions
     */
    Map<String, Action> getAllActions();

    Action getAction(String caseInsensitiveKey);
}
