package org.springframework.roo.classpath;

import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.util.Assert;

/**
 * Provides string manipulation functions for {@link PhysicalTypeMetadata}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public final class PhysicalTypeIdentifier {
	
	// Constants
	private static final String PROVIDES_TYPE_STRING = PhysicalTypeIdentifier.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);
	
	public static final String getMetadataIdentiferType() {
		return PROVIDES_TYPE;
	}
	
	/**
	 * Creates an identifier from the given arguments
	 * 
	 * @param javaType, assumed to be in {@link Path#SRC_MAIN_JAVA} (required)
	 * @return a non-blank ID
	 * @since 1.2.0
	 */
	public static final String createIdentifier(final JavaType javaType) {
		return createIdentifier(javaType, Path.SRC_MAIN_JAVA);
	}
	
	/**
	 * Creates an identifier from the given arguments
	 * 
	 * @param javaType (required)
	 * @param path (required)
	 * @return a non-blank ID
	 */
	public static final String createIdentifier(final JavaType javaType, final Path path) {
		return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
	}

	public static final JavaType getJavaType(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getJavaType(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static final Path getPath(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static boolean isValid(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.isValid(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}
	
	public static String getFriendlyName(String metadataIdentificationString) {
		Assert.isTrue(isValid(metadataIdentificationString), "Invalid metadata identification string '" + metadataIdentificationString + "' provided");
		return getPath(metadataIdentificationString) + "/" + getJavaType(metadataIdentificationString);
	}
	
	/**
	 * Constructor is private to prevent instantiation
	 */
	private PhysicalTypeIdentifier() {}
}
