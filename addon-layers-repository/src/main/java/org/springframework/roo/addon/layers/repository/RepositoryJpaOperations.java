package org.springframework.roo.addon.layers.repository;

import org.springframework.roo.model.JavaType;

/**
 * 
 * @author Stefan Schmidt
 * @since 1.2
 */
public interface RepositoryJpaOperations {

	boolean isRepositoryCommandAvailable();
	
	void setupRepository(JavaType interfaceType, JavaType classType, JavaType domainType);
}
