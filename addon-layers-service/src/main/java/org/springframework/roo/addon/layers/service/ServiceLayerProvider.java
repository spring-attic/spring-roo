package org.springframework.roo.addon.layers.service;

import java.util.Arrays;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.ArrayAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.layers.LayerAdapter;
import org.springframework.roo.layers.LayerType;
import org.springframework.roo.layers.MemberTypeAdditions;
import org.springframework.roo.layers.Priority;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.StringUtils;

/**
 * 
 * @author Stefan Schmidt
 * @since 1.2
 */
@Component
@Service
public class ServiceLayerProvider extends LayerAdapter {
	
	@Reference private TypeLocationService typeLocationService;
	private static final JavaType ANNOTATION_TYPE = new JavaType(RooService.class.getName());
	private static final JavaType AUTOWIRED = new JavaType("org.springframework.beans.factory.annotation.Autowired");

	public LayerType getLayerType() {
		return LayerType.SERVICE;
	}

	@Override
	public MemberTypeAdditions getFindAllMethod(String declaredByMetadataId, JavaSymbolName entityVariableName, JavaType entityType, LayerType layerType) {
		ClassOrInterfaceTypeDetails coitd = findMemberDetails(entityType);
		if (coitd == null) {
			return null;
		}
		
		ClassOrInterfaceTypeDetailsBuilder classBuilder = new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId);
		AnnotationMetadataBuilder annotation = new AnnotationMetadataBuilder(AUTOWIRED);
		JavaType interfaceType = coitd.getImplementsTypes().get(0);
		String repoField = StringUtils.uncapitalize(interfaceType.getSimpleTypeName());
		classBuilder.addField(new FieldMetadataBuilder(declaredByMetadataId, 0, Arrays.asList(annotation), new JavaSymbolName(repoField), interfaceType).build());
//		@SuppressWarnings("unchecked")
//		AnnotationAttributeValue<StringAttributeValue> findAllMethod = (AnnotationAttributeValue<StringAttributeValue>) MemberFindingUtils.getAnnotationOfType(coitd.getAnnotations(), ANNOTATION_TYPE).getAttribute(new JavaSymbolName("findAllMethod"));
		return new MemberTypeAdditions(classBuilder, repoField + ".findAll()");
	}
	
	private ClassOrInterfaceTypeDetails findMemberDetails(JavaType type) {
		for (ClassOrInterfaceTypeDetails coitd : typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(ANNOTATION_TYPE)) {
			AnnotationMetadata annotation = MemberFindingUtils.getAnnotationOfType(coitd.getAnnotations(), ANNOTATION_TYPE);
			if (annotation == null) {
				return null;
			}
			@SuppressWarnings("unchecked")
			ArrayAttributeValue<AnnotationAttributeValue<JavaType>> domainTypes = (ArrayAttributeValue<AnnotationAttributeValue<JavaType>>) annotation.getAttribute(new JavaSymbolName(RooService.DOMAIN_TYPES));
			if (domainTypes == null) {
				return null;
			}
			for (AnnotationAttributeValue<JavaType> javaTypeValue : domainTypes.getValue()) {
				if (javaTypeValue.getValue().equals(type)) {
					return coitd;
				}
			}
		}
		return null;
	}

	public boolean supports(AnnotationMetadata annotation) {
		return annotation.getAnnotationType().equals(ANNOTATION_TYPE);
	}
	
	public int priority() {
		return Priority.LOW.getNumericValue(); // Lowest priority because it's the default provider.
	}
}
