package org.springframework.roo.model;

import java.util.Set;

/**
 * Represents the known imports for a particular compilation unit, and resolves whether a particular type
 * name can be expressed as a simple type name or requires a fully-qualified type name.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public interface ImportRegistrationResolver {
	
	/**
	 * @return the package this compilation unit belongs to (never null)
	 */
	JavaPackage getCompilationUnitPackage();
	
	/**
	 * Determines whether the presented {@link JavaType} must be used in a fully-qualified form or not.
	 * It does not need to be used in a fully-qualified form if it is already registered as an import or
	 * part of java.lang. Note that advanced implementations may be able to determine all types available
	 * in a particular pacakge, but this is not required.
	 * 
	 * @param javaType to lookup (required)
	 * @return true if the presented type can be used in a simple form, or false if a fully qualified form is needed
	 */
	boolean isFullyQualifiedFormRequired(JavaType javaType);
	
	/**
	 * Explicitly registers an import. Note that no verification will be performed to ensure an import is legal or
	 * does not conflict with an existing import. 
	 * 
	 * @param javaType to register (required)
	 */
	void addImport(JavaType javaType);
	
	/**
	 * Provides access to the registered imports.
	 * 
	 * @return an unmodifiable representation of all registered imports (never null, but may be empty)
	 */
	Set<JavaType> getRegisteredImports();

}
