package org.springframework.roo.addon.dod;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeDetails;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.Assert;

/**
 * Implementation of {@link DataOnDemandOperations}.
 * 
 * @author Alan Stewart
 * @since 1.1.3
 */
@Component
@Service
public class DataOnDemandOperationsImpl implements DataOnDemandOperations {
	@Reference private FileManager fileManager;
	@Reference private MetadataService metadataService;
	@Reference private MemberDetailsScanner memberDetailsScanner;
	@Reference private ProjectOperations projectOperations;
	@Reference private TypeManagementService typeManagementService;

	public boolean isPersistentClassAvailable() {
		return projectOperations.isProjectAvailable() && fileManager.exists(projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_RESOURCES, "META-INF/persistence.xml"));
	}

	public void newDod(JavaType entity, JavaType name, Path path) {
		Assert.notNull(entity, "Entity to produce a data on demand provider for is required");
		Assert.notNull(name, "Name of the new data on demand provider is required");
		Assert.notNull(path, "Location of the new data on demand provider is required");

		// Verify the requested entity actually exists as a class and is not abstract
		ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails = getEntity(entity);
		Assert.isTrue(classOrInterfaceTypeDetails.getPhysicalTypeCategory() == PhysicalTypeCategory.CLASS, "Type " + entity.getFullyQualifiedTypeName() + " is not a class");
		Assert.isTrue(!Modifier.isAbstract(classOrInterfaceTypeDetails.getModifier()), "Type " + entity.getFullyQualifiedTypeName() + " is abstract");

		// Check if the requested entity is a JPA @Entity
		MemberDetails memberDetails = memberDetailsScanner.getMemberDetails(DataOnDemandOperationsImpl.class.getName(), classOrInterfaceTypeDetails);
		AnnotationMetadata entityAnnotation = MemberFindingUtils.getDeclaredTypeAnnotation(memberDetails, new JavaType("javax.persistence.Entity"));
		Assert.notNull(entityAnnotation, "Type " + entity.getFullyQualifiedTypeName() + " must be an @Entity");

		// Everything is OK to proceed
		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(name, path);

		if (metadataService.get(declaredByMetadataId) != null) {
			// The file already exists
			return;
		}
		
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		List<AnnotationAttributeValue<?>> dodConfig = new ArrayList<AnnotationAttributeValue<?>>();
		dodConfig.add(new ClassAttributeValue(new JavaSymbolName("entity"), entity));
		annotations.add(new AnnotationMetadataBuilder(new JavaType("org.springframework.roo.addon.dod.RooDataOnDemand"), dodConfig));

		ClassOrInterfaceTypeDetailsBuilder typeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, Modifier.PUBLIC, name, PhysicalTypeCategory.CLASS);
		typeDetailsBuilder.setAnnotations(annotations);
		
		typeManagementService.generateClassFile(typeDetailsBuilder.build());
	}
		
	/**
	 * @param entity the entity to lookup required 
	 * @return the type details (never null; throws an exception if it cannot be obtained or parsed)
	 */
	private ClassOrInterfaceTypeDetails getEntity(JavaType entity) {
		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(entity, Path.SRC_MAIN_JAVA);
		PhysicalTypeMetadata ptm = (PhysicalTypeMetadata) metadataService.get(physicalTypeIdentifier);
		Assert.notNull(ptm, "Java source code unavailable for type " + PhysicalTypeIdentifier.getFriendlyName(physicalTypeIdentifier));
		PhysicalTypeDetails ptd = ptm.getMemberHoldingTypeDetails();
		Assert.notNull(ptd, "Java source code details unavailable for type " + PhysicalTypeIdentifier.getFriendlyName(physicalTypeIdentifier));
		Assert.isInstanceOf(ClassOrInterfaceTypeDetails.class, ptd, "Java source code is immutable for type " + PhysicalTypeIdentifier.getFriendlyName(physicalTypeIdentifier));
		return (ClassOrInterfaceTypeDetails) ptd;
	}
}
