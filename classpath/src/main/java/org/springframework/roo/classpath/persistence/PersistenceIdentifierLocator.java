package org.springframework.roo.classpath.persistence;

import java.util.List;

import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.model.JavaType;

/**
 * Provides a central service for add-ons to inquire about persistence identifier fields 
 * used by a specific domain type.
 * 
 * @author Stefan Schmidt
 * @since 1.2
 */
public interface PersistenceIdentifierLocator {
	
	/**
	 * Locates identifier types for a given domain type.
	 * 
	 * @param domainType The domain type (needs to be part of the project)
	 * @return a list of identifier fields (not null, may be empty)
	 */
	List<FieldMetadata> getIdentifierFields(JavaType domainType);
	
	/**
	 * Locates embedded identifier types for a given domain type.
	 * 
	 * @param domainType The domain type (needs to be part of the project)
	 * @return a list of identifier fields (not null, may be empty)
	 */
	List<FieldMetadata> getEmbeddedIdentifierFields(JavaType domainType);

}
