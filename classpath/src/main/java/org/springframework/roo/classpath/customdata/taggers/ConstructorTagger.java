package org.springframework.roo.classpath.customdata.taggers;

import org.springframework.roo.classpath.customdata.tagkeys.TagKey;
import org.springframework.roo.classpath.details.ConstructorMetadata;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.model.JavaType;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link ConstructorMetadata} specific implementation of {@link Tagger}. Currently
 * ConstructorMetadata instances are only matched based on parameter types.
 *
 * @author James Tyrrell
 * @since 1.1.3
 */
public class ConstructorTagger implements Tagger<ConstructorMetadata>{

	private TagKey<ConstructorMetadata> tagKey;
	private List<JavaType> parameterTypes = new ArrayList<JavaType>();

	public ConstructorTagger(TagKey<ConstructorMetadata> tagKey, List<JavaType> parameterTypes) {
		this.tagKey = tagKey;
		this.parameterTypes = parameterTypes;
	}

	public List<ConstructorMetadata> matches(List<MemberHoldingTypeDetails> memberHoldingTypeDetailsList) {
		List<ConstructorMetadata> constructors = new ArrayList<ConstructorMetadata>();
		for (MemberHoldingTypeDetails memberHoldingTypeDetails : memberHoldingTypeDetailsList) {
			for (ConstructorMetadata constructor : memberHoldingTypeDetails.getDeclaredConstructors()) {
				if (parameterTypes.equals(AnnotatedJavaType.convertFromAnnotatedJavaTypes(constructor.getParameterTypes()))) {
					constructors.add(constructor);
				}
			}
		}
		return constructors;
	}

	public TagKey<ConstructorMetadata> getTagKey() {
		return tagKey;
	}

	public Object getTagValue(ConstructorMetadata key) {
		return null;
	}
}
