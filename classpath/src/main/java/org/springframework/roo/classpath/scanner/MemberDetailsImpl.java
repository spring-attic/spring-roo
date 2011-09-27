package org.springframework.roo.classpath.scanner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.roo.classpath.details.ConstructorMetadata;
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
	
	public MethodMetadata getMethod(final JavaSymbolName methodName, final List<JavaType> parameters) {
		for (final MemberHoldingTypeDetails memberHoldingTypeDetails : this.details) {
			MethodMetadata md = MemberFindingUtils.getDeclaredMethod(memberHoldingTypeDetails, methodName, parameters);
			if (md != null) {
				return md;
			}
		}
		return null;
	}
}
