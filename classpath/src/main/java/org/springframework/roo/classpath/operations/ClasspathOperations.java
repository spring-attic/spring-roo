package org.springframework.roo.classpath.operations;

import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;

/**
 * Provides a common set of operations for Java types.
 * 
 * @author Ben Alex
 * @since 1.0
 */
@Deprecated
public interface ClasspathOperations {

	/** Use {@link TypeLocationService#getPhysicalLocationCanonicalPath(JavaType, Path)} instead. */
	@Deprecated
	String getPhysicalLocationCanonicalPath(JavaType javaType, Path path);

	/** Use {@link TypeLocationService#getPhysicalLocationCanonicalPath(String)} instead. */
	@Deprecated
	String getPhysicalLocationCanonicalPath(String physicalTypeIdentifier);

	/**
	 * Creates a new class, with the location name name provided in the details.
	 * 
	 * <p>
	 * This method is deprecated. Use {@link TypeManagementService#generateClassFile(ClassOrInterfaceTypeDetails)} instead.
	 * 
	 * <p>
	 * An exception is thrown if the class already exists.
	 * 
	 * @param details to create (required)
	 */
	@Deprecated
	void generateClassFile(ClassOrInterfaceTypeDetails details);
}