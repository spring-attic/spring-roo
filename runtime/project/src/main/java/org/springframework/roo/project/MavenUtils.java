package org.springframework.roo.project;

import java.util.regex.Pattern;

/**
 * Maven-related utility methods.
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public final class MavenUtils {

    /**
     * The separator conventionally used between Maven coordinates (groupId,
     * artifactId, etc.) when specifying an artifact via a single string.
     */
    public static final String COORDINATE_SEPARATOR = ":";

    /**
     * The pattern that a String must match in order to be considered a valid
     * Maven ID (e.g. groupId or artifactId). Copied from
     * org.apache.maven.project.validation.DefaultModelValidator
     */
    public static final Pattern MAVEN_ID_REGEX = Pattern
            .compile("[A-Za-z0-9_\\-.]+");

    /**
     * Indicates whether the given String is a valid Maven ID, i.e. matches
     * {@link #MAVEN_ID_REGEX}
     * 
     * @param id the String to check (can be <code>null</code>)
     * @return see above
     */
    public static boolean isValidMavenId(final String id) {
        return id != null && MAVEN_ID_REGEX.matcher(id).matches();
    }

    /**
     * Constructor is private to prevent instantiation
     */
    private MavenUtils() {
    }
}
