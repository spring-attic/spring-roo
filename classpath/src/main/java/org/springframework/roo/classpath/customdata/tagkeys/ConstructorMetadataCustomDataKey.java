package org.springframework.roo.classpath.customdata.tagkeys;

import org.springframework.roo.classpath.details.ConstructorMetadata;

/**
 * {@link ConstructorMetadata}-specific  implementation of {@link InvocableMemberMetadataCustomDataKey}.
 *
 * @author James Tyrrell
 * @since 1.1.3
 */
public class ConstructorMetadataCustomDataKey extends InvocableMemberMetadataCustomDataKey<ConstructorMetadata> {
	private String name;

	public ConstructorMetadataCustomDataKey(String name) {
		this.name = name;
	}

	public String toString() {
		return name;
	}

	public String name() {
		return name;
	}

	public boolean meets(ConstructorMetadata constructorMetadata) {
		return super.meets(constructorMetadata);
	}
}
