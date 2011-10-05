package org.springframework.roo.addon.layers.repository.jpa;

import org.springframework.roo.model.JavaType;

/**
 *
 * @author Stefan Schmidt
 * @since 1.2.0
 */
public interface RepositoryJpaOperations {

	boolean isRepositoryCommandAvailable();

	void setupRepository(JavaType interfaceType, JavaType classType, JavaType domainType);
}
