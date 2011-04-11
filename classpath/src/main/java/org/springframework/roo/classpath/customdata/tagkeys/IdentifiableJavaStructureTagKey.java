package org.springframework.roo.classpath.customdata.tagkeys;

import org.springframework.roo.classpath.details.IdentifiableJavaStructure;
import org.springframework.roo.model.TagKey;

/**
 * {@link IdentifiableJavaStructure} specific implementation of {@link TagKey}.
 *
 * @author James Tyrrell
 * @since 1.1.3
 */
public abstract class IdentifiableJavaStructureTagKey<T extends IdentifiableJavaStructure> implements TagKey<T> {

	private Integer modifier;

	public IdentifiableJavaStructureTagKey(Integer modifier) {
		this.modifier = modifier;
	}

	protected IdentifiableJavaStructureTagKey() {}

	public Integer getModifier() {
		return modifier;
	}

	public void validate(T taggedInstance) throws IllegalStateException {
		//TODO: Add in validation logic for modifier
	}
}
