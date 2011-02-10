package org.springframework.roo.addon.web.mvc.controller.converter;

import java.util.List;

import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.model.CustomData;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Provides an easy way to create a {@link MethodMetadata} instance for testing purposes.
 * 
 * @author Rossen Stoyanchev
 * @since 1.1.1
 */
public class StubMethodMetadata implements MethodMetadata {

	private JavaSymbolName methodName;
	private JavaType returnType;
	private List<AnnotatedJavaType> parameterTypes;
	private List<JavaSymbolName> parameterNames;
	private List<JavaType> throwsTypes;
	private String body;
	private List<AnnotationMetadata> annotations;
	private String declaredByMetadataId;
	private int modifier;
	private CustomData customData;

	public StubMethodMetadata(JavaSymbolName methodName, JavaType returnType) {
		this.methodName = methodName;
		this.returnType = returnType;
	}

	public StubMethodMetadata(String methodName, Class<?> returnType) {
		this(new JavaSymbolName(methodName), new JavaType(returnType.getName()));
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

	public List<AnnotatedJavaType> getParameterTypes() {
		return parameterTypes;
	}

	public void setParameterTypes(List<AnnotatedJavaType> parameterTypes) {
		this.parameterTypes = parameterTypes;
	}

	public List<JavaSymbolName> getParameterNames() {
		return parameterNames;
	}

	public void setParameterNames(List<JavaSymbolName> parameterNames) {
		this.parameterNames = parameterNames;
	}

	public List<JavaType> getThrowsTypes() {
		return throwsTypes;
	}

	public void setThrowsTypes(List<JavaType> throwsTypes) {
		this.throwsTypes = throwsTypes;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public List<AnnotationMetadata> getAnnotations() {
		return annotations;
	}

	public void setAnnotations(List<AnnotationMetadata> annotations) {
		this.annotations = annotations;
	}

	public String getDeclaredByMetadataId() {
		return declaredByMetadataId;
	}

	public void setDeclaredByMetadataId(String declaredByMetadataId) {
		this.declaredByMetadataId = declaredByMetadataId;
	}

	public int getModifier() {
		return modifier;
	}

	public void setModifier(int modifier) {
		this.modifier = modifier;
	}

	public CustomData getCustomData() {
		return customData;
	}

	public void setCustomData(CustomData customData) {
		this.customData = customData;
	}
	
}
