package org.springframework.roo.classpath;

import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.support.util.Assert;

/**
 * Provides string manipulation functions for {@link PhysicalTypeMetadata} IDs.
 *
 * @author Ben Alex
 * @since 1.0
 */
public final class PhysicalTypeIdentifier {

	// Constants
	private static final String PHYSICAL_METADATA_TYPE = PhysicalTypeIdentifier.class.getName();
	
	/**
	 * The class-level ID for physical type metadata.
	 * 
	 * @since 1.2.0
	 */
	public static final String PHYSICAL_METADATA_TYPE_ID = MetadataIdentificationUtils.create(PHYSICAL_METADATA_TYPE);

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
	 * Creates a physical type metadata ID for the given user project type,
	 * which need not exist. If you know the {@link JavaType} exists but don't
	 * know its {@link LogicalPath}, you can use
	 * {@link TypeLocationService#getPhysicalTypeIdentifier(JavaType)} instead.
	 *
	 * @param javaType the type for which to create the identifier (required)
	 * @param path the path in which it's located (required)
	 * @return a non-blank ID
	 */
	public static String createIdentifier(final JavaType javaType, final LogicalPath path) {
		return PhysicalTypeIdentifierNamingUtils.createIdentifier(PHYSICAL_METADATA_TYPE, javaType, path);
	}

	/**
	 * Parses the given metadata ID for the user project type to which it relates.
	 * 
	 * @param physicalTypeId the metadata ID to parse (must identify an instance of {@link PhysicalTypeIdentifier#PHYSICAL_METADATA_TYPE})
	 * @return a non-<code>null</code> type
	 */
	public static JavaType getJavaType(final String physicalTypeId) {
		Assert.isTrue(PhysicalTypeIdentifier.isValid(physicalTypeId), "Physical type identifier is invalid");
		return PhysicalTypeIdentifierNamingUtils.getJavaType(PHYSICAL_METADATA_TYPE, physicalTypeId);
	}

	/**
	 * Parses the given metadata ID for the path of the user project type to which it relates.
	 * 
	 * @param metadataId the metadata ID to parse (must identify an instance of {@link PhysicalTypeIdentifier#PHYSICAL_METADATA_TYPE})
	 * @return a non-<code>null</code> path
	 */
	public static LogicalPath getPath(final String metadataId) {
		return PhysicalTypeIdentifierNamingUtils.getPath(PHYSICAL_METADATA_TYPE, metadataId);
	}

	/**
	 * Indicates whether the given metadata ID identifies a physical Java type,
	 * in other words an interface, class, annotation, or enum.
	 *
	 * @param metadataIdentificationString the metadata ID to check
	 * @return see above
	 */
	public static boolean isValid(final String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.isValid(PHYSICAL_METADATA_TYPE, metadataIdentificationString);
	}

	public static String getFriendlyName(final String metadataId) {
		Assert.isTrue(isValid(metadataId), "Invalid metadata id '" + metadataId + "'");
		return getPath(metadataId) + "/" + getJavaType(metadataId);
	}

	/**
	 * Constructor is private to prevent instantiation
	 * 
	 * @since 1.2.0
	 */
	private PhysicalTypeIdentifier() {}
}
