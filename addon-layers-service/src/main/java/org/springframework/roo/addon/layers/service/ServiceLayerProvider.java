package org.springframework.roo.addon.layers.service;

import java.util.Arrays;
import java.util.LinkedHashMap;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.ArrayAttributeValue;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.layers.CoreLayerProvider;
import org.springframework.roo.project.layers.LayerType;
import org.springframework.roo.project.layers.MemberTypeAdditions;
import org.springframework.roo.support.util.StringUtils;
import org.springframework.uaa.client.util.Assert;

/**
 * The {@link org.springframework.roo.project.layers.LayerProvider} that
 * provides an application's service layer.
 * 
 * @author Stefan Schmidt
 * @since 1.2
 */
@Component
@Service
public class ServiceLayerProvider extends CoreLayerProvider {
	
	// Constants
	private static final JavaType ANNOTATION_TYPE = new JavaType(RooService.class.getName());
	private static final JavaType AUTOWIRED = new JavaType("org.springframework.beans.factory.annotation.Autowired");
	
	// Fields
	@Reference private TypeLocationService typeLocationService;
	@Reference private MetadataService metadataService;

	public MemberTypeAdditions getMemberTypeAdditions(String metadataId, String methodIdentifier, JavaType targetEntity, LinkedHashMap<JavaSymbolName, Object> methodParams) {
		Assert.isTrue(StringUtils.hasText(metadataId), "Metadata identifier required");
		Assert.notNull(methodIdentifier, "Method identifier required");
		Assert.notNull(targetEntity, "Target enitity type required");
		Assert.notNull(methodParams, "Method param names and types required (may be empty)");
		
		if (methodIdentifier.equals(PersistenceCustomDataKeys.FIND_ALL_METHOD.name())) {
			return getFindAllMethod(metadataId, targetEntity);
		}
		return null;
	}

	private MemberTypeAdditions getFindAllMethod(String metadataId, JavaType entityType) {
		ClassOrInterfaceTypeDetails coitd = findMemberDetails(entityType);
		if (coitd == null) {
			return null;
		}
		ClassOrInterfaceTypeDetailsBuilder classBuilder = new ClassOrInterfaceTypeDetailsBuilder(metadataId);
		AnnotationMetadataBuilder annotation = new AnnotationMetadataBuilder(AUTOWIRED);
		String fieldName = StringUtils.uncapitalize(coitd.getName().getSimpleTypeName());
		classBuilder.addField(new FieldMetadataBuilder(metadataId, 0, Arrays.asList(annotation), new JavaSymbolName(fieldName), coitd.getName()).build());
		PluralMetadata pluralMetadata = (PluralMetadata) metadataService.get(PluralMetadata.createIdentifier(entityType, Path.SRC_MAIN_JAVA));
		if (pluralMetadata == null) {
			return null;
		}
//		@SuppressWarnings("unchecked")
//		AnnotationAttributeValue<StringAttributeValue> findAllMethod = (AnnotationAttributeValue<StringAttributeValue>) MemberFindingUtils.getAnnotationOfType(coitd.getAnnotations(), ANNOTATION_TYPE).getAttribute(new JavaSymbolName(RooService.FIND_ALL_METHOD));
		return new MemberTypeAdditions(classBuilder, fieldName + "." + RooService.FIND_ALL_METHOD + pluralMetadata.getPlural() + "()");
	}
	
	private ClassOrInterfaceTypeDetails findMemberDetails(JavaType type) {
		for (ClassOrInterfaceTypeDetails coitd : typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(ANNOTATION_TYPE)) {
			AnnotationMetadata annotation = MemberFindingUtils.getAnnotationOfType(coitd.getAnnotations(), ANNOTATION_TYPE);
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
	
	public int getLayerPosition() {
		return LayerType.SERVICE.getPosition();
	}
}
