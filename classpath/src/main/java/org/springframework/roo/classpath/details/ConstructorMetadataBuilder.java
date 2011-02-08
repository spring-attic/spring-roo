package org.springframework.roo.classpath.details;

/**
 * Builder for {@link ConstructorMetadata}.
 * 
 * @author Ben Alex
 * @since 1.1
 */
public final class ConstructorMetadataBuilder extends AbstractInvocableMemberMetadataBuilder<ConstructorMetadata> {

	public ConstructorMetadataBuilder(String declaredbyMetadataId) {
		super(declaredbyMetadataId);
	}

	public ConstructorMetadataBuilder(ConstructorMetadata existing) {
		super(existing);
	}

	public ConstructorMetadataBuilder(String declaredbyMetadataId, ConstructorMetadata existing) {
		super(declaredbyMetadataId, existing);
	}

	public ConstructorMetadata build() {
		return new DefaultConstructorMetadata(getCustomData().build(), getDeclaredByMetadataId(), getModifier(), buildAnnotations(), getParameterTypes(), getParameterNames(), getThrowsTypes(), getBodyBuilder().getOutput());
	}
}
