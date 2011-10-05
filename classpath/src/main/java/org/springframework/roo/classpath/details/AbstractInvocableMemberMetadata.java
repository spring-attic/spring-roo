package org.springframework.roo.classpath.details;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.model.CustomData;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.CollectionUtils;

/**
 * Abstract implementation of {@link InvocableMemberMetadata}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public abstract class AbstractInvocableMemberMetadata extends AbstractIdentifiableAnnotatedJavaStructureProvider implements InvocableMemberMetadata {

	// Fields
	private final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
	private final List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
	private final List<JavaType> throwsTypes = new ArrayList<JavaType>();
	private final String body;
	
	/**
	 * Constructor
	 *
	 * @param customData
	 * @param declaredByMetadataId
	 * @param modifier
	 * @param annotations
	 * @param parameterTypes
	 * @param parameterNames
	 * @param throwsTypes
	 * @param body
	 */
	protected AbstractInvocableMemberMetadata(CustomData customData, String declaredByMetadataId, int modifier, List<AnnotationMetadata> annotations, List<AnnotatedJavaType> parameterTypes, List<JavaSymbolName> parameterNames, List<JavaType> throwsTypes, String body) {
		super(customData, declaredByMetadataId, modifier, annotations);
		this.body = body;
		CollectionUtils.populate(this.parameterNames, parameterNames);
		CollectionUtils.populate(this.parameterTypes, parameterTypes);
		CollectionUtils.populate(this.throwsTypes, throwsTypes);
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
