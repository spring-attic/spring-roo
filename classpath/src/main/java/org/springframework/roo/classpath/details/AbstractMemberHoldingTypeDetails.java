package org.springframework.roo.classpath.details;

import java.util.Collection;

import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.model.CustomData;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.support.util.Assert;

/**
 * Convenient superclass for {@link MemberHoldingTypeDetails} implementations.
 *
 * @author Andrew Swan
 * @since 1.2.0
 */
public abstract class AbstractMemberHoldingTypeDetails extends AbstractIdentifiableAnnotatedJavaStructureProvider implements MemberHoldingTypeDetails {

	/**
	 * Constructor
	 *
	 * @param customData
	 * @param declaredByMetadataId
	 * @param modifier
	 * @param annotations
	 */
	protected AbstractMemberHoldingTypeDetails(final CustomData customData, final String declaredByMetadataId, final int modifier, final Collection<AnnotationMetadata> annotations) {
		super(customData, declaredByMetadataId, modifier, annotations);
	}

	public JavaSymbolName getUniqueFieldName(final String proposedName, final boolean prepend) {
		Assert.hasText(proposedName, "Proposed field name is required");
		String candidateName = proposedName;
		while (MemberFindingUtils.getField(this, new JavaSymbolName(candidateName)) != null) {
			// The proposed field name is taken; differentiate it
			if (prepend) {
				candidateName = "_" + candidateName;
			} else {
				// Append
				candidateName = candidateName + "_";
			}
		}
		// We've derived a unique name
		return new JavaSymbolName(candidateName);
	}
}