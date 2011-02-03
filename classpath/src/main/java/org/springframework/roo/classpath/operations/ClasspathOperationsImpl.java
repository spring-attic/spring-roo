package org.springframework.roo.classpath.operations;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;

/**
 * Provides convenience methods that can be used to create source code and install the
 * JSR 303 validation API when required.
 * 
 * @author Ben Alex
 * @since 1.0
 */
@Component
@Service
@Deprecated
public class ClasspathOperationsImpl implements ClasspathOperations {
	@Reference private TypeLocationService typeLocationService;
	@Reference private TypeManagementService typeManagementService;
	
	public String getPhysicalLocationCanonicalPath(JavaType javaType, Path path) {
		return typeLocationService.getPhysicalLocationCanonicalPath(javaType, path);
	}
	
	public String getPhysicalLocationCanonicalPath(String physicalTypeIdentifier) {
		return typeLocationService.getPhysicalLocationCanonicalPath(physicalTypeIdentifier);
	}

	public void generateClassFile(ClassOrInterfaceTypeDetails details) {
		typeManagementService.generateClassFile(details);
	}
}
