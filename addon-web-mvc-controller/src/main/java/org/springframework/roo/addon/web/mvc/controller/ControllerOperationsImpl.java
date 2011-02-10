package org.springframework.roo.addon.web.mvc.controller;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.addon.web.mvc.controller.scaffold.WebScaffoldMetadata;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.BooleanAttributeValue;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.Assert;

/**
 * Provides Controller configuration operations.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 */
@Component 
@Service 
public class ControllerOperationsImpl implements ControllerOperations {
	@Reference private FileManager fileManager;
	@Reference private MetadataService metadataService;
	@Reference private ProjectOperations projectOperations;
	@Reference private WebMvcOperations webMvcOperations;
	@Reference private MetadataDependencyRegistry dependencyRegistry;
	@Reference private TypeLocationService typeLocationService;
	@Reference private TypeManagementService typeManagementService;

	public boolean isNewControllerAvailable() {
		return projectOperations.isProjectAvailable();
	}

	public void generateAll(final JavaPackage javaPackage) {
		Set<ClassOrInterfaceTypeDetails> cids = typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(new JavaType(RooEntity.class.getName()));
		for (ClassOrInterfaceTypeDetails cid : cids) {
			if (Modifier.isAbstract(cid.getModifier())) {
				continue;
			}
			
			JavaType javaType = cid.getName();
			Path path = PhysicalTypeIdentifier.getPath(cid.getDeclaredByMetadataId());
			EntityMetadata entityMetadata = (EntityMetadata) metadataService.get(EntityMetadata.createIdentifier(javaType, path));
			
			if (entityMetadata == null || (!entityMetadata.isValid())) {
				continue;
			}
			
			// Check to see if this entity metadata has a web scaffold metadata listening to it
			String downstreamWebScaffoldMetadataId = WebScaffoldMetadata.createIdentifier(javaType, path);
			if (dependencyRegistry.getDownstream(entityMetadata.getId()).contains(downstreamWebScaffoldMetadataId)) {
				// There is already a controller for this entity
				continue;
			}
			
			// To get here, there is no listening controller, so add one
			JavaType controller = new JavaType(javaPackage.getFullyQualifiedPackageName() + "." + javaType.getSimpleTypeName() + "Controller");
			createAutomaticController(controller, javaType, new HashSet<String>(), entityMetadata.getPlural().toLowerCase());
		}
		return;
	}

	public void createAutomaticController(JavaType controller, JavaType entity, Set<String> disallowedOperations, String path) {
		Assert.notNull(controller, "Controller Java Type required");
		Assert.notNull(entity, "Entity Java Type required");
		Assert.notNull(disallowedOperations, "Set of disallowed operations required");
		Assert.hasText(path, "Controller base path required");

		String resourceIdentifier = typeLocationService.getPhysicalLocationCanonicalPath(controller, Path.SRC_MAIN_JAVA);

		if (fileManager.exists(resourceIdentifier)) {
			return; //type exists already - nothing to do
		}
			
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

		// Create annotation @RooWebScaffold(path = "/test", formBackingObject = MyObject.class)
		List<AnnotationAttributeValue<?>> rooWebScaffoldAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		rooWebScaffoldAttributes.add(new StringAttributeValue(new JavaSymbolName("path"), path));
		rooWebScaffoldAttributes.add(new ClassAttributeValue(new JavaSymbolName("formBackingObject"), entity));
		for (String operation : disallowedOperations) {
			rooWebScaffoldAttributes.add(new BooleanAttributeValue(new JavaSymbolName(operation), false));
		}
		annotations.add(new AnnotationMetadataBuilder(new JavaType(RooWebScaffold.class.getName()), rooWebScaffoldAttributes));

		// Create annotation @RequestMapping("/myobject/**")
		List<AnnotationAttributeValue<?>> requestMappingAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		requestMappingAttributes.add(new StringAttributeValue(new JavaSymbolName("value"), "/" + path));
		annotations.add(new AnnotationMetadataBuilder(new JavaType("org.springframework.web.bind.annotation.RequestMapping"), requestMappingAttributes));

		// Create annotation @Controller
		List<AnnotationAttributeValue<?>> controllerAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		annotations.add(new AnnotationMetadataBuilder(new JavaType("org.springframework.stereotype.Controller"), controllerAttributes));

		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(controller, projectOperations.getPathResolver().getPath(resourceIdentifier));
		ClassOrInterfaceTypeDetailsBuilder typeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, Modifier.PUBLIC, controller, PhysicalTypeCategory.CLASS);
		typeDetailsBuilder.setAnnotations(annotations);

		typeManagementService.generateClassFile(typeDetailsBuilder.build());

		webMvcOperations.installAllWebMvcArtifacts();
	}
}
