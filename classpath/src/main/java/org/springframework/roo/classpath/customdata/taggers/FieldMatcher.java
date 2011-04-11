package org.springframework.roo.classpath.customdata.taggers;

import org.springframework.roo.model.CustomDataKey;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.model.JavaSymbolName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link FieldMetadata} specific implementation of {@link Matcher}. Matches
 * are based on whether the field is annotated with at least one of the annotations
 * specified.
 *
 * @author James Tyrrell
 * @since 1.1.3
 */
public class FieldMatcher implements Matcher<FieldMetadata> {
	private List<AnnotationMetadata> annotations;
		private CustomDataKey<FieldMetadata> customDataKey;

		public FieldMatcher(CustomDataKey<FieldMetadata> customDataKey, List<AnnotationMetadata> annotations) {
			this.annotations = annotations;
			this.customDataKey = customDataKey;
		}

		public List<FieldMetadata> matches(List<MemberHoldingTypeDetails> memberHoldingTypeDetailsList) {
			List<FieldMetadata> fields = new ArrayList<FieldMetadata>();
			for (MemberHoldingTypeDetails memberHoldingTypeDetails : memberHoldingTypeDetailsList) {
				for (FieldMetadata field : memberHoldingTypeDetails.getDeclaredFields()) {
					if (getMatchingAnnotation(field) != null) {
						fields.add(field);
					}
				}
			}
			return fields;
		}

		public Object getTagValue(FieldMetadata field) {
			return getAttributeMap(field);
		}

		private Map<String, Object> getAttributeMap(FieldMetadata field) {
			Map<String, Object> map = new HashMap<String, Object>();
			AnnotationMetadata annotationMetadata = getMatchingAnnotation(field);
			if (annotationMetadata != null) {
				for (JavaSymbolName attributeName : annotationMetadata.getAttributeNames()) {
					map.put(attributeName.getSymbolName(), annotationMetadata.getAttribute(attributeName).getValue());
				}
			}
			return map;
		}

		private AnnotationMetadata getMatchingAnnotation(FieldMetadata field) {
			for (AnnotationMetadata fieldAnnotation : field.getAnnotations()) {
				for (AnnotationMetadata matchingAnnotation : annotations){
					if (fieldAnnotation.getAnnotationType().equals(matchingAnnotation.getAnnotationType())) {
						return fieldAnnotation;
					}
				}
			}
			return null;
		}

		public CustomDataKey<FieldMetadata> getCustomDataKey() {
			return customDataKey;
		}
}
