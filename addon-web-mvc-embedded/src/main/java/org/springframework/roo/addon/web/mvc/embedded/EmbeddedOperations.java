package org.springframework.roo.addon.web.mvc.embedded;

import java.util.Map;

/**
 * Provides operations for the web mvc embed addon.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
public interface EmbeddedOperations {

    boolean embed(String url, String viewName);

    boolean install(String viewName, Map<String, String> options);

    boolean isEmbeddedInstallationPossible();
}
