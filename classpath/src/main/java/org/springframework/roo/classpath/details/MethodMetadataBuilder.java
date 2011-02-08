package org.springframework.roo.classpath.details;

import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Builder for {@link MethodMetadata}.
 * 
 * @author Ben Alex
 * @since 1.1
 */
public final class MethodMetadataBuilder extends AbstractInvocableMemberMetadataBuilder<MethodMetadata> {
	private JavaSymbolName methodName;
	private JavaType returnType;
	
	public MethodMetadataBuilder(String declaredbyMetadataId) {
		super(declaredbyMetadataId);
	}
	
	public MethodMetadataBuilder(MethodMetadata existing) {
		super(existing);
		init(existing.getMethodName(), existing.getReturnType());
	}
	
	public MethodMetadataBuilder(String declaredbyMetadataId, MethodMetadata existing) {
		super(declaredbyMetadataId, existing);
		init(existing.getMethodName(), existing.getReturnType());
	}

	public MethodMetadataBuilder(String declaredbyMetadataId, int modifier, JavaSymbolName methodName, JavaType returnType, List<AnnotatedJavaType> parameterTypes, List<JavaSymbolName> parameterNames, InvocableMemberBodyBuilder bodyBuilder) {
		this(declaredbyMetadataId);
		setModifier(modifier);
		setParameterTypes(parameterTypes);
		setParameterNames(parameterNames);
		init(methodName, returnType);
		setBodyBuilder(bodyBuilder);
	}

	public MethodMetadataBuilder(String declaredbyMetadataId, int modifier, JavaSymbolName methodName, JavaType returnType, InvocableMemberBodyBuilder bodyBuilder) {
		this(declaredbyMetadataId, modifier, methodName, returnType, new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), bodyBuilder);
	}

	public MethodMetadata build() {
		return new DefaultMethodMetadata(getCustomData().build(), getDeclaredByMetadataId(), getModifier(), buildAnnotations(), getMethodName(), getReturnType(), getParameterTypes(), getParameterNames(), getThrowsTypes(), getBodyBuilder().getOutput());
	}
	
	private void init(JavaSymbolName methodName, JavaType returnType) {
		this.methodName = methodName;
		this.returnType = returnType;
	}

	public JavaSymbolName getMethodName() {
		return methodName;
	}

	public void setMethodName(JavaSymbolName methodName) {
		this.methodName = methodName;
	}

	public JavaType getReturnType() {
		return returnType;
	}

	public void setReturnType(JavaType returnType) {
		this.returnType = returnType;
	}
}
