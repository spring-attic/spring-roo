package org.springframework.roo.addon.layers.repository;

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
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.layers.LayerAdapter;
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
public class RepositoryJpaLayerProvider extends LayerAdapter {
	
	@Reference private TypeLocationService typeLocationService;
	private static final JavaType ANNOTATION_TYPE = new JavaType(RooRepositoryJpa.class.getName());
	private static final JavaType AUTOWIRED = new JavaType("org.springframework.beans.factory.annotation.Autowired");
	
	@Override
	public MemberTypeAdditions getFindAllMethod(String declaredByMetadataId, JavaSymbolName entityVariableName, JavaType entityType, int layerPosition) {
		ClassOrInterfaceTypeDetails coitd = findMemberDetails(entityType);
		if (coitd == null) {
			return null;
		}
		
		ClassOrInterfaceTypeDetailsBuilder classBuilder = new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId);
		AnnotationMetadataBuilder annotation = new AnnotationMetadataBuilder(AUTOWIRED);
		String repoField = StringUtils.uncapitalize(coitd.getName().getSimpleTypeName());
		classBuilder.addField(new FieldMetadataBuilder(declaredByMetadataId, 0, Arrays.asList(annotation), new JavaSymbolName(repoField), coitd.getName()).build());
		
		return new MemberTypeAdditions(classBuilder, repoField + ".findAll()"); // TODO: retrieve method name from annotation values
	}

	private ClassOrInterfaceTypeDetails findMemberDetails(JavaType type) {
		for (ClassOrInterfaceTypeDetails coitd : typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(ANNOTATION_TYPE)) {
			AnnotationMetadata annotation = MemberFindingUtils.getAnnotationOfType(coitd.getAnnotations(), ANNOTATION_TYPE);
			if (annotation == null) {
				return null;
			}
			@SuppressWarnings("unchecked")
			AnnotationAttributeValue<JavaType> domainType = (AnnotationAttributeValue<JavaType>) annotation.getAttribute(new JavaSymbolName(RooRepositoryJpa.DOMAIN_TYPE));
			if (domainType == null) {
				return null;
			}
			if (domainType.getValue().equals(type)) {
				return coitd;
			}
		}
		return null;
	}

	public int getLayerPosition() {
		return LayerType.REPOSITORY.getPosition();
	}

	public boolean supports(AnnotationMetadata annotation) {
		return annotation.getAnnotationType().equals(ANNOTATION_TYPE);
	}
	
	public int priority() {
		return Priority.LOW.getNumericValue(); // Lowest priority because it's the default provider.
	}
}
