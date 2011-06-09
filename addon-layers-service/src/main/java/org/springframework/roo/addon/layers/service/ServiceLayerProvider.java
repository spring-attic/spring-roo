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
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.layers.CoreLayerProvider;
import org.springframework.roo.project.layers.LayerType;
import org.springframework.roo.project.layers.MemberTypeAdditions;
import org.springframework.roo.project.layers.Priority;
import org.springframework.roo.support.util.StringUtils;

/**
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

	@Override
	public MemberTypeAdditions getFindAllMethod(String declaredByMetadataId, JavaSymbolName entityVariableName, JavaType entityType, int layerPosition) {
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
		return new MemberTypeAdditions(classBuilder, repoField + ".findAll()"); // TODO get method name from @RooService annotation
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
	
	public int getLayerPosition() {
		return LayerType.SERVICE.getPosition();
	}
	
	//TODO should this concern be moved to a more core type?
	public int priority() {
		return Priority.LOW.getNumericValue(); // Lowest priority because it's the default provider.
	}
}
