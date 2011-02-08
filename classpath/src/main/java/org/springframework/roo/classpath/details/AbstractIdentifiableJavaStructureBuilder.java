package org.springframework.roo.classpath.details;

import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.AbstractCustomDataAccessorBuilder;
import org.springframework.roo.model.Builder;
import org.springframework.roo.support.util.Assert;

/**
 * Assists in the creation of a {@link Builder} for types that eventually implement {@link IdentifiableJavaStructure}.
 * 
 * @author Ben Alex
 * @since 1.1
 */
public abstract class AbstractIdentifiableJavaStructureBuilder<T extends IdentifiableJavaStructure> extends AbstractCustomDataAccessorBuilder<T> {
	private String declaredByMetadataId;
	private int modifier;

	protected AbstractIdentifiableJavaStructureBuilder(String declaredByMetadataId) {
		Assert.isTrue(MetadataIdentificationUtils.isIdentifyingInstance(declaredByMetadataId), "Declared by metadata ID must identify a specific instance (not '" + declaredByMetadataId + "')");
		this.declaredByMetadataId = declaredByMetadataId;
	}

	protected AbstractIdentifiableJavaStructureBuilder(IdentifiableJavaStructure existing) {
		super(existing);
		this.declaredByMetadataId = existing.getDeclaredByMetadataId();
		this.modifier = existing.getModifier();
	}

	protected AbstractIdentifiableJavaStructureBuilder(String declaredbyMetadataId, IdentifiableJavaStructure existing) {
		super(existing);
		this.declaredByMetadataId = declaredbyMetadataId;
		this.modifier = existing.getModifier();		
	}

	public final String getDeclaredByMetadataId() {
		return declaredByMetadataId;
	}

	public final int getModifier() {
		return modifier;
	}

	public final void setModifier(int modifier) {
		this.modifier = modifier;
	}
}
