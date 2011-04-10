package org.springframework.roo.classpath.customdata.tagkeys;

import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;

/**
 * {@link MemberHoldingTypeDetails} specific implementation of {@link TagKey}.
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
