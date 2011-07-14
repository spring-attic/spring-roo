package org.springframework.roo.project.layers;

import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.classpath.customdata.taggers.TypeMatcher;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.ArrayAttributeValue;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.model.CustomDataKey;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Layer type specific matcher which populates a tag value based on the layer provider specific trigger annotation.
 * 
 * @author Stefan Schmidt
 * @since 1.2
 */
public class LayerTypeMatcher extends TypeMatcher {
	
	private JavaType annotationType;
	private JavaSymbolName annotationAttributeValue;
	
	public LayerTypeMatcher(CustomDataKey<MemberHoldingTypeDetails> customDataKey, String declaredBy, JavaType annotationType, JavaSymbolName annotationAttributeName) {
		super(customDataKey, declaredBy);
		this.annotationType = annotationType;
		this.annotationAttributeValue = annotationAttributeName;
	}
	
	@Override
	public Object getTagValue(MemberHoldingTypeDetails key) {
		AnnotationMetadata annotation = MemberFindingUtils.getAnnotationOfType(((ItdTypeDetails) key).getGovernor().getAnnotations(), annotationType);
		if (annotation == null || annotation.getAttribute(annotationAttributeValue) == null) {
			return null;
		}
		AnnotationAttributeValue<?> value = annotation.getAttribute(annotationAttributeValue);
		List<JavaType> domainTypes = new ArrayList<JavaType>();
		if (value instanceof ClassAttributeValue) {
			domainTypes.add(((ClassAttributeValue) value).getValue());
		} else if (value instanceof ArrayAttributeValue<?>) {
			ArrayAttributeValue<?> castValue = (ArrayAttributeValue<?>) value;
			for (AnnotationAttributeValue<?> val : castValue.getValue()) {
				if (val instanceof ClassAttributeValue) {
					domainTypes.add(((ClassAttributeValue) val).getValue());
				}
			}
		}
		return domainTypes;
	}

}
