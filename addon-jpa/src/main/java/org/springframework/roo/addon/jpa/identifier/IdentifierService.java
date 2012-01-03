package org.springframework.roo.addon.jpa.identifier;

import java.util.List;

import org.springframework.roo.model.JavaType;

/**
 * Provides a list of identifier fields that a given {@link JavaType} may
 * require.
 * 
 * @author Ben Alex
 * @since 1.1
 */
public interface IdentifierService {

    /**
     * For the given type, returns zero or more identifier fields that the type
     * requires. An implementation may return null if they do not have knowledge
     * of any identifier fields for that type. An implementation returning a
     * non-null value indicates the implementation is authoritative for
     * determining identifier fields for the type. It is legal to return a
     * non-null list, which would denote an authoritative implementation but the
     * type simply has no identifier field requirement.
     * 
     * @param pkType the PK class type for which identifier information is
     *            desired (required)
     * @return null if the implementation is non-authoritative for the type,
     *         otherwise zero or more identifiers that the type should have
     */
    List<Identifier> getIdentifiers(JavaType pkType);
}
