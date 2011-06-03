package org.springframework.roo.addon.layers.dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
import org.springframework.roo.classpath.details.annotations.ArrayAttributeValue;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.layers.LayerAdapter;
import org.springframework.roo.project.layers.LayerType;
import org.springframework.roo.project.layers.MemberTypeAdditions;
import org.springframework.roo.project.layers.Priority;

/**
 * 
 * @author Stefan Schmidt
 * @since 1.2
 */
@Component
@Service
public class DaoJpaLayerProvider extends LayerAdapter {
	
	@Reference private TypeLocationService typeLocationService;
	@Reference private MemberDetailsScanner memberDetailsScanner;
	private static final JavaType ANNOTATION_TYPE = new JavaType(RooDaoJpa.class.getName());
	
	@Override
	public MemberTypeAdditions getDeleteMethod(String id, JavaSymbolName entityVariableName, JavaType entityType, LayerType layerType) {
		JavaSymbolName methodName = new JavaSymbolName("remove");
		MemberDetails memberDetails = findMemberDetails(entityType);
		if (memberDetails == null || MemberFindingUtils.getMethod(memberDetails, methodName, Arrays.asList(JavaType.LONG_OBJECT)) == null) {
			return null;
		}
		JavaSymbolName injectedFieldName = new JavaSymbolName(entityVariableName + "Dao");
		ClassOrInterfaceTypeDetailsBuilder metadataBuilder = new ClassOrInterfaceTypeDetailsBuilder(id);
		metadataBuilder.addField(new FieldMetadataBuilder(id, 0, injectedFieldName, new JavaType(entityType.getFullyQualifiedTypeName() + "Dao"), null).build());
		return new MemberTypeAdditions(metadataBuilder, injectedFieldName.getSymbolName() + "." + methodName.getSymbolName() + "(" + entityVariableName.getSymbolName() + ");");
	}
	
	private MemberDetails findMemberDetails(JavaType type) {
		for (ClassOrInterfaceTypeDetails coitd : typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(ANNOTATION_TYPE)) {
			AnnotationMetadata annotation = MemberFindingUtils.getAnnotationOfType(coitd.getAnnotations(), ANNOTATION_TYPE);
			if (annotation == null) {
				return null;
			}
			@SuppressWarnings("unchecked")
			ArrayAttributeValue<AnnotationAttributeValue<JavaType>> domainTypes = (ArrayAttributeValue<AnnotationAttributeValue<JavaType>>) annotation.getAttribute(new JavaSymbolName(RooDaoJpa.DOMAIN_TYPES));
			if (domainTypes == null) {
				return null;
			}
			List<ClassOrInterfaceTypeDetails> foundTypes = new ArrayList<ClassOrInterfaceTypeDetails>();
			List<AnnotationAttributeValue<JavaType>> values = domainTypes.getValue();
			for (AnnotationAttributeValue<JavaType> javaTypeValue : values) {
				if (javaTypeValue.getValue().equals(type)) { // TODO: possibly move this check to JavaType
					foundTypes.add(coitd);
				}
			}
			if (foundTypes.isEmpty()) {
				return null;
			} else if (foundTypes.size() == 1) {
				return memberDetailsScanner.getMemberDetails(DaoJpaLayerProvider.class.getName(), foundTypes.get(0));
			}
			throw new IllegalStateException("Detected " + foundTypes.size() + " project types annotated with @" + RooDaoJpa.class.getSimpleName() + " that defines " + type.getSimpleTypeName() + " as domain type.");
		}
		return null;
	}

	public LayerType getLayerType() {
		return LayerType.DAO;
	}

	public boolean supports(AnnotationMetadata annotation) {
		return annotation.getAnnotationType().equals(ANNOTATION_TYPE);
	}
	
	public int priority() {
		return Priority.LOW.getNumericValue(); // Lowest priority because it's the default provider.
	}
}
