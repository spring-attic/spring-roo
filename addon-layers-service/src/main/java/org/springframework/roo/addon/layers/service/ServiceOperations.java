package org.springframework.roo.addon.layers.service;

import org.springframework.roo.model.JavaType;

/**
 * 
 * @author Stefan Schmidt
 * @since 1.2
 */
public interface ServiceOperations {

	boolean isServiceCommandAvailable();
	
	void setupService(JavaType interfaceType, JavaType classType, JavaType domainType);
}
