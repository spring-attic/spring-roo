package org.springframework.roo.classpath;

import org.apache.commons.lang3.Validate;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * Provides string manipulation functions for {@link PhysicalTypeMetadata} IDs.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public final class PhysicalTypeIdentifier {

    private static final String PHYSICAL_METADATA_TYPE = PhysicalTypeIdentifier.class
            .getName();

    /**
     * The class-level ID for physical type metadata.
     * 
     * @since 1.2.0
     */
    public static final String PHYSICAL_METADATA_TYPE_ID = MetadataIdentificationUtils
            .create(PHYSICAL_METADATA_TYPE);

    /**
     * Creates a physical type metadata ID for the given user project type,
     * which need not exist. If you know the {@link JavaType} exists but don't
     * know its {@link LogicalPath}, you can use
     * {@link TypeLocationService#getPhysicalTypeIdentifier(JavaType)} instead.
     * 
     * @param javaType the type for which to create the identifier (required)
     * @param path the path in which it's located (required)
     * @return a non-blank ID
     */
    public static String createIdentifier(final JavaType javaType,
            final LogicalPath path) {
        return PhysicalTypeIdentifierNamingUtils.createIdentifier(
                PHYSICAL_METADATA_TYPE, javaType, path);
    }

    public static String getFriendlyName(final String metadataId) {
        Validate.isTrue(isValid(metadataId), "Invalid metadata id '%s'",
                metadataId);
        return getPath(metadataId) + "/" + getJavaType(metadataId);
    }

    /**
     * Parses the given metadata ID for the user project type to which it
     * relates.
     * 
     * @param physicalTypeId the metadata ID to parse (must identify an instance
     *            of {@link PhysicalTypeIdentifier#PHYSICAL_METADATA_TYPE})
     * @return a non-<code>null</code> type
     */
    public static JavaType getJavaType(final String physicalTypeId) {
        Validate.isTrue(PhysicalTypeIdentifier.isValid(physicalTypeId),
                "Physical type identifier is invalid");
        return PhysicalTypeIdentifierNamingUtils.getJavaType(
                PHYSICAL_METADATA_TYPE, physicalTypeId);
    }

    /**
     * Returns the class-level ID for physical type metadata. Equivalent to
     * accessing {@link #PHYSICAL_METADATA_TYPE_ID} directly.
     * 
     * @return {@value #PHYSICAL_METADATA_TYPE_ID}
     */
    public static String getMetadataIdentiferType() {
        return PHYSICAL_METADATA_TYPE_ID;
    }

    /**
     * Parses the given metadata ID for the path of the user project type to
     * which it relates.
     * 
     * @param metadataId the metadata ID to parse (must identify an instance of
     *            {@link PhysicalTypeIdentifier#PHYSICAL_METADATA_TYPE})
     * @return a non-<code>null</code> path
     */
    public static LogicalPath getPath(final String metadataId) {
        return PhysicalTypeIdentifierNamingUtils.getPath(
                PHYSICAL_METADATA_TYPE, metadataId);
    }

    /**
     * Indicates whether the given metadata ID identifies a physical Java type,
     * in other words an interface, class, annotation, or enum.
     * 
     * @param metadataIdentificationString the metadata ID to check
     * @return see above
     */
    public static boolean isValid(final String metadataIdentificationString) {
        return PhysicalTypeIdentifierNamingUtils.isValid(
                PHYSICAL_METADATA_TYPE, metadataIdentificationString);
    }

    /**
     * Constructor is private to prevent instantiation
     * 
     * @since 1.2.0
     */
    private PhysicalTypeIdentifier() {
    }
}
