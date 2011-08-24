package org.springframework.roo.classpath.customdata.taggers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.model.CustomDataKey;
import org.springframework.roo.model.JavaType;


public class AnnotatedTypeMatcher extends TypeMatcher {

	private List<JavaType> annotationTypesToMatchOn;
	private CustomDataKey<MemberHoldingTypeDetails> customDataKey;

	public AnnotatedTypeMatcher(CustomDataKey<MemberHoldingTypeDetails> customDataKey, JavaType... annotationTypeToMatchOn) {
		this.annotationTypesToMatchOn = Arrays.asList(annotationTypeToMatchOn);
		this.customDataKey = customDataKey;
	}

	public List<MemberHoldingTypeDetails> matches(List<MemberHoldingTypeDetails> memberHoldingTypeDetailsList) {
		Map<String, MemberHoldingTypeDetails> matched = new HashMap<String, MemberHoldingTypeDetails>();
		for (MemberHoldingTypeDetails memberHoldingTypeDetails : memberHoldingTypeDetailsList) {
			for (AnnotationMetadata annotationMetadata : memberHoldingTypeDetails.getAnnotations()) {
				for (JavaType annotationTypeToMatchOn : annotationTypesToMatchOn) {
					if (annotationMetadata.getAnnotationType().equals(annotationTypeToMatchOn)) {
						matched.put(memberHoldingTypeDetails.getDeclaredByMetadataId(), memberHoldingTypeDetails);
					}
				}
			}
		}
		return new ArrayList<MemberHoldingTypeDetails>(matched.values());
	}

	public CustomDataKey<MemberHoldingTypeDetails> getCustomDataKey() {
		return customDataKey;
	}

	public Object getTagValue(MemberHoldingTypeDetails key) {
		return null;
	}
}
