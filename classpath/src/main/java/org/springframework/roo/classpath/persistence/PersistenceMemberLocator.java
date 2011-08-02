package org.springframework.roo.classpath.persistence;

import java.util.List;

import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.JavaType;

/**
 * Provides metadata about persistence-related members within domain types.
 * 
 * @author Stefan Schmidt
 * @since 1.2
 */
public interface PersistenceMemberLocator {
	
	/**
	 * Returns the ID accessor for the given domain type
	 * 
	 * @param domainType the domain type (can be <code>null</code>)
	 * @return <code>null</code> if the given type is <code>null</code> or does
	 * not have an ID accessor
	 */
	MethodMetadata getIdentifierAccessor(JavaType domainType);
	
	/**
	 * Returns the ID accessor for the given domain type. Less expensive than
	 * calling the other {@link #getIdentifierAccessor} methods if you already
	 * have the {@link MemberDetails}.
	 * 
	 * @param domainType the domain type (can be <code>null</code>)
	 * @return <code>null</code> if the given type is <code>null</code> or does
	 * not have an ID accessor
	 */
	MethodMetadata getIdentifierAccessor(MemberDetails domainType);
	
	/**
	 * Returns the ID accessor for the given domain type. Less expensive than
	 * calling {@link #getIdentifierAccessor(JavaType)} if you already have the
	 * physical details.
	 * 
	 * @param domainType the physical domain type (can be <code>null</code>)
	 * @return <code>null</code> if the given type is <code>null</code> or does
	 * not have an ID accessor
	 */
	MethodMetadata getIdentifierAccessor(PhysicalTypeMetadata domainType);
	
	/**
	 * Locates identifier types for a given domain type.
	 * 
	 * @param domainType The domain type (needs to be part of the project)
	 * @return a list of identifier fields (not null, may be empty)
	 */
	List<FieldMetadata> getIdentifierFields(JavaType domainType);
	
	/**
	 * Locates identifier types for a given domain type. Less expensive
	 * than calling the other {@link #getIdentifierFields} methods if you already
	 * have the {@link MemberDetails}.
	 * 
	 * @param memberDetails The domain type details (needs to be part of the project)
	 * @return a list of identifier fields (not null, may be empty)
	 */
	List<FieldMetadata> getIdentifierFields(MemberDetails memberDetails);
	
	/**
	 * Locates embedded identifier types for a given domain type.
	 * 
	 * @param domainType The domain type (needs to be part of the project)
	 * @return a list of identifier fields (not null, may be empty)
	 */
	List<FieldMetadata> getEmbeddedIdentifierFields(JavaType domainType);
	
	/**
	 * Locates embedded identifier types for a given domain type. Less expensive
	 * than calling the other {@link #getEmbeddedIdentifierFields} methods if you already
	 * have the {@link MemberDetails}.
	 * 
	 * @param memberDetails The domain type details (needs to be part of the project)
	 * @return a list of identifier fields (not null, may be empty)
	 */
	List<FieldMetadata> getEmbeddedIdentifierFields(MemberDetails memberDetails);

	/**
	 * Returns the version accessor for the given domain type. Less expensive
	 * than calling the other {@link #getVersionAccessor} methods if you already
	 * have the {@link MemberDetails}.
	 * 
	 * @param domainType the domain type (can be <code>null</code>)
	 * @return <code>null</code> if the given type is <code>null</code> or does
	 * not have a version accessor
	 */
	MethodMetadata getVersionAccessor(MemberDetails domainType);
	
	/**
	 * Returns the version accessor for the given domain type. 
	 * 
	 * @param domainType the domain type (can be <code>null</code>)
	 * @return <code>null</code> if the given type is <code>null</code> or does
	 * not have a version accessor
	 */
	MethodMetadata getVersionAccessor(JavaType domainType);
	
	/**
	 * Returns the version field for the given domain type, if it has one.
	 * 
	 * @param domainType the domain type (can be <code>null</code>)
	 * @return <code>null</code> if the given domain type is null or has no
	 * version field
	 * @throws IllegalStateException if there is more than one version field
	 */
	FieldMetadata getVersionField(MemberDetails domainType);
	
	/**
	 * Returns the version field(s) for the given domain type
	 * 
	 * @param domainType the domain type (can be <code>null</code>)
	 * @return a non-<code>null</code> list; empty if the given domain type is
	 * null or has no version fields
	 */
	List<FieldMetadata> getVersionFields(MemberDetails domainType);
}
