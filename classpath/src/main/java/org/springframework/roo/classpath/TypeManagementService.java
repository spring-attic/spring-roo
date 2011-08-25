package org.springframework.roo.classpath;

import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.model.JavaSymbolName;

/**
 * Creates and maintains types.
 * 
 * @author Alan Stewart
 * @since 1.1.2
 */
public interface TypeManagementService {
	
	/**
	 * Creates a new class, with the location name name provided in the details.
	 * 
	 * <p>
	 * An exception is thrown if the class already exists.
	 * 
	 * @param classOrInterfaceTypeDetails the {@link ClassOrInterfaceTypeDetails} to create (required)
	 */
	void generateClassFile(ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails);
	
	/**
	 * Adds a new enum constant to an existing class.
	 * 
	 * @param physicalTypeIdentifier to add (required)
	 * @param constantName the name of the constant (required)
	 */
	void addEnumConstant(String physicalTypeIdentifier, JavaSymbolName constantName);

	/**
	 * Adds a new field to an existing class.
	 * 
	 * <p>
	 * An exception is thrown if the class does not exist, cannot be modified or a field with the requested name is already declared.
	 *  
	 * @param fieldMetadata the field to add (required)
	 */
	void addField(FieldMetadata fieldMetadata);

	/**
	 * Creates a physical type with the contents based on the {@link ClassOrInterfaceTypeDetails} passed in at the
	 * location denoted by the passed in path.  This method expects the passed in file location to be correct.
	 *
	 * @param classOrInterfaceTypeDetails {@link ClassOrInterfaceTypeDetails} to base the file contents on (required)
	 * @param fileIdentifier the path to the type (required)
	 */
	void createOrUpdateTypeOnDisk(final ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails, String fileIdentifier);

	/**
	 * Creates the physical type on the disk, with the structure shown.
	 *
	 * <p>
	 * An implementation is not required to support all of the constructs in the presented {@link PhysicalTypeMetadata}.
	 * An implementation must throw an exception if it cannot create the presented type.
	 *
	 * <p>
	 * An implementation may merge the contents with an existing file, if the type already exists.
	 *
	 * @param toCreate to create (required)
	 */
	void createPhysicalType(PhysicalTypeMetadata toCreate);
}
