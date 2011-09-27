package org.springframework.roo.classpath.scanner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.roo.classpath.details.ConstructorMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.Assert;

/**
 * Default implementation of {@link MemberDetails}.
 * 
 * @author Ben Alex
 * @since 1.1
 */
public class MemberDetailsImpl implements MemberDetails {
	
	// Fields
	private  List<MemberHoldingTypeDetails> details;

	/**
	 * Constructs a new instance.
	 * 
	 * @param details the member holders that should be stored in this instance (cannot be null or empty)
	 */
	MemberDetailsImpl(List<MemberHoldingTypeDetails> details) {
		Assert.notEmpty(details, "Member holding details required");
		this.details = details;
	}
	
	public AnnotationMetadata getAnnotation(final JavaType type) {
		Assert.notNull(type, "Annotation type to locate required");
		for (MemberHoldingTypeDetails memberHoldingTypeDetails : this.details) {
			AnnotationMetadata md = memberHoldingTypeDetails.getAnnotation(type);
			if (md != null) {
				return md;
			}
		}
		return null;
	}
	
	public List<ConstructorMetadata> getConstructors() {
		final List<ConstructorMetadata> result = new ArrayList<ConstructorMetadata>();
		for (MemberHoldingTypeDetails memberHoldingTypeDetails : this.details) {
			result.addAll(memberHoldingTypeDetails.getDeclaredConstructors());
		}
		return result;
	}

	public List<MemberHoldingTypeDetails> getDetails() {
		return Collections.unmodifiableList(details);
	}
	
	public List<FieldMetadata> getFields() {
		final List<FieldMetadata> result = new ArrayList<FieldMetadata>();
		for (final MemberHoldingTypeDetails memberHoldingTypeDetails : this.details) {
			result.addAll(memberHoldingTypeDetails.getDeclaredFields());
		}
		return result;
	}
	
	public MethodMetadata getMethod(final JavaSymbolName methodName) {
		for (final MemberHoldingTypeDetails memberHoldingTypeDetails : this.details) {
			final MethodMetadata md = MemberFindingUtils.getDeclaredMethod(memberHoldingTypeDetails, methodName);
			if (md != null) {
				return md;
			}
		}
		return null;
	}
	
	public MethodMetadata getMethod(final JavaSymbolName methodName, final List<JavaType> parameters) {
		for (final MemberHoldingTypeDetails memberHoldingTypeDetails : this.details) {
			MethodMetadata md = MemberFindingUtils.getDeclaredMethod(memberHoldingTypeDetails, methodName, parameters);
			if (md != null) {
				return md;
			}
		}
		return null;
	}
	
	public MethodMetadata getMethod(final JavaSymbolName methodName, final List<JavaType> parameters, final String excludingMid) {
		for (final MemberHoldingTypeDetails memberHoldingTypeDetails : this.details) {
			MethodMetadata method = MemberFindingUtils.getDeclaredMethod(memberHoldingTypeDetails, methodName, parameters);
			if (method != null && !method.getDeclaredByMetadataId().equals(excludingMid)) {
				return method;
			}
		}
		return null;
	}
	
	public List<MethodMetadata> getMethods() {
		final List<MethodMetadata> result = new ArrayList<MethodMetadata>();
		for (MemberHoldingTypeDetails memberHoldingTypeDetails : this.details) {
			result.addAll(memberHoldingTypeDetails.getDeclaredMethods());
		}
		return result;
	}
	
	public List<MethodMetadata> getMethodsWithTag(final Object tagKey) {
		Assert.notNull(tagKey, "Custom data key required");
		final List<MethodMetadata> result = new ArrayList<MethodMetadata>();
		for (final MethodMetadata method : getMethods()) {
			if (method.getCustomData().keySet().contains(tagKey)) {
				result.add(method);
			}
		}
		return result;
	}
	
	public boolean isMethodDeclaredByAnother(final JavaSymbolName methodName, final List<JavaType> parameterTypes, final String declaredByMetadataId) {
		final MethodMetadata method = getMethod(methodName, parameterTypes);
		return method != null && !method.getDeclaredByMetadataId().equals(declaredByMetadataId);
	}
	
	public boolean isRequestingAnnotatedWith(final AnnotationMetadata annotationMetadata, final String requestingMid) {
		for (final MemberHoldingTypeDetails memberHoldingTypeDetails : this.details) {
			if (MemberFindingUtils.getAnnotationOfType(memberHoldingTypeDetails.getAnnotations(), annotationMetadata.getAnnotationType()) != null) {
				if (memberHoldingTypeDetails.getDeclaredByMetadataId().equals(requestingMid)) {
					return true;
				}
			}
		}
		return false;
	}
}
