package org.springframework.roo.classpath.customdata.taggers;

import org.springframework.roo.model.CustomDataKey;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link MemberHoldingTypeDetails}-specific implementation of {@link Matcher}. Matches
 * are based on the the type's MID.
 *
 * @author James Tyrrell
 * @since 1.1.3
 */
public class TypeMatcher implements Matcher<MemberHoldingTypeDetails> {
	private CustomDataKey<MemberHoldingTypeDetails> customDataKey;
	private String declaredBy;

	public TypeMatcher(CustomDataKey<MemberHoldingTypeDetails> customDataKey, String declaredBy) {
		this.customDataKey = customDataKey;
		this.declaredBy = declaredBy;
	}

	public List<MemberHoldingTypeDetails> matches(List<MemberHoldingTypeDetails> memberHoldingTypeDetailsList) {
		List<MemberHoldingTypeDetails> types = new ArrayList<MemberHoldingTypeDetails>();
		for (MemberHoldingTypeDetails memberHoldingTypeDetails : memberHoldingTypeDetailsList) {
			if (memberHoldingTypeDetails.getDeclaredByMetadataId().startsWith("MID:" + declaredBy)) {
				types.add(memberHoldingTypeDetails);
			}
		}
		return types;
	}

	public CustomDataKey<MemberHoldingTypeDetails> getCustomDataKey() {
		return customDataKey;
	}

	public Object getTagValue(MemberHoldingTypeDetails key) {
		return null;
	}
}
