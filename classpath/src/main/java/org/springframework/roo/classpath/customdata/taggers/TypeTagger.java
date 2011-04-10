package org.springframework.roo.classpath.customdata.taggers;

import org.springframework.roo.classpath.customdata.tagkeys.TagKey;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link MemberHoldingTypeDetails} specific implementation of {@link Tagger}. Matches
 * are based on the the type's MID.
 *
 * @author James Tyrrell
 * @since 1.1.3
 */
public class TypeTagger implements Tagger<MemberHoldingTypeDetails>{

	private TagKey<MemberHoldingTypeDetails> tagKey;
	private String declaredBy;

	public TypeTagger(TagKey<MemberHoldingTypeDetails> tagKey, String declaredBy) {
		this.tagKey = tagKey;
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

	public TagKey<MemberHoldingTypeDetails> getTagKey() {
		return tagKey;
	}

	public Object getTagValue(MemberHoldingTypeDetails key) {
		return null;
	}
}
