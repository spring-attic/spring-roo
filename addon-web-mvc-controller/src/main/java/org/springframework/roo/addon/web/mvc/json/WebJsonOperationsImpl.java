package org.springframework.roo.addon.web.mvc.json;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.web.mvc.controller.scaffold.WebScaffoldAnnotationValues;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeDetails;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.MutableClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.support.util.Assert;

/**
 * Implements {@link WebJsonOperations}.
 * 
 * @author Stefan Schmidt
 * @since 1.2.0
 */
@Component(immediate = true)
@Service
public class WebJsonOperationsImpl implements WebJsonOperations {
	
	@Reference private FileManager fileManager;
	@Reference private PathResolver pathResolver;
	@Reference private MetadataService metadataService;
	@Reference private TypeLocationService typeLocationService;
	@Reference private TypeManagementService typeManagementService;

	public boolean isSetupAvailable() {
		String mvcConfig = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/spring/webmvc-config.xml");
		return !fileManager.exists(mvcConfig);
	}

	public boolean isCommandAvailable() {
		return !isSetupAvailable();
	}

	public void annotateType(JavaType type, JavaType jsonEntity) {
		Assert.notNull(type, "Target type required");
		Assert.notNull(jsonEntity, "Json entity requiured");
		
		String id = typeLocationService.findIdentifier(type);
		if (id == null) {
			createNewType(type, jsonEntity);
		} else {
			appendToExistingType(type, jsonEntity);
		}
	}

	public void annotateAll(JavaPackage javaPackage) {
		if (javaPackage == null) {
			ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
			javaPackage = projectMetadata.getTopLevelPackage();
		}
		for (ClassOrInterfaceTypeDetails cod : typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_JSON)) {
			if (Modifier.isAbstract(cod.getModifier())) {
				continue;
			}
			JavaType jsonType = cod.getName();
			JavaType mvcType = null;
			for (ClassOrInterfaceTypeDetails mvcCod : typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(RooJavaType.ROO_WEB_SCAFFOLD)) {
				// We know this physical type exists given type location service just found it.
				PhysicalTypeMetadata mvcMd = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(mvcCod.getName()));
				WebScaffoldAnnotationValues webScaffoldAnnotationValues = new WebScaffoldAnnotationValues(mvcMd);
				if (webScaffoldAnnotationValues.isAnnotationFound() && webScaffoldAnnotationValues.getFormBackingObject().equals(jsonType)) {
					mvcType = mvcCod.getName();
					break;
				}
			}
			if (mvcType == null) {
				createNewType(new JavaType(javaPackage.getFullyQualifiedPackageName() + "." + jsonType.getSimpleTypeName() + "Controller"), jsonType);
			} else {
				appendToExistingType(mvcType, jsonType);
			}
		}
	}
	
	private void appendToExistingType(JavaType type, JavaType jsonEntity) {
		String id = typeLocationService.findIdentifier(type);
		if (id == null) {
			throw new IllegalArgumentException("Cannot locate source for '" + type.getFullyQualifiedTypeName() + "'");
		}

		// Obtain the physical type and itd mutable details
		PhysicalTypeMetadata ptm = (PhysicalTypeMetadata) metadataService.get(id);
		Assert.notNull(ptm, "Java source code unavailable for type " + PhysicalTypeIdentifier.getFriendlyName(id));
		PhysicalTypeDetails ptd = ptm.getMemberHoldingTypeDetails();
		Assert.notNull(ptd, "Java source code details unavailable for type " + PhysicalTypeIdentifier.getFriendlyName(id));
		Assert.isInstanceOf(MutableClassOrInterfaceTypeDetails.class, ptd, "Java source code is immutable for type " + PhysicalTypeIdentifier.getFriendlyName(id));
		MutableClassOrInterfaceTypeDetails mutableTypeDetails = (MutableClassOrInterfaceTypeDetails) ptd;

		for (AnnotationMetadata annotation : mutableTypeDetails.getAnnotations()) {
			if (annotation.getAnnotationType().equals(RooJavaType.ROO_WEB_JSON)) {
				return;
			}
		}
		
		mutableTypeDetails.addTypeAnnotation(getAnnotation(jsonEntity).build());
	}
	
	private void createNewType(JavaType type, JavaType jsonEntity) {
		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(type, Path.SRC_MAIN_JAVA);
		ClassOrInterfaceTypeDetailsBuilder classOrInterfaceTypeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, Modifier.PUBLIC, type, PhysicalTypeCategory.CLASS);
		classOrInterfaceTypeDetailsBuilder.addAnnotation(getAnnotation(jsonEntity));
		typeManagementService.generateClassFile(classOrInterfaceTypeDetailsBuilder.build());
	}

	private AnnotationMetadataBuilder getAnnotation(JavaType type) {
		// Create annotation @RooWebJson(jsonObject = MyObject.class)
		List<AnnotationAttributeValue<?>> rooJsonAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		rooJsonAttributes.add(new ClassAttributeValue(new JavaSymbolName("jsonObject"), type));
		AnnotationMetadataBuilder annotation = new AnnotationMetadataBuilder(RooJavaType.ROO_WEB_JSON, rooJsonAttributes);
		return annotation;
	}
}
