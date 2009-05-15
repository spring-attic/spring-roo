package org.springframework.roo.classpath.details;

import java.lang.reflect.Modifier;
import java.util.List;

import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.support.style.ToStringCreator;

/**
 * Default implementation of {@link ConstructorMetadata}.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public class DefaultConstructorMetadata extends AbstractInvocableMemberMetadata implements ConstructorMetadata {

	public DefaultConstructorMetadata(String declaredByMetadataId, int modifier, List<AnnotatedJavaType> parameters, List<JavaSymbolName> parameterNames, List<AnnotationMetadata> annotations, String body) {
		super(declaredByMetadataId, modifier, parameters, parameterNames, annotations, body);
	}
	
	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("declaredByMetadataId", getDeclaredByMetadataId());
		tsc.append("modifier", Modifier.toString(getModifier()));
		tsc.append("parameterTypes", getParameterTypes());
		tsc.append("parameterNames", getParameterNames());
		tsc.append("annotations", getAnnotations());
		tsc.append("body", getBody());
		return tsc.toString();
	}

}
