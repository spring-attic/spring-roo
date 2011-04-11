package org.springframework.roo.classpath.customdata.tagkeys;

import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.model.TagKey;

/**
 * {@link ItdTypeDetails} specific implementation of {@link org.springframework.roo.model.TagKey}.
 * TODO: Create ItdTypeDetailsTagKeyBuilder
 *
 * @author James Tyrrell
 * @since 1.1.3
 */
public class ItdTypeDetailsTagKey implements TagKey<ItdTypeDetails> {

	private String tag;

	public ItdTypeDetailsTagKey(String tag) {
		this.tag = tag;
	}

	public void validate(ItdTypeDetails taggedInstance) throws IllegalStateException {}

	@Override
	public String toString() {
		return tag;
	}
}
