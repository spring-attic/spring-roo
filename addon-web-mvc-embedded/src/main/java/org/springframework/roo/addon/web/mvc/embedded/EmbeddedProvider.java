package org.springframework.roo.addon.web.mvc.embedded;

import java.util.Map;

/**
 * Addon extension point interface. Implement this to add new embeddable
 * providers by taking advantage of the infrastructure provided by this addon.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
public interface EmbeddedProvider {

    /**
     * Create a embed page via a generic URL supplied to this command.
     * 
     * @param url the URL to be inspected
     * @param viewName the name for the resulting jspx page (optional)
     * @return true if this addon can handle the URL offered (otherwise you MUST
     *         return false so other addons can provide the implementation for
     *         the URL provided)
     */
    boolean embed(String url, String viewName);

    /**
     * Implement this method to provide alternative (to the generic URL-based
     * approach) offered by embed method.
     * 
     * @param viewName viewName the name for the resulting jspx page (optional)
     * @param options a map of options to be consumed by the addon
     * @return true if this addon can handle the options offered (otherwise you
     *         MUST return false so other addons can provide the implementation
     *         for the options provided)
     */
    boolean install(String viewName, Map<String, String> options);
}
