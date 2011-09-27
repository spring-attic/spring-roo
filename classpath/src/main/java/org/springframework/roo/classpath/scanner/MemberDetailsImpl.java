package org.springframework.roo.classpath.scanner;

import java.util.Collections;
import java.util.List;

import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
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
	 * Constructors a new instance.
	 * 
	 * @param details the member holders that should be stored in this instance (cannot be null or empty)
	 */
	MemberDetailsImpl(List<MemberHoldingTypeDetails> details) {
		Assert.notEmpty(details, "Member holding details required");
		this.details = details;
	}

	public List<MemberHoldingTypeDetails> getDetails() {
		return Collections.unmodifiableList(details);
	}

	/**
	 * Locates the specified type-level annotation on any of the
	 * {@link MemberHoldingTypeDetails} in this {@link MemberDetails}.
	 * 
	 * @param memberDetails the {@link MemberDetails} to search (required)
	 * @param type to locate (required)
	 * @return the annotation, or null if not found
	 */
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
}
