package org.springframework.roo.classpath;

import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.ContextualPath;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.util.Assert;

/**
 * Produces metadata identification strings that represent a {@link JavaType} 
 * located in a particular {@link ContextualPath}.
 * 
 * <p>
 * The metadata identification strings separate the path name from the fully qualified
 * type name via the presence of a question mark character ("?"). A question mark is used
 * given it is reserved by {@link Path}.
 * 
 * TODO these methods are not specific to physical types; either rename this
 * class, move them somewhere more generic, and/or make them more specific, e.g.
 * hardcode the "metadata class" arguments to that of physical types.
 *
 * @author Ben Alex
 * @since 1.0
 */
public final class PhysicalTypeIdentifierNamingUtils {

	// Constants
	private static final String PATH_SUFFIX = "?";

	/**
	 * Creates a metadata ID from the given inputs
	 *
	 * @param metadataClass the fully-qualified name of the metadata class (required)
	 * @param projectType the fully-qualified name of the user project type to which the metadata relates (required)
	 * @param path the path to that type within the project (required)
	 * @return a non-blank ID
	 */
	public static String createIdentifier(final String metadataClass, final JavaType projectType, final ContextualPath path) {
		Assert.notNull(projectType, "Java type required");
		Assert.notNull(path, "Path required");
		return MetadataIdentificationUtils.create(metadataClass, path.getName() + PATH_SUFFIX + projectType.getFullyQualifiedTypeName());
	}

	/**
	 * Returns the user project type with which the given metadata ID is associated.
	 * 
	 * @param metadataClass the fully-qualified name of the metadata type (required)
	 * @param metadataId the ID of the metadata instance (must identify an instance of the given metadata class)
	 * @return a non-<code>null</code> type
	 */
	public static JavaType getJavaType(final String metadataClass, final String metadataId) {
		final String instanceKey = getInstanceKey(metadataClass, metadataId);
		return new JavaType(instanceKey.substring(instanceKey.indexOf(PATH_SUFFIX) + 1));
	}

	/**
	 * Parses the user project path from the given metadata ID.
	 * 
	 * @param metadataClass the fully-qualified name of the metadata type (required)
	 * @param metadataId the ID of the metadata instance (must identify an instance of the given metadata class)
	 * @return a non-<code>null</code> path
	 */
	public static ContextualPath getPath(final String providesType, final String metadataIdentificationString) {
		Assert.isTrue(isValid(providesType, metadataIdentificationString), "Metadata identification string '" + metadataIdentificationString + "' does not appear to be a valid physical type identifier");
		String instance = MetadataIdentificationUtils.getMetadataInstance(metadataIdentificationString);
		int index = instance.indexOf("?");
		return ContextualPath.getInstance(instance.substring(0, index));
	}

	public static ContextualPath getPath(final String metadataIdentificationString) {
		Assert.isTrue(metadataIdentificationString.contains("#"), "Metadata identification string '" + metadataIdentificationString + "' does not appear to be a valid identifier");
		String instance = MetadataIdentificationUtils.getMetadataInstance(metadataIdentificationString);
		int index = instance.indexOf("?");
		return ContextualPath.getInstance(instance.substring(0, index));
	}

	public static JavaType getJavaType(final String metadataIdentificationString) {
		Assert.isTrue(metadataIdentificationString.contains("#"), "Metadata identification string '" + metadataIdentificationString + "' does not appear to be a valid identifier");
		String instance = MetadataIdentificationUtils.getMetadataInstance(metadataIdentificationString);
		int index = instance.indexOf("?");
		return new JavaType(instance.substring(index+1));
	}
	
	/**
	 * Parses the instance key from the given metadata ID.
	 * 
	 * @param metadataClass the fully-qualified name of the metadata type (required)
	 * @param metadataId the ID of the metadata instance (must identify an instance of the given metadata class)
	 * @return a non-blank key, as per {@link MetadataIdentificationUtils#getMetadataInstance(String)}
	 */
	private static String getInstanceKey(final String metadataClass, final String metadataId) {
		Assert.isTrue(isValid(metadataClass, metadataId), "Metadata id '" + metadataId + "' is not a valid " + metadataClass + " identifier");
		return MetadataIdentificationUtils.getMetadataInstance(metadataId);
	}

	/**
	 * Indicates whether the given metadata id appears to identify an instance
	 * of the given metadata class.
	 *
	 * @param metadataClass the fully-qualified name of the expected metadata type (can be blank)
	 * @param metadataId the ID to evaluate (can be blank)
	 * @return true only if the metadata ID appears to be valid
	 */
	public static boolean isValid(final String metadataClass, final String metadataId) {
		return MetadataIdentificationUtils.isIdentifyingInstance(metadataId)
			&& MetadataIdentificationUtils.getMetadataClass(metadataId).equals(metadataClass)
			&& MetadataIdentificationUtils.getMetadataInstance(metadataId).contains(PATH_SUFFIX);
	}
	
	/**
	 * Constructor is private to prevent instantiation
	 * 
	 * @since 1.2.0
	 */
	private PhysicalTypeIdentifierNamingUtils() {}
}
