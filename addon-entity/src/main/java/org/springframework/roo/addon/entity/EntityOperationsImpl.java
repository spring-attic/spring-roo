package org.springframework.roo.addon.entity;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
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
}
