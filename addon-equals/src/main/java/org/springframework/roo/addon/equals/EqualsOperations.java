package org.springframework.roo.addon.equals;

import java.util.Set;

import org.springframework.roo.model.JavaType;

/**
 * Provides equals and hashCode method operations.
 * 
 * @author Alan Stewart
 * @since 1.2.0
 */
public interface EqualsOperations {

    void addEqualsAndHashCodeMethods(final JavaType javaType,
            boolean appendSuper, final Set<String> excludeFields);
}
