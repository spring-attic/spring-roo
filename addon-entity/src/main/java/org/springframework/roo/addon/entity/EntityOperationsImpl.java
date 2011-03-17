package org.springframework.roo.addon.entity;

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
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
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
 * Implementation of {@link EntityOperations}.
 * 
 * @author Alan Stewart
 * @since 1.1.2
 */
@Component
@Service
public class EntityOperationsImpl implements EntityOperations {
	@Reference private FileManager fileManager;
	@Reference private MetadataService metadataService;
	@Reference private MemberDetailsScanner memberDetailsScanner;
	@Reference private ProjectOperations projectOperations;
	@Reference private TypeLocationService typeLocationService;
	@Reference private TypeManagementService typeManagementService;

	public boolean isPersistentClassAvailable() {
		return projectOperations.isProjectAvailable() && fileManager.exists(projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_RESOURCES, "META-INF/persistence.xml"));
	}

	public void newEntity(JavaType name, boolean createAbstract, JavaType superclass, List<AnnotationMetadataBuilder> annotations) {
		Assert.notNull(name, "Entity name required");
		
		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(name, Path.SRC_MAIN_JAVA);
		
		int modifier = Modifier.PUBLIC;
		if (createAbstract) {
			modifier |= Modifier.ABSTRACT;
		}
		
		ClassOrInterfaceTypeDetailsBuilder typeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, modifier, name, PhysicalTypeCategory.CLASS);

		if (!superclass.equals(new JavaType("java.lang.Object"))) {
			ClassOrInterfaceTypeDetails superclassClassOrInterfaceTypeDetails = typeLocationService.getClassOrInterface(superclass);
			if (superclassClassOrInterfaceTypeDetails != null) {
				typeDetailsBuilder.setSuperclass(new ClassOrInterfaceTypeDetailsBuilder(superclassClassOrInterfaceTypeDetails));
			}
		}
		
		List<JavaType> extendsTypes = new ArrayList<JavaType>();
		extendsTypes.add(superclass);
		typeDetailsBuilder.setExtendsTypes(extendsTypes);
		
		typeDetailsBuilder.setAnnotations(annotations);

		typeManagementService.generateClassFile(typeDetailsBuilder.build());
	}

	public void newEmbeddableClass(JavaType name, boolean serializable) {
		Assert.notNull(name, "Embeddable name required");
		
		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(name, Path.SRC_MAIN_JAVA);

		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(new AnnotationMetadataBuilder(new JavaType("org.springframework.roo.addon.javabean.RooJavaBean")));
		annotations.add(new AnnotationMetadataBuilder(new JavaType("org.springframework.roo.addon.tostring.RooToString")));
		annotations.add(new AnnotationMetadataBuilder(new JavaType("javax.persistence.Embeddable")));
		
		if (serializable) {
			annotations.add(new AnnotationMetadataBuilder(new JavaType("org.springframework.roo.addon.serializable.RooSerializable")));
		}

		int modifier = Modifier.PUBLIC;
		ClassOrInterfaceTypeDetailsBuilder typeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, modifier, name, PhysicalTypeCategory.CLASS);
		typeDetailsBuilder.setAnnotations(annotations);
		
		typeManagementService.generateClassFile(typeDetailsBuilder.build());
	}

	public void newIdentifier(JavaType identifierType, String identifierField, String identifierColumn) {
		Assert.notNull(identifierType, "Identifier type required");
		
		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(identifierType, Path.SRC_MAIN_JAVA);
		List<AnnotationMetadataBuilder> identifierAnnotations = new ArrayList<AnnotationMetadataBuilder>();
		identifierAnnotations.add(new AnnotationMetadataBuilder(new JavaType("org.springframework.roo.addon.tostring.RooToString")));
		identifierAnnotations.add(new AnnotationMetadataBuilder(new JavaType("org.springframework.roo.addon.entity.RooIdentifier")));
		
		ClassOrInterfaceTypeDetailsBuilder typeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, Modifier.PUBLIC | Modifier.FINAL, identifierType, PhysicalTypeCategory.CLASS);
		typeDetailsBuilder.setAnnotations(identifierAnnotations);
		
		typeManagementService.generateClassFile(typeDetailsBuilder.build());
	}

	public void newIntegrationTest(JavaType entity) {
		Assert.notNull(entity, "Entity to produce an integration test for is required");

		// Verify the requested entity actually exists as a class and is not abstract
		ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails = getEntity(entity);
		Assert.isTrue(!Modifier.isAbstract(classOrInterfaceTypeDetails.getModifier()), "Type " + entity.getFullyQualifiedTypeName() + " is abstract");
		
		newDod(entity, new JavaType(entity.getFullyQualifiedTypeName() + "DataOnDemand"), Path.SRC_TEST_JAVA);
		
		JavaType name = new JavaType(entity + "IntegrationTest");
		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(name, Path.SRC_TEST_JAVA);

		if (metadataService.get(declaredByMetadataId) != null) {
			// The file already exists
			return;
		}

		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		List<AnnotationAttributeValue<?>> config = new ArrayList<AnnotationAttributeValue<?>>();
		config.add(new ClassAttributeValue(new JavaSymbolName("entity"), entity));
		annotations.add(new AnnotationMetadataBuilder(new JavaType("org.springframework.roo.addon.test.RooIntegrationTest"), config));

		List<MethodMetadataBuilder> methods = new ArrayList<MethodMetadataBuilder>();
		List<AnnotationMetadataBuilder> methodAnnotations = new ArrayList<AnnotationMetadataBuilder>();
		methodAnnotations.add(new AnnotationMetadataBuilder(new JavaType("org.junit.Test")));
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(declaredByMetadataId, Modifier.PUBLIC, new JavaSymbolName("testMarkerMethod"), JavaType.VOID_PRIMITIVE, new InvocableMemberBodyBuilder());
		methodBuilder.setAnnotations(methodAnnotations);
		methods.add(methodBuilder);

		ClassOrInterfaceTypeDetailsBuilder typeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, Modifier.PUBLIC, name, PhysicalTypeCategory.CLASS);
		typeDetailsBuilder.setAnnotations(annotations);
		typeDetailsBuilder.setDeclaredMethods(methods);
		
		typeManagementService.generateClassFile(typeDetailsBuilder.build());
	}

	public void newDod(JavaType entity, JavaType name, Path path) {
		Assert.notNull(entity, "Entity to produce a data on demand provider for is required");
		Assert.notNull(name, "Name of the new data on demand provider is required");
		Assert.notNull(path, "Location of the new data on demand provider is required");

		// Verify the requested entity actually exists as a class and is not abstract
		ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails = getEntity(entity);
		Assert.isTrue(classOrInterfaceTypeDetails.getPhysicalTypeCategory() == PhysicalTypeCategory.CLASS, "Type " + entity.getFullyQualifiedTypeName() + " is not a class");

		// Check if the requested entity is a JPA @Entity
		MemberDetails memberDetails = memberDetailsScanner.getMemberDetails(EntityOperationsImpl.class.getName(), classOrInterfaceTypeDetails);
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
