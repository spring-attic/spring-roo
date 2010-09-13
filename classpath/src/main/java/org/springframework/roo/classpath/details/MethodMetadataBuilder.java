package org.springframework.roo.classpath.details;

import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Builder for {@link MethodMetadata}.
 * 
 * @author Ben Alex
 * @since 1.1
 *
 */
public final class MethodMetadataBuilder extends AbstractInvocableMemberMetadataBuilder<MethodMetadata> {

	private JavaSymbolName methodName;
	private JavaType returnType;
	
	public MethodMetadataBuilder(String declaredbyMetadataId) {
		super(declaredbyMetadataId);
	}
	
	public MethodMetadataBuilder(MethodMetadata existing) {
		super(existing);
		this.methodName = existing.getMethodName();
		this.returnType = existing.getReturnType();
	}
	
	public MethodMetadata build() {
		return new DefaultMethodMetadata(getCustomData().build(), getDeclaredByMetadataId(), getModifier(), buildAnnotations(), getMethodName(), getReturnType(), getParameterTypes(), getParameterNames(), getThrowsTypes(), getBodyBuilder().getOutput());
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
