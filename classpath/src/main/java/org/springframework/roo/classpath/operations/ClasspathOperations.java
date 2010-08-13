package org.springframework.roo.classpath.operations;

import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;

/**
 * Provides a common set of operations for Java types.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public interface ClasspathOperations {

	boolean isProjectAvailable();

	boolean isPersistentClassAvailable();

	String getPhysicalLocationCanonicalPath(JavaType javaType, Path path);

	String getPhysicalLocationCanonicalPath(String physicalTypeIdentifier);

	/**
	 * Obtains the superclass, if one is specified.
	 * 
	 * <p>
	 * Throws an exception is a superclass was requested but could not be parsed or found.
	 * 
	 * @param superclass requested superclass (can be null)
	 * @return null if a superclass is not requested or not required (ie java.lang.Object)
	 */
	ClassOrInterfaceTypeDetails getSuperclass(JavaType superclass);

	/**
	 * Obtains the requested {@link JavaType}, assuming it is a class or interface that exists at this time and can be parsed.
	 * If these assumption are not met, an exception will be thrown.
	 * 
	 * @param requiredClassOrInterface that should be parsed (required)
	 * @return the details (never returns null)
	 */
	ClassOrInterfaceTypeDetails getClassOrInterface(JavaType requiredClassOrInterface);

	/**
	 * Creates a new class, with the location name name provided in the details.
	 * 
	 * <p>
	 * An exception is thrown if the class already exists.
	 * 
	 * @param details to create (required)
	 */
	void generateClassFile(ClassOrInterfaceTypeDetails details);

	/**
	 * Adds a new enum constant to an existing class.
	 * 
	 * @param physicalTypeIdentifier to add (required)
	 * @param the name of the constant (required)
	 */
	void addEnumConstant(String physicalTypeIdentifier, JavaSymbolName constantName);

	/**
	 * Adds a new field to an existing class.
	 * 
	 * <p>
	 * An exception is thrown if the class does not exist, cannot be modified or a field with the requested name is already declared.
	 *  
	 * @param fieldMetadata to add (required)
	 */
	void addField(FieldMetadata fieldMetadata);

	/**
	 * Creates an integration test for the entity. Automatically produces a data on demand if one does not exist.
	 * Silently returns if the integration test file already exists.
	 * 
	 * @param entity to produce an integration test for (required)
	 */
	void newIntegrationTest(JavaType entity);

	/**
	 * Creates a new data on demand provider for the entity. Silently returns if the DOD already exists.
	 * 
	 * @param entity to produce a data on demand provider for (required)
	 * @param name the name of the new data on demand class (required)
	 * @param path the location for the new data on demand class (required)
	 */
	void newDod(JavaType entity, JavaType name, Path path);
}