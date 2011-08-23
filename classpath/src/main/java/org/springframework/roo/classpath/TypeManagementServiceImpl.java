package org.springframework.roo.classpath;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Dependency;
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
	@Reference private ProjectOperations projectOperations;
	@Reference private TypeLocationService typeLocationService;
	@Reference private TypeParsingService typeParsingService;
	
	public void generateClassFile(ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails) {
		Assert.isTrue(projectOperations.isProjectAvailable(), "Class file cannot be generated at this time");
		Assert.notNull(classOrInterfaceTypeDetails, "Details required");
		
		// Determine the canonical filename
		String physicalLocationCanonicalPath = typeLocationService.getPhysicalTypeCanonicalPath(classOrInterfaceTypeDetails.getDeclaredByMetadataId());
	
		// Check the file doesn't already exist
		Assert.isTrue(!fileManager.exists(physicalLocationCanonicalPath), projectOperations.getPathResolver().getFriendlyName(physicalLocationCanonicalPath) + " already exists");
		
		createOrUpdateTypeOnDisk(classOrInterfaceTypeDetails, physicalLocationCanonicalPath);
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
		ClassOrInterfaceTypeDetailsBuilder classOrInterfaceTypeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder((ClassOrInterfaceTypeDetails) ptd);

		// Ensure it's an enum
		Assert.isTrue(classOrInterfaceTypeDetailsBuilder.getPhysicalTypeCategory() == PhysicalTypeCategory.ENUMERATION,  PhysicalTypeIdentifier.getFriendlyName(physicalTypeIdentifier) + " is not an enum");

		classOrInterfaceTypeDetailsBuilder.addEnumConstant(constantName);
		String fileIdentifier = typeLocationService.getPhysicalTypeCanonicalPath(classOrInterfaceTypeDetailsBuilder.getDeclaredByMetadataId());
		createOrUpdateTypeOnDisk(classOrInterfaceTypeDetailsBuilder.build(), fileIdentifier);
	}
	
	public void addField(FieldMetadata fieldMetadata) {
		Assert.isTrue(projectOperations.isProjectAvailable(), "Field cannot be added at this time");
		Assert.notNull(fieldMetadata, "Field metadata not provided");
		
		// Obtain the physical type and ITD mutable details
		PhysicalTypeMetadata ptm = (PhysicalTypeMetadata) metadataService.get(fieldMetadata.getDeclaredByMetadataId());
		Assert.notNull(ptm, "Java source code unavailable for type " + PhysicalTypeIdentifier.getFriendlyName(fieldMetadata.getDeclaredByMetadataId()));
		PhysicalTypeDetails ptd = ptm.getMemberHoldingTypeDetails();
		Assert.notNull(ptd, "Java source code details unavailable for type " + PhysicalTypeIdentifier.getFriendlyName(fieldMetadata.getDeclaredByMetadataId()));
		ClassOrInterfaceTypeDetailsBuilder classOrInterfaceTypeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder((ClassOrInterfaceTypeDetails) ptd);
		
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
		classOrInterfaceTypeDetailsBuilder.addField(fieldMetadata);
		String fileIdentifier = typeLocationService.getPhysicalTypeCanonicalPath(classOrInterfaceTypeDetailsBuilder.getDeclaredByMetadataId());
		createOrUpdateTypeOnDisk(classOrInterfaceTypeDetailsBuilder.build(), fileIdentifier);
	}

	public void createOrUpdateTypeOnDisk(final ClassOrInterfaceTypeDetails cit, String fileIdentifier) {
		Assert.notNull(fileManager, "File manager required");
		Assert.notNull(cit, "Class or interface type details required");
		Assert.hasText(fileIdentifier, "File identifier required");

		final String newContents = typeParsingService.getCompilationUnitContents(cit);
		fileManager.createOrUpdateTextFileIfRequired(fileIdentifier, newContents, true);
	}

	public void createPhysicalType(PhysicalTypeMetadata toCreate) {
		Assert.notNull(toCreate, "Metadata to create is required");
		PhysicalTypeDetails physicalTypeDetails = toCreate.getMemberHoldingTypeDetails();
		Assert.notNull(physicalTypeDetails, "Unable to parse '" + toCreate + "'");
		Assert.isInstanceOf(ClassOrInterfaceTypeDetails.class, physicalTypeDetails, "This implementation can only create class or interface types");
		ClassOrInterfaceTypeDetails cit = (ClassOrInterfaceTypeDetails) physicalTypeDetails;
		String fileIdentifier = toCreate.getPhysicalLocationCanonicalPath();
		createOrUpdateTypeOnDisk(cit, fileIdentifier);
	}
}
