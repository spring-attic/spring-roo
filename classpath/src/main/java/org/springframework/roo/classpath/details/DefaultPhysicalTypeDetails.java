package org.springframework.roo.classpath.details;

import org.apache.commons.lang3.Validate;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeDetails;
import org.springframework.roo.model.AbstractCustomDataAccessorProvider;
import org.springframework.roo.model.CustomDataImpl;
import org.springframework.roo.model.JavaType;

/**
 * Simple implementation of {@link PhysicalTypeDetails} that is suitable for
 * {@link PhysicalTypeCategory#OTHER} or sub-classing by category-specific
 * implementations.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class DefaultPhysicalTypeDetails extends
        AbstractCustomDataAccessorProvider implements PhysicalTypeDetails {

    private final JavaType javaType;
    private final PhysicalTypeCategory physicalTypeCategory;

    public DefaultPhysicalTypeDetails(
            final PhysicalTypeCategory physicalTypeCategory,
            final JavaType javaType) {
        super(CustomDataImpl.NONE);
        Validate.notNull(javaType, "Java type required");
        Validate.notNull(physicalTypeCategory,
                "Physical type category required");
        this.javaType = javaType;
        this.physicalTypeCategory = physicalTypeCategory;
    }

    public JavaType getName() {
        return getType();
    }

    public PhysicalTypeCategory getPhysicalTypeCategory() {
        return physicalTypeCategory;
    }

    public JavaType getType() {
        return javaType;
    }
}
