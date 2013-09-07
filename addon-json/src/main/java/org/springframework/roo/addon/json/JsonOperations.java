package org.springframework.roo.addon.json;

import org.springframework.roo.model.JavaType;

/**
 * Interface of operations for addon-json operations.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
public interface JsonOperations {

    /**
     * Annotate all types in the project which are annotated with @
     * {@link org.springframework.roo.addon.javabean.RooJavaBean}.
     */
    @Deprecated
    void annotateAll();

    /**
     * Annotate all types in the project which are annotated with @
     * {@link org.springframework.roo.addon.javabean.RooJavaBean}.
     * 
     * @param deepSerialize Indication if deep serialization should be enabled
     *            (optional)
     */
    @Deprecated
    void annotateAll(boolean deepSerialize);

    /**
     * Annotate all types in the project which are annotated with @
     * {@link org.springframework.roo.addon.javabean.RooJavaBean}.
     * 
     * @param deepSerialize
     *            Indication if deep serialization should be enabled (optional)
     * @param iso8601Dates
     *            Indication if dates should be de/serialized in ISO 8601 format
     *            (optional)
     */
    void annotateAll(boolean deepSerialize, boolean iso8601Dates);

    /**
     * Annotate a given {@link JavaType} with @{@link RooJson} annotation.
     * 
     * @param type The type to annotate (required)
     * @param rootName The root name which should be used to wrap the JSON
     *            document (optional)
     */
    @Deprecated
    void annotateType(JavaType type, String rootName);

    /**
     * Annotate a given {@link JavaType} with @{@link RooJson} annotation.
     * 
     * @param type
     *            The type to annotate (required)
     * @param rootName
     *            The root name which should be used to wrap the JSON document
     *            (optional)
     * @param deepSerialize
     *            Indication if deep serialization should be enabled (optional)
     */
    @Deprecated
    void annotateType(JavaType type, String rootName, boolean deepSerialize);
    
    /**
     * Annotate a given {@link JavaType} with @{@link RooJson} annotation.
     * 
     * @param type
     *            The type to annotate (required)
     * @param rootName
     *            The root name which should be used to wrap the JSON document
     *            (optional)
     * @param deepSerialize
     *            Indication if deep serialization should be enabled (optional)
     * @param iso8601Dates
     *            Indication if dates should be de/serialized in ISO 8601 format
     *            (optional)
     */
    void annotateType(JavaType type, String rootName, boolean deepSerialize,
            boolean iso8601Dates);

    /**
     * Indicates whether this commands for this add-on should be available.
     * 
     * @return true if commands are available
     */
    boolean isJsonInstallationPossible();
}