package org.springframework.roo.addon.web.mvc.controller.finder;

import org.springframework.roo.model.JavaType;

/**
 *  Operations for Web MVC finder functionality.
 * 
 * @author Stefan Schmidt
 * @since 1.2.0
 */
public interface WebFinderOperations {
	
	boolean isCommandAvailable();
	
	void annotateAll();
	
	void annotateType(JavaType controllerType, JavaType entityType);
	
}
