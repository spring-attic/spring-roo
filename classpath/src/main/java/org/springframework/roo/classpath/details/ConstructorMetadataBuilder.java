package org.springframework.roo.classpath.details;

/**
 * Builder for {@link ConstructorMetadata}.
 * 
 * @author Ben Alex
 * @since 1.1
 */
public class ConstructorMetadataBuilder extends
        AbstractInvocableMemberMetadataBuilder<ConstructorMetadata> {

    /**
     * Constructor
     * 
     * @param existing
     */
    public ConstructorMetadataBuilder(final ConstructorMetadata existing) {
        super(existing);
    }

    /**
     * Constructor
     * 
     * @param declaredbyMetadataId
     */
    public ConstructorMetadataBuilder(final String declaredbyMetadataId) {
        super(declaredbyMetadataId);
    }

    /**
     * Constructor
     * 
     * @param declaredbyMetadataId
     * @param existing
     */
    public ConstructorMetadataBuilder(final String declaredbyMetadataId,
            final ConstructorMetadata existing) {
        super(declaredbyMetadataId, existing);
    }

    public ConstructorMetadata build() {
        return new DefaultConstructorMetadata(getCustomData().build(),
                getDeclaredByMetadataId(), getModifier(), buildAnnotations(),
                getParameterTypes(), getParameterNames(), getThrowsTypes(),
                getBodyBuilder().getOutput());
    }
}
