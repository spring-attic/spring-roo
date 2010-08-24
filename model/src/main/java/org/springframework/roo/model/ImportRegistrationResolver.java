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
	 * It may only be used in simple form if:
	 * 
	 * <ul>
	 * <li>it is of {@link DataType#VARIABLE}; or</li>
	 * <li>it is of {@link DataType#PRIMITIVE}; or</li>
	 * <li>it is already registered as an import; or</li>
	 * <li>it is in the same package as the compilation unit; or</li>
	 * <li>it is part of java.lang</li>
	 * </ul>
	 * 
	 * <p>
	 * Note that advanced implementations may be able to determine all types available in a particular package, 
	 * but this is not required.
	 * 
	 * @param javaType to lookup (required)
	 * @return true if a fully-qualified form must be used, or false if a simple form can be used 
	 */
	boolean isFullyQualifiedFormRequired(JavaType javaType);
	
	/**
	 * Automatically invokes {@link #isAdditionLegal(JavaType)}, then {@link #addImport(JavaType)}, and
	 * finally {@link #isFullyQualifiedFormRequired(JavaType)}, returning the result of the final method.
	 * This method is the main method that should be used by callers, as it will automatically attempt to
	 * cause a {@link JavaType} to be used in its simple form if at all possible.
	 * 
	 * @param javaType to automatically register (if possible) and lookup whether simplified used is available (required)
	 * @return true if a fully-qualified form must be used, or false if a simple form can be used 
	 */
	boolean isFullyQualifiedFormRequiredAfterAutoImport(JavaType javaType);
	
	/**
	 * Indicates whether the presented {@link JavaType} can be legally presented to {@link #addImport(JavaType)}.
	 * It is considered legal only if the presented {@link JavaType} is of type {@link DataType#TYPE},
	 * there is not an existing conflicting registered import, and the proposed type is not within the default
	 * package. Note it is legal to add types from the same package as the compilation unit, and indeed may be 
	 * required by implementations that are otherwise unaware of all the types available in a particular package.
	 * 
	 * @param javaType
	 * @return true is the presented type can be legally presented to {@link #addImport(JavaType)}, otherwise false.
	 */
	boolean isAdditionLegal(JavaType javaType);

	/**
	 * Explicitly registers an import. Note that no verification will be performed to ensure an import is legal or
	 * does not conflict with an existing import (use {@link #isAdditionLegal(JavaType)} for verification).
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
