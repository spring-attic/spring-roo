package org.springframework.roo.classpath.details;

import java.lang.reflect.Modifier;

import org.springframework.roo.model.CustomDataAccessor;

/**
 * Allows an identifiable Java structure (ie a member or a type) to be traced
 * back to its declaring type.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public interface IdentifiableJavaStructure extends CustomDataAccessor {

    /**
     * @return the ID of the metadata that declared this member (never null)
     */
    String getDeclaredByMetadataId();

    /**
     * Indicates the access modifier of the member. The integer is formatted in
     * accordance with {@link Modifier}. Returning 0 is acceptable the less
     * common structures that don't support modifiers (eg static initializers).
     * 
     * @return the modifier, if available (required)
     */
    int getModifier();
}
