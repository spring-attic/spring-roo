package org.springframework.roo.metadata;

import org.apache.commons.lang3.StringUtils;

/**
 * Utility methods relating to metadata identification strings.
 * <p>
 * We use identification strings for metadata in order to reduce memory
 * consumption and garbage collection overhead. Strings also have the advantage
 * of being immutable and easy to display as text.
 * <p>
 * Metadata identification strings can identify either:
 * <ul>
 * <li>a class of {@link MetadataItem} (in which case
 * {@link #isIdentifyingClass(String)} returns <code>true</code>), or</li>
 * <li>a specific instance of such a class (in which case
 * {@link #isIdentifyingInstance(String)} returns <code>true</code>)</li>
 * </ul>
 * 
 * @author Ben Alex
 * @since 1.0
 */
public final class MetadataIdentificationUtils {

    /*
     * This delimiter was chosen because it never appears in a Java type name,
     * is uncommon to use in file system paths, and looks OK to a human in a
     * metadata id string. The first instance of this character in a given MID
     * separates the metadata class name from the name of the project type to
     * which the metadata applies.
     */
    static final String INSTANCE_DELIMITER = "#";

    // All MIDs start with these characters
    private static final char[] MID_PREFIX_CHARACTERS = { 'M', 'I', 'D', ':' };

    static final String MID_PREFIX = String.valueOf(MID_PREFIX_CHARACTERS);

    private static final int MID_PREFIX_LENGTH = MID_PREFIX_CHARACTERS.length;

    /**
     * Returns the class-level ID for the given type of metadata
     * 
     * @param metadataClass the metadata class for which to create an ID (can be
     *            <code>null</code>)
     * @return a non-blank metadata ID, or <code>null</code> if a
     *         <code>null</code> class was given
     * @since 1.2.0
     */
    public static String create(final Class<?> metadataClass) {
        if (metadataClass == null) {
            return null;
        }
        return create(metadataClass.getName());
    }

    /**
     * Creates a class-specific metadata id for the given fully qualified class
     * name.
     * <p>
     * You can acquire a fully qualified class name using
     * {@link Class#getName()}, although it's more typesafe to call
     * {@link #create(Class)}.
     * 
     * @param fullyQualifiedClassName to create (can be null or empty)
     * @return the metadata identification string (may be null if the input was
     *         invalid)
     */
    public static String create(final String fullyQualifiedClassName) {
        if (StringUtils.isBlank(fullyQualifiedClassName)
                || fullyQualifiedClassName.contains(INSTANCE_DELIMITER)) {
            return null;
        }
        return MID_PREFIX + fullyQualifiedClassName;
    }

    /**
     * Creates an instance-specific metadata identification string for the
     * presented class/key pair.
     * 
     * @param fullyQualifiedMetadataClass
     * @param instanceIdentificationKey
     * @return <code>null</code> if either of the given strings is blank or the
     *         metadata class name is not well-formed
     */
    public static String create(final String fullyQualifiedMetadataClass,
            final String instanceIdentificationKey) {
        if (StringUtils.isBlank(instanceIdentificationKey)
                || StringUtils.isBlank(fullyQualifiedMetadataClass)
                || fullyQualifiedMetadataClass.contains(INSTANCE_DELIMITER)) {
            return null;
        }
        final StringBuilder mid = new StringBuilder();
        mid.append(MID_PREFIX);
        mid.append(fullyQualifiedMetadataClass);
        mid.append(INSTANCE_DELIMITER);
        mid.append(instanceIdentificationKey);
        return mid.toString();
    }

    /**
     * Indicates the class of metadata a particular string represents. The class
     * will be returned even if the metadata identification string represents a
     * specific instance.
     * 
     * @param metadataId to evaluate (can be null or empty)
     * @return the class only, or null if the identification string is invalid
     *         in some way
     */
    public static String getMetadataClass(final String metadataId) {
        if (!isValid(metadataId)
                || metadataId.equals(MID_PREFIX + INSTANCE_DELIMITER)) {
            return null;
        }
        final int delimiterIndex = metadataId.indexOf(INSTANCE_DELIMITER);
        if (delimiterIndex == -1) {
            // No specific metadata instance was identified, so return
            // everything except "MID:"
            return metadataId.substring(MID_PREFIX_LENGTH);
        }
        // A particular instance was identified, so we only return the instance
        // name part
        return metadataId.substring(MID_PREFIX_LENGTH, delimiterIndex);
    }

    /**
     * Returns the ID of the given metadata's class.
     * 
     * @param metadataId the metadata ID for which to return the class ID (can
     *            be blank)
     * @return <code>null</code> if a blank ID is given, otherwise a valid
     *         class-level ID
     * @since 1.2.0
     */
    public static String getMetadataClassId(final String metadataId) {
        return create(getMetadataClass(metadataId));
    }

    /**
     * Returns the instance key from the given metadata instance ID.
     * 
     * @param metadataId the MID to evaluate (can be blank)
     * @return the instance ID only, or <code>null</code> if the identification
     *         string is invalid in some way
     */
    public static String getMetadataInstance(final String metadataId) {
        if (isIdentifyingInstance(metadataId)) {
            return metadataId
                    .substring(metadataId.indexOf(INSTANCE_DELIMITER) + 1);
        }
        return null;
    }

    /**
     * Indicates whether the argument appears to represent a particular metadata
     * identification class. This method returns false if a particular instance
     * is identified.
     * 
     * @param metadataIdentificationString to evaluate (can be null or empty)
     * @return true if the string is identifying a class of {@link MetadataItem}
     */
    public static boolean isIdentifyingClass(
            final String metadataIdentificationString) {
        return isValid(metadataIdentificationString)
                && !metadataIdentificationString.contains(INSTANCE_DELIMITER);
    }

    /**
     * Indicates whether the argument appears to represent a specific metadata
     * instance.
     * 
     * @param metadataIdentificationString to evaluate (can be null or empty)
     * @return true if the string is identifying a specific instance of a
     *         {@link MetadataItem}
     */
    public static boolean isIdentifyingInstance(
            final String metadataIdentificationString) {
        return isValid(metadataIdentificationString)
                && metadataIdentificationString.contains(INSTANCE_DELIMITER)
                && !metadataIdentificationString.endsWith(INSTANCE_DELIMITER);
    }

    /**
     * Indicates whether the argument is a well-formed metadata identification
     * string. This does not guarantee that it is valid, i.e. that the
     * identified metadata actually exists or could ever exist.
     * 
     * @param metadataIdentificationString to evaluate (can be null or empty)
     * @return <code>true</code> if the string appears to be a valid metadata
     *         identification string
     */
    public static boolean isValid(final String metadataIdentificationString) {
        /*
         * According to the first comment on ROO-1932, the algorithm below is an
         * optimisation over simply checking for null and calling
         * String#startsWith().
         */
        if (metadataIdentificationString == null
                || metadataIdentificationString.length() <= MID_PREFIX_LENGTH) {
            return false;
        }
        for (int i = 0; i < MID_PREFIX_LENGTH; i++) {
            if (metadataIdentificationString.charAt(i) != MID_PREFIX_CHARACTERS[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Constructor is private to prevent instantiation
     */
    private MetadataIdentificationUtils() {
    }
}
