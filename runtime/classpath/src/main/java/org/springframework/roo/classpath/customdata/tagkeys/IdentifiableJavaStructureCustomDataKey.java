package org.springframework.roo.classpath.customdata.tagkeys;

import org.springframework.roo.classpath.details.IdentifiableJavaStructure;
import org.springframework.roo.model.CustomDataKey;

/**
 * {@link IdentifiableJavaStructure}-specific implementation of
 * {@link org.springframework.roo.model.CustomDataKey}.
 * 
 * @author James Tyrrell
 * @since 1.1.3
 */
public abstract class IdentifiableJavaStructureCustomDataKey<T extends IdentifiableJavaStructure>
        implements CustomDataKey<T> {
    private Integer modifier;

    protected IdentifiableJavaStructureCustomDataKey() {
    }

    public IdentifiableJavaStructureCustomDataKey(final Integer modifier) {
        this.modifier = modifier;
    }

    public Integer getModifier() {
        return modifier;
    }

    public boolean meets(final T identifiableJavaStructure)
            throws IllegalStateException {
        // TODO: Add in validation logic for modifier
        return true;
    }
}
