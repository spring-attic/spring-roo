package org.springframework.roo.classpath.details;

import org.apache.commons.lang3.Validate;
import org.springframework.roo.model.AbstractCustomDataAccessorProvider;
import org.springframework.roo.model.CustomData;

/**
 * Abstract class for {@link IdentifiableJavaStructure} subclasses.
 * 
 * @author Ben Alex
 * @since 1.1
 */
public abstract class AbstractIdentifiableJavaStructureProvider extends
        AbstractCustomDataAccessorProvider implements IdentifiableJavaStructure {

    private final String declaredByMetadataId;
    private final int modifier;

    /**
     * Constructor
     * 
     * @param customData
     * @param declaredByMetadataId
     * @param modifier
     */
    protected AbstractIdentifiableJavaStructureProvider(
            final CustomData customData, final String declaredByMetadataId,
            final int modifier) {
        super(customData);
        Validate.notBlank(declaredByMetadataId,
                "Declared by metadata ID required");
        this.declaredByMetadataId = declaredByMetadataId;
        this.modifier = modifier;
    }

    public final String getDeclaredByMetadataId() {
        return declaredByMetadataId;
    }

    public final int getModifier() {
        return modifier;
    }
}
