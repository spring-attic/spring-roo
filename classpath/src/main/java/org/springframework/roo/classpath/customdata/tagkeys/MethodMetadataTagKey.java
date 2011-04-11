package org.springframework.roo.classpath.customdata.tagkeys;

import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

import java.util.List;

/**
 * {@link MethodMetadata} specific  implementation of {@link InvocableMemberMetadataTagKey}.
 * TODO: Create MethodMetadataTagKeyBuilder
 *
 * @author James Tyrrell
 * @since 1.1.3
 */
public class MethodMetadataTagKey extends InvocableMemberMetadataTagKey<MethodMetadata>{

	private JavaType returnType;
	private JavaSymbolName methodName;
	private String tag;

	public MethodMetadataTagKey(Integer modifier, List<AnnotationMetadata> annotations, List<AnnotatedJavaType> parameterTypes, List<JavaSymbolName> parameterNames, List<JavaType> throwsTypes, JavaType returnType, JavaSymbolName methodName) {
		super(modifier, annotations, parameterTypes, parameterNames, throwsTypes);
		this.returnType = returnType;
		this.methodName = methodName;
	}

	public MethodMetadataTagKey(String tag) {
		super();
		this.tag = tag;
	}

	@Override
	public void validate(MethodMetadata taggedInstance) throws IllegalStateException {
		super.validate(taggedInstance);
		//TODO: Add in validation logic for returnType, methodName
	}

	public JavaType getReturnType() {
		return returnType;
	}

	public JavaSymbolName getMethodName() {
		return methodName;
	}

	@Override
	public String toString() {
		return tag;
	}
}
