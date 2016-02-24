package org.springframework.roo.classpath.details;

import org.apache.commons.lang3.Validate;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.AbstractCustomDataAccessorBuilder;
import org.springframework.roo.model.Builder;

/**
 * Assists in the creation of a {@link Builder} for types that eventually
 * implement {@link IdentifiableJavaStructure}.
 * 
 * @author Ben Alex
 * @since 1.1
 */
public abstract class AbstractIdentifiableJavaStructureBuilder<T extends IdentifiableJavaStructure>
        extends AbstractCustomDataAccessorBuilder<T> {

    private final String declaredByMetadataId;
    private int modifier;

    /**
     * Constructor
     * 
     * @param existing
     */
    protected AbstractIdentifiableJavaStructureBuilder(
            final IdentifiableJavaStructure existing) {
        super(existing);
        this.declaredByMetadataId = existing.getDeclaredByMetadataId();
        this.modifier = existing.getModifier();
    }

    /**
     * Constructor
     * 
     * @param declaredByMetadataId
     */
    protected AbstractIdentifiableJavaStructureBuilder(
            final String declaredByMetadataId) {
        Validate.isTrue(
                MetadataIdentificationUtils
                        .isIdentifyingInstance(declaredByMetadataId),
                "Declared by metadata ID must identify a specific instance (not '%s')",
                declaredByMetadataId);
        this.declaredByMetadataId = declaredByMetadataId;
    }

    /**
     * Constructor
     * 
     * @param declaredbyMetadataId
     * @param existing
     */
    protected AbstractIdentifiableJavaStructureBuilder(
            final String declaredbyMetadataId,
            final IdentifiableJavaStructure existing) {
        super(existing);
        this.declaredByMetadataId = declaredbyMetadataId;
        this.modifier = existing.getModifier();
    }

    public String getDeclaredByMetadataId() {
        return declaredByMetadataId;
    }

    public final int getModifier() {
        return modifier;
    }

    /**
     * Sets the modifier of the built Java structure
     * 
     * @param modifier the modifier to set
     * @see Modifier#PUBLIC, etc.
     */
    public final void setModifier(final int modifier) {
        this.modifier = modifier;
    }
}
