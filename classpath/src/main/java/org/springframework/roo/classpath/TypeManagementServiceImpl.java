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
import org.springframework.roo.project.LogicalPath;
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

	// Fields
	@Reference private FileManager fileManager;
	@Reference private MetadataService metadataService;
	@Reference private ProjectOperations projectOperations;
	@Reference private TypeLocationService typeLocationService;
	@Reference private TypeParsingService typeParsingService;

	@Deprecated
	public void generateClassFile(final ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails) {
		createOrUpdateTypeOnDisk(classOrInterfaceTypeDetails);
	}

	public void addEnumConstant(final String physicalTypeIdentifier, final JavaSymbolName constantName) {
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
		createOrUpdateTypeOnDisk(classOrInterfaceTypeDetailsBuilder.build());
	}

	public void addField(final FieldMetadata field) {
		Assert.notNull(field, "Field metadata not provided");

		// Obtain the physical type and ITD mutable details
		PhysicalTypeMetadata ptm = (PhysicalTypeMetadata) metadataService.get(field.getDeclaredByMetadataId());
		Assert.notNull(ptm, "Java source code unavailable for type " + PhysicalTypeIdentifier.getFriendlyName(field.getDeclaredByMetadataId()));
		PhysicalTypeDetails ptd = ptm.getMemberHoldingTypeDetails();
		Assert.notNull(ptd, "Java source code details unavailable for type " + PhysicalTypeIdentifier.getFriendlyName(field.getDeclaredByMetadataId()));
		ClassOrInterfaceTypeDetailsBuilder classOrInterfaceTypeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder((ClassOrInterfaceTypeDetails) ptd);

		// Automatically add JSR 303 (Bean Validation API) support if there is no current JSR 303 support but a JSR 303 annotation is present
		boolean jsr303Required = false;
		for (AnnotationMetadata annotation : field.getAnnotations()) {
			if (annotation.getAnnotationType().getFullyQualifiedTypeName().startsWith("javax.validation")) {
				jsr303Required = true;
				break;
			}
		}

		LogicalPath path = PhysicalTypeIdentifier.getPath(classOrInterfaceTypeDetailsBuilder.getDeclaredByMetadataId());

		if (jsr303Required) {
			// It's more likely the version below represents a later version than any specified in the user's own dependency list
			projectOperations.addDependency(path.getModule(), "javax.validation", "validation-api", "1.0.0.GA");
		}
		classOrInterfaceTypeDetailsBuilder.addField(field);
		createOrUpdateTypeOnDisk(classOrInterfaceTypeDetailsBuilder.build());
	}

	public void createOrUpdateTypeOnDisk(final ClassOrInterfaceTypeDetails cit) {
		final String fileCanonicalPath = typeLocationService.getPhysicalTypeCanonicalPath(cit.getDeclaredByMetadataId());
		final String newContents = typeParsingService.getCompilationUnitContents(cit);
		fileManager.createOrUpdateTextFileIfRequired(fileCanonicalPath, newContents, true);
	}
}
