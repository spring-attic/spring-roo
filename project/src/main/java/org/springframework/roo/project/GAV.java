package org.springframework.roo.project;

import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

/**
 * The combination of Maven-style groupId, artifactId, and version.
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class GAV implements Comparable<GAV> {

    /**
     * Returns an instance based on the given concatenated Maven coordinates.
     * 
     * @param coordinates the groupId, artifactId, and version, separated by
     *            {@link MavenUtils#COORDINATE_SEPARATOR}
     * @return a non-blank instance
     * @throws IllegalArgumentException if the string is not formatted as
     *             explained above, or if any of the elements are themselves
     *             invalid.
     */
    public static GAV getInstance(final String coordinates) {
        final String[] coordinateArray = ArrayUtils.nullToEmpty(StringUtils
                .split(coordinates, MavenUtils.COORDINATE_SEPARATOR));
        Validate.isTrue(
                coordinateArray.length == 3,
                "Expected three coordinates, but found "
                        + coordinateArray.length + ": "
                        + Arrays.toString(coordinateArray)
                        + "; did you use the '"
                        + MavenUtils.COORDINATE_SEPARATOR + "' separator?");
        return new GAV(coordinateArray[0], coordinateArray[1],
                coordinateArray[2]);
    }

    private final String artifactId;
    private final String groupId;
    private final String version;

    /**
     * Constructor
     * 
     * @param groupId must be a valid Maven ID
     * @param artifactId must be a valid Maven ID
     * @param version cannot be blank
     */
    public GAV(final String groupId, final String artifactId,
            final String version) {
        // Check
        Validate.isTrue(MavenUtils.isValidMavenId(groupId),
                "Invalid groupId '%s'", groupId);
        Validate.isTrue(MavenUtils.isValidMavenId(artifactId),
                "Invalid artifactId '%s'", artifactId);
        Validate.notBlank(version, "Version is required for %s:%s", groupId,
                artifactId);

        // Assign
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    public int compareTo(final GAV other) {
        Validate.notNull(other, "Cannot compare %s to null", this);
        int result = groupId.compareTo(other.getGroupId());
        if (result == 0) {
            result = artifactId.compareTo(other.getArtifactId());
        }
        if (result == 0) {
            result = version.compareTo(other.getVersion());
        }
        return result;
    }

    @Override
    public boolean equals(final Object other) {
        return other == this || other instanceof GAV
                && compareTo((GAV) other) == 0;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public int hashCode() {
        return artifactId.hashCode();
    }

    @Override
    public String toString() {
        // For debugging
        return StringUtils.join(
                ArrayUtils.toArray(groupId, artifactId, version), ":");
    }
}
