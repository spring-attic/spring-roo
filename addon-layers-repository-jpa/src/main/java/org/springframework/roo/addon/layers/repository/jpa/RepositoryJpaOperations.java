package org.springframework.roo.addon.layers.repository.jpa;

import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Feature;

/**
 * @author Stefan Schmidt
 * @since 1.2.0
 */
public interface RepositoryJpaOperations extends Feature {

    boolean isRepositoryInstallationPossible();

    void setupRepository(JavaType interfaceType, JavaType classType);
}
