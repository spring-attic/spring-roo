package org.springframework.roo.addon.web.mvc.embedded;

import java.util.Map;

/**
 * Operations interface for web mvc embed addon.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 *
 */
public interface EmbeddedOperations {
	
	boolean isCommandAvailable();

	boolean embed(String url, String viewName);
	
	boolean install(String viewName, Map<String, String> options);
}
