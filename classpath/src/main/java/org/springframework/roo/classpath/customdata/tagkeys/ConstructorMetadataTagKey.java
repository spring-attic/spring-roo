package org.springframework.roo.classpath.customdata.tagkeys;

import org.springframework.roo.classpath.details.ConstructorMetadata;

/**
 * {@link ConstructorMetadata} specific  implementation of {@link InvocableMemberMetadataTagKey}.
 * TODO: Create ConstructorMetadataTagKeyBuilder
 *
 * @author James Tyrrell
 * @since 1.1.3
 */
public class ConstructorMetadataTagKey extends InvocableMemberMetadataTagKey<ConstructorMetadata>{

	private String tag;

	public ConstructorMetadataTagKey(String tag) {
		this.tag = tag;
	}

	@Override
	public String toString() {
		return tag;
	}
}
