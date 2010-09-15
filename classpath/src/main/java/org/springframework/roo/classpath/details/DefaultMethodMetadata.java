package org.springframework.roo.classpath.details;

import java.lang.reflect.Modifier;
import java.util.List;

import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.model.CustomData;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * Default implementation of {@link MethodMetadata}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public final class DefaultMethodMetadata extends AbstractInvocableMemberMetadata implements MethodMetadata {
	private JavaSymbolName methodName;
	private JavaType returnType;

	// Package protected to mandate the use of MethodMetadataBuilder
	DefaultMethodMetadata(CustomData customData, String declaredByMetadataId, int modifier, List<AnnotationMetadata> annotations, JavaSymbolName methodName, JavaType returnType, List<AnnotatedJavaType> parameterTypes, List<JavaSymbolName> parameterNames, List<JavaType> throwsTypes, String body) {
		super(customData, declaredByMetadataId, modifier, annotations, parameterTypes, parameterNames, throwsTypes, body);
		Assert.notNull(methodName, "Method name required");
		Assert.notNull(returnType, "Return type required");
		this.methodName = methodName;
		this.returnType = returnType;
	}

	public JavaSymbolName getMethodName() {
		return methodName;
	}

	public final JavaType getReturnType() {
		return returnType;
	}

	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("declaredByMetadataId", getDeclaredByMetadataId());
		tsc.append("modifier", Modifier.toString(getModifier()));
		tsc.append("methodName", methodName);
		tsc.append("parameterTypes", getParameterTypes());
		tsc.append("parameterNames", getParameterNames());
		tsc.append("returnType", returnType);
		tsc.append("annotations", getAnnotations());
		tsc.append("throwsTypes", getThrowsTypes());
		tsc.append("customData", getCustomData());
		tsc.append("body", getBody());
		return tsc.toString();
	}
}
