package org.springframework.roo.classpath;

import org.springframework.roo.model.CustomDataAccessor;
import org.springframework.roo.model.JavaType;

/**
 * Provides details of the actual type presented by a
 * {@link PhysicalTypeMetadata} instance.
 * <p>
 * Sub-interfaces are created for different major Java types, such as those
 * specific to classes, enums, and annotations. This allows sub-interfaces to
 * provide accessors applicable to the specific category of Java type.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public interface PhysicalTypeDetails extends CustomDataAccessor {

    /**
     * @see #getType(), which returns the same thing but is better named
     */
    JavaType getName();

    /**
     * @return the category of Java type being provided by this
     *         {@link PhysicalTypeDetails} instance (never null)
     */
    PhysicalTypeCategory getPhysicalTypeCategory();

    /**
     * Returns the {@link JavaType} provided by this physical type. If possible,
     * indicates any type parameters.
     * 
     * @return the full name of the type that members will eventually be
     *         available from when compiled, including any available type
     *         parameters (may be null if unable to parse)
     * @since 1.2.0
     */
    JavaType getType();
}
