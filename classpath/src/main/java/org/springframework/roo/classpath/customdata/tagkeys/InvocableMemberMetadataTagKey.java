package org.springframework.roo.classpath.customdata.tagkeys;

import org.springframework.roo.classpath.details.InvocableMemberMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

import java.util.List;

/**
 * {@link InvocableMemberMetadata} specific implementation of
 * {@link IdentifiableAnnotatedJavaStructureTagKey}.
 *
 * @author James Tyrrell
 * @since 1.1.3
 */
public abstract class InvocableMemberMetadataTagKey<T extends InvocableMemberMetadata> extends IdentifiableAnnotatedJavaStructureTagKey<T>{

	private List<AnnotatedJavaType> parameterTypes;
	private List<JavaSymbolName> parameterNames;
	private List<JavaType> throwsTypes;

	protected InvocableMemberMetadataTagKey(Integer modifier, List<AnnotationMetadata> annotations, List<AnnotatedJavaType> parameterTypes, List<JavaSymbolName> parameterNames, List<JavaType> throwsTypes) {
		super(modifier, annotations);
		this.parameterTypes = parameterTypes;
		this.parameterNames = parameterNames;
		this.throwsTypes = throwsTypes;
	}

	protected InvocableMemberMetadataTagKey() {
		super();
	}

	public void validate(T taggedInstance) throws IllegalStateException {
		super.validate(taggedInstance);
		//TODO: Add in validation logic for parameterTypes, parameterNames, throwsTypes
	}

	public List<AnnotatedJavaType> getParameterTypes() {
		return parameterTypes;
	}

	public List<JavaSymbolName> getParameterNames() {
		return parameterNames;
	}

	public List<JavaType> getThrowsTypes() {
		return throwsTypes;
	}
}
