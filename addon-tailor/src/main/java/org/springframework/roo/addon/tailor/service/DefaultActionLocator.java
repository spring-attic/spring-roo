package org.springframework.roo.addon.tailor.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.tailor.actions.Action;

/**
 * Locates all available actions => all OSGi Services of type {@link Action}
 * 
 * @author Vladimir Tihomirov
 */
@Component(immediate = true)
@Service
@Reference(name = "action", strategy = ReferenceStrategy.EVENT, policy = ReferencePolicy.DYNAMIC, referenceInterface = Action.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE)
public class DefaultActionLocator implements ActionLocator {

    /**
     * A map of all the actions found in the OSGi container. Bound dynamically
     * by Felix, keys are the simple class names in lower case, values the
     * respective OSGi services.
     */
    private final Map<String, Action> actionsMap = new LinkedHashMap<String, Action>();

    public Action getAction(final String caseInsensitiveKey) {
        return actionsMap.get(caseInsensitiveKey.toLowerCase());
    }

    public Map<String, Action> getAllActions() {
        return actionsMap;
    }

    protected void bindAction(final Action action) {
        final String actionClassName = action.getClass().getSimpleName()
                .toLowerCase();
        actionsMap.put(actionClassName, action);
    }

    protected void unbindAction(final Action action) {
        final String actionClassName = action.getClass().getSimpleName()
                .toLowerCase();
        actionsMap.remove(actionClassName);
    }

}
