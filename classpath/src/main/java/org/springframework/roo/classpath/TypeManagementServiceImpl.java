package org.springframework.roo.classpath;

import java.io.File;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.DefaultPhysicalTypeMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MutableClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.Assert;

/**
 * Implementation of {@link TypeManagementService}.
 * 
 * @author Alan Stewart
 * @since 1.1.2
 */
@Component
@Service 
public class TypeManagementServiceImpl implements TypeManagementService {
	@Reference private FileManager fileManager;
	@Reference private MetadataService metadataService;
	@Reference private MutablePhysicalTypeMetadataProvider physicalTypeMetadataProvider;
	@Reference private ProjectOperations projectOperations;
	
	public void generateClassFile(ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails) {
		Assert.isTrue(projectOperations.isProjectAvailable(), "Class file cannot be generated at this time");
		Assert.notNull(classOrInterfaceTypeDetails, "Details required");
		
		// Determine the canonical filename
		String physicalLocationCanonicalPath = getPhysicalLocationCanonicalPath(classOrInterfaceTypeDetails.getDeclaredByMetadataId());
	
		// Check the file doesn't already exist
		Assert.isTrue(!fileManager.exists(physicalLocationCanonicalPath), projectOperations.getPathResolver().getFriendlyName(physicalLocationCanonicalPath) + " already exists");
		
		// Compute physical location
		PhysicalTypeMetadata toCreate = new DefaultPhysicalTypeMetadata(classOrInterfaceTypeDetails.getDeclaredByMetadataId(), physicalLocationCanonicalPath, classOrInterfaceTypeDetails);
		physicalTypeMetadataProvider.createPhysicalType(toCreate);
	}
	
	public void addEnumConstant(String physicalTypeIdentifier, JavaSymbolName constantName) {
		Assert.isTrue(projectOperations.isProjectAvailable(), "Cannot add a constant at this time");
		Assert.hasText(physicalTypeIdentifier, "Type identifier not provided");
		Assert.notNull(constantName, "Constant name required");
		
		// Obtain the physical type and itd mutable details
		PhysicalTypeMetadata ptm = (PhysicalTypeMetadata) metadataService.get(physicalTypeIdentifier);
		Assert.notNull(ptm, "Java source code unavailable for type " + PhysicalTypeIdentifier.getFriendlyName(physicalTypeIdentifier));
		PhysicalTypeDetails ptd = ptm.getMemberHoldingTypeDetails();
		Assert.notNull(ptd, "Java source code details unavailable for type " + PhysicalTypeIdentifier.getFriendlyName(physicalTypeIdentifier));
		Assert.isInstanceOf(MutableClassOrInterfaceTypeDetails.class, ptd, "Java source code is immutable for type " + PhysicalTypeIdentifier.getFriendlyName(physicalTypeIdentifier));
		MutableClassOrInterfaceTypeDetails mutableTypeDetails = (MutableClassOrInterfaceTypeDetails) ptd;

		// Ensure it's an enum
		Assert.isTrue(mutableTypeDetails.getPhysicalTypeCategory() == PhysicalTypeCategory.ENUMERATION,  PhysicalTypeIdentifier.getFriendlyName(physicalTypeIdentifier) + " is not an enum");
		
		mutableTypeDetails.addEnumConstant(constantName);
	}
	
	public void addField(FieldMetadata fieldMetadata) {
		Assert.isTrue(projectOperations.isProjectAvailable(), "Field cannot be added at this time");
		Assert.notNull(fieldMetadata, "Field metadata not provided");
		
		// Obtain the physical type and ITD mutable details
		PhysicalTypeMetadata ptm = (PhysicalTypeMetadata) metadataService.get(fieldMetadata.getDeclaredByMetadataId());
		Assert.notNull(ptm, "Java source code unavailable for type " + PhysicalTypeIdentifier.getFriendlyName(fieldMetadata.getDeclaredByMetadataId()));
		PhysicalTypeDetails ptd = ptm.getMemberHoldingTypeDetails();
		Assert.notNull(ptd, "Java source code details unavailable for type " + PhysicalTypeIdentifier.getFriendlyName(fieldMetadata.getDeclaredByMetadataId()));
		Assert.isInstanceOf(MutableClassOrInterfaceTypeDetails.class, ptd, "Java source code is immutable for type " + PhysicalTypeIdentifier.getFriendlyName(fieldMetadata.getDeclaredByMetadataId()));
		MutableClassOrInterfaceTypeDetails mutableTypeDetails = (MutableClassOrInterfaceTypeDetails) ptd;
		
		// Automatically add JSR 303 (Bean Validation API) support if there is no current JSR 303 support but a JSR 303 annotation is present
		boolean jsr303Required = false;
		for (AnnotationMetadata annotation : fieldMetadata.getAnnotations()) {
			if (annotation.getAnnotationType().getFullyQualifiedTypeName().startsWith("javax.validation")) {
				jsr303Required = true;
				break;
			}
		}
		
		if (jsr303Required) {
			// It's more likely the version below represents a later version than any specified in the user's own dependency list
			projectOperations.addDependency(new Dependency("javax.validation", "validation-api", "1.0.0.GA"));
		}
		
		mutableTypeDetails.addField(fieldMetadata);
	}
	
	private String getPhysicalLocationCanonicalPath(String physicalTypeIdentifier) {
		Assert.isTrue(PhysicalTypeIdentifier.isValid(physicalTypeIdentifier), "Physical type identifier is invalid");
		JavaType javaType = PhysicalTypeIdentifier.getJavaType(physicalTypeIdentifier);
		Path path = PhysicalTypeIdentifier.getPath(physicalTypeIdentifier);
		String relativePath = javaType.getFullyQualifiedTypeName().replace('.', File.separatorChar) + ".java";
		return projectOperations.getPathResolver().getIdentifier(path, relativePath);
	}
}
