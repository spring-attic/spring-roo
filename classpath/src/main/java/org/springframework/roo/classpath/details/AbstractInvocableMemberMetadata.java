package org.springframework.roo.classpath.details;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.model.CustomData;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Abstract implementation of {@link InvocableMemberMetadata}.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public abstract class AbstractInvocableMemberMetadata extends AbstractIdentifiableAnnotatedJavaStructureProvider implements InvocableMemberMetadata {

	private List<JavaSymbolName> parameterNames =  new ArrayList<JavaSymbolName>();
	private List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
	private List<JavaType> throwsTypes = new ArrayList<JavaType>();
	private String body;
	
	public AbstractInvocableMemberMetadata(CustomData customData, String declaredByMetadataId, int modifier, List<AnnotationMetadata> annotations, List<AnnotatedJavaType> parameterTypes, List<JavaSymbolName> parameterNames, List<JavaType> throwsTypes, String body) {
		super(customData, declaredByMetadataId, modifier, annotations);

		if (parameterTypes != null) {
			this.parameterTypes = new ArrayList<AnnotatedJavaType>(parameterTypes.size());
			this.parameterTypes.addAll(parameterTypes);
		}
		
		if (parameterNames != null) {
			this.parameterNames = new ArrayList<JavaSymbolName>(parameterNames.size());
			this.parameterNames.addAll(parameterNames);
		}
		
		if (throwsTypes != null) {
			this.throwsTypes = new ArrayList<JavaType>(throwsTypes.size());
			this.throwsTypes.addAll(throwsTypes);
		}

		this.body = body;
	}
	
	public final List<JavaSymbolName> getParameterNames() {
		return Collections.unmodifiableList(parameterNames);
	}

	public final List<AnnotatedJavaType> getParameterTypes() {
		return Collections.unmodifiableList(parameterTypes);
	}
	
	public final List<JavaType> getThrowsTypes() {
		return Collections.unmodifiableList(throwsTypes);
	}

	public final String getBody() {
		return body;
	}
}
