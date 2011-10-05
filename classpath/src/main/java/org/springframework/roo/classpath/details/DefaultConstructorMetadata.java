package org.springframework.roo.classpath.details;

import java.lang.reflect.Modifier;
import java.util.List;

import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.model.CustomData;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.style.ToStringCreator;

/**
 * Default implementation of {@link ConstructorMetadata}.
 *
 * @author Ben Alex
 * @since 1.0
 */
public class DefaultConstructorMetadata extends AbstractInvocableMemberMetadata implements ConstructorMetadata {

	// Package protected to mandate the use of ConstructorMetadataBuilder
	DefaultConstructorMetadata(final CustomData customData, final String declaredByMetadataId, final int modifier, final List<AnnotationMetadata> annotations, final List<AnnotatedJavaType> parameterTypes, final List<JavaSymbolName> parameterNames, final List<JavaType> throwsTypes, final String body) {
		super(customData, declaredByMetadataId, modifier, annotations, parameterTypes, parameterNames, throwsTypes, body);
	}

	@Override
	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("declaredByMetadataId", getDeclaredByMetadataId());
		tsc.append("modifier", Modifier.toString(getModifier()));
		tsc.append("parameterTypes", getParameterTypes());
		tsc.append("parameterNames", getParameterNames());
		tsc.append("annotations", getAnnotations());
		tsc.append("customData", getCustomData());
		tsc.append("body", getBody());
		return tsc.toString();
	}
}
