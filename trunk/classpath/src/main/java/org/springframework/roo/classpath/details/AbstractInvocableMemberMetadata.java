package org.springframework.roo.classpath.details;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.support.util.Assert;

/**
 * Abstract implementation of {@link InvocableMemberMetadata}.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public abstract class AbstractInvocableMemberMetadata implements InvocableMemberMetadata {

	private List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
	private List<JavaSymbolName> parameterNames =  new ArrayList<JavaSymbolName>();
	private List<AnnotatedJavaType> parameters = new ArrayList<AnnotatedJavaType>();
	private String body;
	private String declaredByMetadataId;
	private int modifier;
	
	public AbstractInvocableMemberMetadata(String declaredByMetadataId, int modifier, List<AnnotatedJavaType> parameters, List<JavaSymbolName> parameterNames, List<AnnotationMetadata> annotations, String body) {
		Assert.hasText(declaredByMetadataId, "Declared by metadata ID required");
		this.declaredByMetadataId = declaredByMetadataId;

		if (parameters != null) {
			this.parameters = new ArrayList<AnnotatedJavaType>(parameters.size());
			this.parameters.addAll(parameters);
		}
		
		if (parameterNames != null) {
			this.parameterNames = new ArrayList<JavaSymbolName>(parameterNames.size());
			this.parameterNames.addAll(parameterNames);
		}
		
		this.body = body;
		this.modifier = modifier;
		
		if (annotations != null) {
			this.annotations = new ArrayList<AnnotationMetadata>(annotations.size());
			this.annotations.addAll(annotations);
		}
	}
	
	public String getDeclaredByMetadataId() {
		return declaredByMetadataId;
	}

	public List<AnnotationMetadata> getAnnotations() {
		return Collections.unmodifiableList(annotations);
	}
	
	public List<JavaSymbolName> getParameterNames() {
		return Collections.unmodifiableList(parameterNames);
	}

	public List<AnnotatedJavaType> getParameterTypes() {
		return Collections.unmodifiableList(parameters);
	}
	
	public int getModifier() {
		return modifier;
	}

	public String getBody() {
		return body;
	}
	
}
