package org.springframework.roo.classpath.customdata.tagkeys;

import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.model.CustomDataKey;

/**
 * {@link MemberHoldingTypeDetails}-specific implementation of {@link org.springframework.roo.model.CustomDataKey}.
 *
 * @author James Tyrrell
 * @since 1.1.3
 */
public class MemberHoldingTypeDetailsCustomDataKey implements CustomDataKey<MemberHoldingTypeDetails> {
	private String name;

	public MemberHoldingTypeDetailsCustomDataKey(String name) {
		this.name = name;
	}

	public String toString() {
		return name;
	}

	public boolean meets(MemberHoldingTypeDetails memberHoldingTypeDetails) throws IllegalStateException {
		return true;
	}

	public String name() {
		return name;
	}
}
