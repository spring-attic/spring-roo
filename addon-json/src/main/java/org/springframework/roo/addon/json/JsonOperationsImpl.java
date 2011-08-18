package org.springframework.roo.addon.json;

import static org.springframework.roo.model.RooJavaType.ROO_JAVA_BEAN;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.PhysicalTypeDetails;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.PhysicalTypeMetadataProvider;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MutableClassOrInterfaceTypeDetails;
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
	
	@Reference private MetadataService metadataService;
	@Reference private PhysicalTypeMetadataProvider physicalTypeMetadataProvider;
	@Reference private TypeLocationService typeLocationService;

	public boolean isCommandAvailable() {
		return metadataService.get(ProjectMetadata.getProjectIdentifier()) != null;
	}

	public void annotateType(JavaType javaType, String rootName, boolean deepSerialize) {
		Assert.notNull(javaType, "Java type required");

		String id = physicalTypeMetadataProvider.findIdentifier(javaType);
		if (id == null) {
			throw new IllegalArgumentException("Cannot locate source for '" + javaType.getFullyQualifiedTypeName() + "'");
		}

		// Obtain the physical type and itd mutable details
		PhysicalTypeMetadata ptm = (PhysicalTypeMetadata) metadataService.get(id);
		Assert.notNull(ptm, "Java source code unavailable for type " + PhysicalTypeIdentifier.getFriendlyName(id));
		PhysicalTypeDetails ptd = ptm.getMemberHoldingTypeDetails();
		Assert.notNull(ptd, "Java source code details unavailable for type " + PhysicalTypeIdentifier.getFriendlyName(id));
		Assert.isInstanceOf(MutableClassOrInterfaceTypeDetails.class, ptd, "Java source code is immutable for type " + PhysicalTypeIdentifier.getFriendlyName(id));
		MutableClassOrInterfaceTypeDetails mutableTypeDetails = (MutableClassOrInterfaceTypeDetails) ptd;

		// TODO do these two "if" statements check the same thing?
		if (MemberFindingUtils.getAnnotationOfType(mutableTypeDetails.getAnnotations(), RooJavaType.ROO_JSON) == null) {
			if (!mutableTypeDetails.getAnnotations().contains(RooJavaType.ROO_JSON)) {
				AnnotationMetadataBuilder annotationBuilder = new AnnotationMetadataBuilder(RooJavaType.ROO_JSON);
				if (rootName != null && rootName.length() > 0) {
					annotationBuilder.addStringAttribute("rootName", rootName);
				}
				if (deepSerialize) {
					annotationBuilder.addBooleanAttribute("deepSerialize", true);
				}
				mutableTypeDetails.addTypeAnnotation(annotationBuilder.build());
			}
		}
	}
	
	public void annotateType(JavaType javaType, String rootName) {
		annotateType(javaType, rootName, false);
	}
	
	public void annotateAll(final boolean deepSerialize) {
		for (final JavaType type: typeLocationService.findTypesWithAnnotation(ROO_JAVA_BEAN)) {
			annotateType(type, "", deepSerialize);
		}
	}
	
	public void annotateAll() {
		annotateAll(false);
	}
}