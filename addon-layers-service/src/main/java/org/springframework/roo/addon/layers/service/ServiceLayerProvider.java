package org.springframework.roo.addon.layers.service;

import java.util.Arrays;
import java.util.LinkedHashMap;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
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
import org.springframework.roo.metadata.MetadataDependencyRegistry;
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
	private static final Path SRC = Path.SRC_MAIN_JAVA;
	
	// Fields
	@Reference private TypeLocationService typeLocationService;
	@Reference private MetadataService metadataService;
	@Reference private MetadataDependencyRegistry metadataDependencyRegistry;

	public MemberTypeAdditions getMemberTypeAdditions(String metadataId, String methodIdentifier, JavaType targetEntity, LinkedHashMap<JavaSymbolName, Object> methodParams) {
		Assert.isTrue(StringUtils.hasText(metadataId), "Metadata identifier required");
		Assert.notNull(methodIdentifier, "Method identifier required");
		Assert.notNull(targetEntity, "Target enitity type required");
		Assert.notNull(methodParams, "Method param names and types required (may be empty)");
		
		ClassOrInterfaceTypeDetails coitd = findMemberDetails(targetEntity);
		if (coitd == null) {
			return null;
		}
		metadataDependencyRegistry.registerDependency(coitd.getDeclaredByMetadataId(), metadataId);
		ServiceAnnotationValues annotationValues = getServiceAnnotationValues(coitd.getName());
		if (annotationValues == null) {
			return null;
		}
		String plural = getPlural(targetEntity);
		if (plural == null) {
			return null;
		}
		metadataDependencyRegistry.registerDependency(PluralMetadata.createIdentifier(targetEntity, SRC), metadataId);
		if (methodIdentifier.equals(PersistenceCustomDataKeys.FIND_ALL_METHOD.name())) {
			return getFindAllMethod(metadataId, coitd, annotationValues, plural);
		}
		return null;
	}

	private MemberTypeAdditions getFindAllMethod(String metadataId, ClassOrInterfaceTypeDetails coitd, ServiceAnnotationValues serviceAnnotationValues, String plural) {
		ClassOrInterfaceTypeDetailsBuilder classBuilder = new ClassOrInterfaceTypeDetailsBuilder(metadataId);
		AnnotationMetadataBuilder annotation = new AnnotationMetadataBuilder(AUTOWIRED);
		String fieldName = StringUtils.uncapitalize(coitd.getName().getSimpleTypeName());
		classBuilder.addField(new FieldMetadataBuilder(metadataId, 0, Arrays.asList(annotation), new JavaSymbolName(fieldName), coitd.getName()).build());
		return new MemberTypeAdditions(classBuilder, fieldName + "." + serviceAnnotationValues.getFindAllMethod() + plural + "()");
	}
	
	private String getPlural(JavaType entity) {
		String pluralId = PluralMetadata.createIdentifier(entity, SRC);
		PluralMetadata pluralMetadata = (PluralMetadata) metadataService.get(pluralId);
		if (pluralMetadata == null) {
			return null;
		}
		return pluralMetadata.getPlural();
	}
	
	private ServiceAnnotationValues getServiceAnnotationValues(JavaType type) {
		PhysicalTypeMetadata physicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(type, SRC));
		if (physicalTypeMetadata == null) {
			return null;
		}
		return new ServiceAnnotationValues(physicalTypeMetadata);
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
