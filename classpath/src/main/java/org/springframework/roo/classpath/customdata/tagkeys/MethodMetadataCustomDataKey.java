package org.springframework.roo.classpath.customdata.tagkeys;

import java.util.List;

import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * {@link MethodMetadata}-specific  implementation of {@link InvocableMemberMetadataCustomDataKey}.
 *
 * @author James Tyrrell
 * @since 1.1.3
 */
public class MethodMetadataCustomDataKey extends InvocableMemberMetadataCustomDataKey<MethodMetadata> {
	private JavaType returnType;
	private JavaSymbolName methodName;
	private String name;

	public MethodMetadataCustomDataKey(final Integer modifier, final List<AnnotationMetadata> annotations, final List<AnnotatedJavaType> parameterTypes, final List<JavaSymbolName> parameterNames, final List<JavaType> throwsTypes, final JavaType returnType, final JavaSymbolName methodName) {
		super(modifier, annotations, parameterTypes, parameterNames, throwsTypes);
		this.returnType = returnType;
		this.methodName = methodName;
	}

	public MethodMetadataCustomDataKey(final String tag) {
		super();
		this.name = tag;
	}

	@Override
	public boolean meets(final MethodMetadata methodMetadata) throws IllegalStateException {
		// TODO: Add in validation logic for returnType, methodName
		return super.meets(methodMetadata);
	}

	public JavaType getReturnType() {
		return returnType;
	}

	public JavaSymbolName getMethodName() {
		return methodName;
	}

	@Override
	public String toString() {
		return name;
	}

	public String name() {
		return name;
	}
}
