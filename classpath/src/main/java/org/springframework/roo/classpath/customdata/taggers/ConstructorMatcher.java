package org.springframework.roo.classpath.customdata.taggers;

import org.springframework.roo.model.CustomDataKey;
import org.springframework.roo.classpath.details.ConstructorMetadata;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.model.JavaType;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link ConstructorMetadata} specific implementation of {@link Matcher}. Currently
 * ConstructorMetadata instances are only matched based on parameter types.
 *
 * @author James Tyrrell
 * @since 1.1.3
 */
public class ConstructorMatcher implements Matcher<ConstructorMetadata> {

	private CustomDataKey<ConstructorMetadata> customDataKey;
	private List<JavaType> parameterTypes = new ArrayList<JavaType>();

	public ConstructorMatcher(CustomDataKey<ConstructorMetadata> customDataKey, List<JavaType> parameterTypes) {
		this.customDataKey = customDataKey;
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

	public CustomDataKey<ConstructorMetadata> getCustomDataKey() {
		return customDataKey;
	}

	public Object getTagValue(ConstructorMetadata key) {
		return null;
	}
}
