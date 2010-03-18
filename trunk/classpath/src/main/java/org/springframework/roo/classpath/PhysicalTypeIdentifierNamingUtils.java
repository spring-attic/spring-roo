package org.springframework.roo.classpath;

import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.util.Assert;

/**
 * Used to produce metadata identification strings that represent a {@link JavaType} 
 * located in a particular {@link ClassloaderInclusivePath}.
 * 
 * <p>
 * The metadata identification strings separate the path name from the fully qualified
 * type name via the presence of a question mark character ("?"). A question mark is used
 * given it is reserved by {@link Path}.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public abstract class PhysicalTypeIdentifierNamingUtils {
	
	public static final String createIdentifier(String providesType, JavaType javaType, Path path) {
		Assert.notNull(javaType, "Java type required");
		Assert.notNull(path, "Path required");
		return MetadataIdentificationUtils.create(providesType, path.getName() + "?" + javaType.getFullyQualifiedTypeName());
	}

	public static final JavaType getJavaType(String providesType, String metadataIdentificationString) {
		Assert.isTrue(isValid(providesType, metadataIdentificationString), "Metadata identification string '" + metadataIdentificationString + "' does not appear to be a valid physical type identifier");
		String instance = MetadataIdentificationUtils.getMetadataInstance(metadataIdentificationString);
		int index = instance.indexOf("?");
		return new JavaType(instance.substring(index+1));
	}

	public static final Path getPath(String providesType, String metadataIdentificationString) {
		Assert.isTrue(isValid(providesType, metadataIdentificationString), "Metadata identification string '" + metadataIdentificationString + "' does not appear to be a valid physical type identifier");
		String instance = MetadataIdentificationUtils.getMetadataInstance(metadataIdentificationString);
		int index = instance.indexOf("?");
		return new Path(instance.substring(0, index));
	}

	/**
	 * Indicates whether the presented metadata identification string appears to be valid.
	 * 
	 * @param providesType to verify the presented type (required)
	 * @param metadataIdentificationString to evaluate (can be null or empty)
	 * @return true only if the string appears to be valid
	 */
	public static boolean isValid(String providesType, String metadataIdentificationString) {
		if (!MetadataIdentificationUtils.isIdentifyingInstance(metadataIdentificationString)) {
			return false;
		}
		if (!MetadataIdentificationUtils.getMetadataClass(metadataIdentificationString).equals(providesType)) {
			return false;
		}
		return MetadataIdentificationUtils.getMetadataInstance(metadataIdentificationString).contains("?");
	}
}
