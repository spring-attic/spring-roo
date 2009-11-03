package org.springframework.roo.classpath.details;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * Default implementation of {@link MethodMetadata}.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public class DefaultMethodMetadata extends AbstractInvocableMemberMetadata implements MethodMetadata {

	private JavaSymbolName methodName;
	private JavaType returnType;
	private List<JavaType> throwsTypes = new ArrayList<JavaType>();
	
	public DefaultMethodMetadata(String declaredByMetadataId, int modifier, JavaSymbolName methodName, JavaType returnType, List<AnnotatedJavaType> parameters, List<JavaSymbolName> parameterNames, List<AnnotationMetadata> annotations, List<JavaType> throwsTypes, String body) {
		super(declaredByMetadataId, modifier, parameters, parameterNames, annotations, body);
		Assert.notNull(methodName, "Method name required");
		Assert.notNull(returnType, "Return type required");
		this.methodName = methodName;
		this.returnType = returnType;
		
		if (throwsTypes != null) {
			this.throwsTypes = new ArrayList<JavaType>(throwsTypes.size());
			this.throwsTypes.addAll(throwsTypes);
		}
	}

	public JavaSymbolName getMethodName() {
		return methodName;
	}

	public JavaType getReturnType() {
		return returnType;
	}

	public List<JavaType> getThrowsTypes() {
		return throwsTypes;
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
		tsc.append("throwsTypes", throwsTypes);
		tsc.append("body", getBody());
		return tsc.toString();
	}


}
