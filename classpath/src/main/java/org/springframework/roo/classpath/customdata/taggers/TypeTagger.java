package org.springframework.roo.classpath.customdata.taggers;

import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.model.TagKey;
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
public class TypeTagger implements Tagger<ItdTypeDetails>{

	private TagKey<ItdTypeDetails> tagKey;
	private String declaredBy;

	public TypeTagger(TagKey<ItdTypeDetails> tagKey, String declaredBy) {
		this.tagKey = tagKey;
		this.declaredBy = declaredBy;
	}

	public List<ItdTypeDetails> matches(List<MemberHoldingTypeDetails> memberHoldingTypeDetailsList) {
		List<ItdTypeDetails> types = new ArrayList<ItdTypeDetails>();
		for (MemberHoldingTypeDetails memberHoldingTypeDetails : memberHoldingTypeDetailsList) {
			if (memberHoldingTypeDetails.getDeclaredByMetadataId().startsWith("MID:" + declaredBy)) {
				types.add((ItdTypeDetails) memberHoldingTypeDetails);
			}
		}
		return types;
	}

	public TagKey<ItdTypeDetails> getTagKey() {
		return tagKey;
	}

	public Object getTagValue(ItdTypeDetails key) {
		return null;
	}
}
