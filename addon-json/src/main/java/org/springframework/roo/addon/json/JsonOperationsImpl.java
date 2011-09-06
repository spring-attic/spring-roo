package org.springframework.roo.addon.json;

import static org.springframework.roo.model.RooJavaType.ROO_JAVA_BEAN;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.support.util.Assert;

/**
 * Implementation of addon-json operations interface.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
@Component 
@Service 
public class JsonOperationsImpl implements JsonOperations {

	// Fields
	@Reference private MetadataService metadataService;
	@Reference private TypeLocationService typeLocationService;
	@Reference private TypeManagementService typeManagementService;

	public boolean isCommandAvailable() {
		return metadataService.get(ProjectMetadata.getProjectIdentifier()) != null;
	}

	public void annotateType(JavaType javaType, String rootName, boolean deepSerialize) {
		Assert.notNull(javaType, "Java type required");

		ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails = typeLocationService.findClassOrInterface(javaType);
		if (classOrInterfaceTypeDetails == null) {
			throw new IllegalArgumentException("Cannot locate source for '" + javaType.getFullyQualifiedTypeName() + "'");
		}

		if (MemberFindingUtils.getAnnotationOfType(classOrInterfaceTypeDetails.getAnnotations(), RooJavaType.ROO_JSON) == null) {
			AnnotationMetadataBuilder annotationBuilder = new AnnotationMetadataBuilder(RooJavaType.ROO_JSON);
			if (rootName != null && rootName.length() > 0) {
				annotationBuilder.addStringAttribute("rootName", rootName);
			}
			if (deepSerialize) {
				annotationBuilder.addBooleanAttribute("deepSerialize", true);
			}
			ClassOrInterfaceTypeDetailsBuilder classOrInterfaceTypeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(classOrInterfaceTypeDetails);
			classOrInterfaceTypeDetailsBuilder.addAnnotation(annotationBuilder);
			typeManagementService.createOrUpdateTypeOnDisk(classOrInterfaceTypeDetailsBuilder.build());
		}
	}
	
	public void annotateType(JavaType javaType, String rootName) {
		annotateType(javaType, rootName, false);
	}
	
	public void annotateAll(final boolean deepSerialize) {
		for (final JavaType type : typeLocationService.findTypesWithAnnotation(ROO_JAVA_BEAN)) {
			annotateType(type, "", deepSerialize);
		}
	}
	
	public void annotateAll() {
		annotateAll(false);
	}
}