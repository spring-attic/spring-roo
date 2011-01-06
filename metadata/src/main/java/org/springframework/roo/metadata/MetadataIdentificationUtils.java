package org.springframework.roo.metadata;

/**
 * Tokenizes metadata identification strings.
 * 
 * <p>
 * Identification strings are used for metadata in order to reduce memory consumption and
 * garbage collection overhead. It also benefits from the immutability and textual representation
 * simplicity that are fundamental to {@link String}s.
 * 
 * <p>
 * Metadata identification strings can identify either (a) a specific {@link MetadataItem} or (b)
 * a class of {@link MetadataItem}. A string representing a specific {@link MetadataItem} is denoted
 * by the presence of a hash character ("#") in the string name. A hash is used because it is not
 * legal to use in a Java type name, is uncommon to use in file system paths, and yet it still looks
 * relatively logical should a human read the metadata identification string.
 * 
 * <p>
 * Any metadata identification string always commences with "MID:" and then the result of calling 
 * {@link Class#getName()}. If there is no hash sign in the metadata identification string, it is 
 * taken as representing a class of {@link MetadataItem}. If there is one of more hash characters in 
 * the metadata identification string, the first hash is taken as denoting the end of the type name. 
 * The hash is then discarded and the remainder of the string is taken as identifying a specific 
 * {@link MetadataItem}. The "MID:" prefix is for convenience of presentation together with providing
 * some basic verification a randomly-presented string is less likely to be parsed as a formal
 * metadata identification string.
 * 
 * <p>
 * This utility class simplifies working with these metadata identification strings. It should not be
 * necessary for any part of the system to manually build or parse these strings.
 *  
 * @author Ben Alex
 * @since 1.0
 *
 */
public abstract class MetadataIdentificationUtils {
	private static final char[] MID_COLON = {'M', 'I', 'D', ':'};

	/**
	 * Indicates whether the argument appears to be a valid metadata identification string.
	 * 
	 * @param metadataIdentificationString to evaluate (can be null or empty)
	 * @return true if the string appears to be a valid metadata identification string
	 */
	public static final boolean isValid(String metadataIdentificationString) {
		// previously: return metadataIdentificationString != null && metadataIdentificationString.startsWith("MID:");
		if (metadataIdentificationString == null) {
			return false;
		}
		if (metadataIdentificationString.length() < 4) {
			return false;
		}
		for (int i = 0; i < 4; i++) {
			if (metadataIdentificationString.charAt(i) != MID_COLON[i]) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Indicates whether the argument appears to represent a particular metadata identification
	 * class. This method returns false if a particular instance is identified.
	 * 
	 * @param metadataIdentificationString to evaluate (can be null or empty)
	 * @return true if the string is identifying a class of {@link MetadataItem}
	 */
	public static final boolean isIdentifyingClass(String metadataIdentificationString) {
		if (!isValid(metadataIdentificationString)) {
			return false;
		}
		return !metadataIdentificationString.contains("#");
	}
	
	/**
	 * Indicates whether the argument appears to represent a specific metadata instance.
	 * 
	 * @param metadataIdentificationString to evaluate (can be null or empty)
	 * @return true if the string is identifying a specific instance of a {@link MetadataItem}
	 */
	public static final boolean isIdentifyingInstance(String metadataIdentificationString) {
		if (!isValid(metadataIdentificationString)) {
			return false;
		}
		return metadataIdentificationString.contains("#");
	}
	
	/**
	 * Indicates the class of metadata a particular string represents. The class will be
	 * returned even if the metadata identification string represents a specific instance.
	 * 
	 * @param metadataIdentificationString to evaluate (can be null or empty)
	 * @return the class only, or null if the identification string is invalid in some way
	 */
	public static final String getMetadataClass(String metadataIdentificationString) {
		if (!isValid(metadataIdentificationString)) {
			return null;
		}
		if (metadataIdentificationString.length() == 4) {
			// Only gave a "MID:", so quit
			return null;
		}
		int index = metadataIdentificationString.indexOf("#");
		if (index == -1) {
			// No specific metadata instance was identified, so return everything except "MID:"
			return metadataIdentificationString.substring(4);
		}
		// A particular instance was identified, so we need to only return the class name portion
		if (metadataIdentificationString.length() == 5) {
			// Only gave "MID:#", so quit
			return null;
		}
		return metadataIdentificationString.substring(4, index);
	}

	/**
	 * Indicates the instance key a particular string represents. If an instance key cannot
	 * be determined, perhaps due to an illegal metadata identification string or the instance
	 * portion of the string being empty, null will be returned
	 * 
	 * @param metadataIdentificationString to evaluate (can be null or empty)
	 * @return the instance only, or null if the identification string is invalid in some way
	 */
	public static final String getMetadataInstance(String metadataIdentificationString) {
		if (!isIdentifyingInstance(metadataIdentificationString)) {
			return null;
		}
		if (metadataIdentificationString.endsWith("#")) {
			// There isn't an instance key in there we can read (eg "MID:xyz#" was given)
			return null;
		}
		int index = metadataIdentificationString.indexOf("#");
		return metadataIdentificationString.substring(index+1);
	}
	
	/**
	 * Creates a class-specific metadata identification string for the presented fully qualified class name.
	 * 
	 * <p>
	 * A fully qualified class name should be acquired using {@link Class#getName()} or equivalent.
	 * 
	 * @param fullyQualifiedClassName to create (can be null or empty)
	 * @return the metadata identification string (may be null if the input was invalid)
	 */
	public static final String create(String fullyQualifiedClassName) {
		if (fullyQualifiedClassName == null || "".equals(fullyQualifiedClassName) || fullyQualifiedClassName.contains("#")) {
			return null;
		}
		return "MID:" + fullyQualifiedClassName;
	}
	
	/**
	 * Creates an instance-specific metadata identification string for the presented class/key pair. 
	 * 
	 * @param fullyQualifiedClassName to create (mandatory, cannot be empty or null)
	 * @param instanceIdentificationKey to create (mandatory, cannot be empty or null)
	 * @return the metadata identification string (never null)
	 */
	public static final String create(String fullyQualifiedClassName, String instanceIdentificationKey) {
		if (instanceIdentificationKey == null || "".equals(instanceIdentificationKey) || fullyQualifiedClassName == null || "".equals(fullyQualifiedClassName) || fullyQualifiedClassName.contains("#")) {
			return null;
		}
		return "MID:" + fullyQualifiedClassName + "#" + instanceIdentificationKey;
	}

}
