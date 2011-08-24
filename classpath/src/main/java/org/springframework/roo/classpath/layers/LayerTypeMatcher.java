package org.springframework.roo.classpath.layers;

import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.classpath.customdata.taggers.AnnotatedTypeMatcher;
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
 * @since 1.2.0
 */
public class LayerTypeMatcher extends AnnotatedTypeMatcher {
	
	// Fields
	private final JavaSymbolName annotationAttributeValue;
	private final JavaType annotationType;
	
	/**
	 * Constructor
	 *
	 * @param customDataKey
	 * @param annotationType
	 * @param annotationAttributeName
	 * @param annotationsToMatchOn
	 */
	public LayerTypeMatcher(CustomDataKey<MemberHoldingTypeDetails> customDataKey, JavaType annotationType, JavaSymbolName annotationAttributeName, JavaType... annotationsToMatchOn) {
		super(customDataKey, annotationsToMatchOn);
		this.annotationAttributeValue = annotationAttributeName;
		this.annotationType = annotationType;
	}
	
	@Override
	public Object getTagValue(MemberHoldingTypeDetails key) {
		AnnotationMetadata annotation = MemberFindingUtils.getAnnotationOfType(key.getAnnotations(), annotationType);
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
