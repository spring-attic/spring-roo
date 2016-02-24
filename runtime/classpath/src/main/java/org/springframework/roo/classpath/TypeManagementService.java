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
     * Adds a new enum constant to an existing class.
     * 
     * @param physicalTypeIdentifier to add (required)
     * @param constantName the name of the constant (required)
     */
    void addEnumConstant(String physicalTypeIdentifier,
            JavaSymbolName constantName);

    /**
     * Adds a new field to an existing class.
     * <p>
     * An exception is thrown if the class does not exist, cannot be modified or
     * a field with the requested name is already declared.
     * 
     * @param field the field to add (required)
     */
    void addField(FieldMetadata field);

    /**
     * Creates a physical type with the contents based on the
     * {@link ClassOrInterfaceTypeDetails} passed in at the location denoted by
     * the passed in path. This method expects the passed in file location to be
     * correct.
     * 
     * @param cid {@link ClassOrInterfaceTypeDetails} to base the file contents
     *            on (required)
     */
    void createOrUpdateTypeOnDisk(ClassOrInterfaceTypeDetails cid);

    /**
     * Creates a new class, with the location name name provided in the details.
     * <p>
     * An exception is thrown if the class already exists.
     * 
     * @param cid the {@link ClassOrInterfaceTypeDetails} to create (required)
     */
    @Deprecated
    void generateClassFile(ClassOrInterfaceTypeDetails cid);
}
