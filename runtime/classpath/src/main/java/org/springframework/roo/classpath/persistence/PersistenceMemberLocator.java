package org.springframework.roo.classpath.persistence;

import java.util.List;

import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.model.JavaType;

/**
 * Provides metadata about persistence-related members of domain types.
 * 
 * @author Stefan Schmidt
 * @author Andrew Swan
 * @since 1.2.0
 */
public interface PersistenceMemberLocator {

    /**
     * Locates embedded identifier types for a given domain type.
     * 
     * @param domainType The domain type (needs to be part of the project)
     * @return a list of identifier fields (not null, may be empty)
     */
    List<FieldMetadata> getEmbeddedIdentifierFields(JavaType domainType);

    /**
     * Returns the ID accessor for the given domain type
     * 
     * @param domainType the domain type (can be <code>null</code>)
     * @return <code>null</code> if the given type is <code>null</code> or does
     *         not have an ID accessor
     */
    MethodMetadata getIdentifierAccessor(JavaType domainType);

    /**
     * Returns the identifier fields of the given domain type.
     * 
     * @param domainType The domain type (can be <code>null</code>)
     * @return a list of identifier fields (not null, may be empty)
     */
    List<FieldMetadata> getIdentifierFields(JavaType domainType);

    /**
     * Returns the identifier type of the given domain type.
     * 
     * @param domainType The domain type (can be <code>null</code>)
     * @return the identifier type (may be null)
     */
    JavaType getIdentifierType(JavaType domainType);

    /**
     * Returns the version accessor for the given domain type.
     * 
     * @param domainType the domain type (can be <code>null</code>)
     * @return <code>null</code> if the given type is <code>null</code> or does
     *         not have a version accessor
     */
    MethodMetadata getVersionAccessor(JavaType domainType);

    /**
     * Locates the version field for a given domain type.
     * 
     * @param domainType The domain type (needs to be part of the project)
     * @return a version field (may be null)
     */
    FieldMetadata getVersionField(JavaType domainType);
}
