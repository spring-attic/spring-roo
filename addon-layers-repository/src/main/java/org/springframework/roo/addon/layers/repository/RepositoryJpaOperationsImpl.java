package org.springframework.roo.addon.layers.repository;

import java.lang.reflect.Modifier;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.uaa.client.util.Assert;

/**
 * 
 * @author Stefan Schmidt
 * @since 1.2
 */
@Component
@Service
public class RepositoryJpaOperationsImpl implements RepositoryJpaOperations {
	
	@Reference private FileManager fileManager;
	@Reference private ProjectOperations projectOperations;
	@Reference private TypeLocationService typeLocationService;
	@Reference private TypeManagementService typeManagementService;

	public boolean isRepositoryCommandAvailable() {
		return projectOperations.isProjectAvailable() && fileManager.exists(projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_RESOURCES, "META-INF/persistence.xml"));
	}

	public void setupRepository(JavaType interfaceType, JavaType classType, JavaType domainType) {
		Assert.notNull(interfaceType, "Interface type required");
		Assert.notNull(classType, "Class type required");
		Assert.notNull(domainType, "Domain type required");
		
		String interfaceIdentifier = typeLocationService.getPhysicalLocationCanonicalPath(interfaceType, Path.SRC_MAIN_JAVA);
		String classIdentifier = typeLocationService.getPhysicalLocationCanonicalPath(classType, Path.SRC_MAIN_JAVA);
		
		if (fileManager.exists(interfaceIdentifier) || fileManager.exists(classIdentifier)) {
			return; //type exists already - nothing to do
		}
		
		// First build interface type
		AnnotationMetadataBuilder interfaceAnnotationMetadata = new AnnotationMetadataBuilder(new JavaType(RooRepositoryJpa.class.getName()));
		interfaceAnnotationMetadata.addAttribute(new ClassAttributeValue(new JavaSymbolName("domainType"), domainType));
		String interfaceMdId = PhysicalTypeIdentifier.createIdentifier(interfaceType, projectOperations.getPathResolver().getPath(interfaceIdentifier));
		ClassOrInterfaceTypeDetailsBuilder interfaceTypeBuilder = new ClassOrInterfaceTypeDetailsBuilder(interfaceMdId, Modifier.PUBLIC, interfaceType, PhysicalTypeCategory.INTERFACE);
		interfaceTypeBuilder.addAnnotation(interfaceAnnotationMetadata.build());
		typeManagementService.generateClassFile(interfaceTypeBuilder.build());
		
		// Second build the implementing class
		String classMdId = PhysicalTypeIdentifier.createIdentifier(classType, projectOperations.getPathResolver().getPath(classIdentifier));
		ClassOrInterfaceTypeDetailsBuilder classTypeBuilder = new ClassOrInterfaceTypeDetailsBuilder(classMdId, Modifier.PUBLIC, classType, PhysicalTypeCategory.CLASS);
		classTypeBuilder.addImplementsType(interfaceType);
		typeManagementService.generateClassFile(classTypeBuilder.build());
		
		// Third, take care of project configs
		configureProject();
	}

	private void configureProject() {
		// TODO
	}

}
