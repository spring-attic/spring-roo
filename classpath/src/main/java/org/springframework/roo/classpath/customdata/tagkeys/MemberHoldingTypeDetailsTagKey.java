package org.springframework.roo.classpath.customdata.tagkeys;

import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.model.TagKey;

/**
 * {@link MemberHoldingTypeDetails} specific implementation of {@link org.springframework.roo.model.TagKey}.
 * TODO: Create MemberHoldingTypeDetailsTagKeyBuilder
 *
 * @author James Tyrrell
 * @since 1.1.3
 */
public class MemberHoldingTypeDetailsTagKey implements TagKey<MemberHoldingTypeDetails> {

	private String tag;

	public MemberHoldingTypeDetailsTagKey(String tag) {
		this.tag = tag;
	}

	@Override
	public String toString() {
		return tag;
	}

	public void validate(MemberHoldingTypeDetails taggedInstance) throws IllegalStateException {}
}
