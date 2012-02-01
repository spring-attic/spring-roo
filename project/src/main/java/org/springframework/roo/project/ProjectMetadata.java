package org.springframework.roo.project;

import java.io.File;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.metadata.AbstractMetadataItem;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.project.maven.Pom;

/**
 * The metadata for a module within the user's project. A simple project will
 * have one instance of this class, whereas a multi-module project will have
 * several.
 * 
 * @since 1.0
 */
public class ProjectMetadata extends AbstractMetadataItem {

    static final String MODULE_SEPARATOR = "?";
    static final String PROJECT_MID_PREFIX = MetadataIdentificationUtils
            .create(ProjectMetadata.class.getName(), "the_project");

    public static String getModuleName(final String metadataIdentificationString) {
        if (metadataIdentificationString.contains(MODULE_SEPARATOR)) {
            return StringUtils.substringAfterLast(metadataIdentificationString,
                    MODULE_SEPARATOR);
        }
        return "";
    }

    /**
     * Returns the metadata ID for the project-level metadata of the given
     * module.
     * 
     * @param moduleName the fully-qualified module name, separated by
     *            {@link File#separator} and/or "/" if different; can be blank
     *            for the root or only module
     * @return a non-blank MID
     */
    public static String getProjectIdentifier(final String moduleName) {
        final StringBuilder sb = new StringBuilder(PROJECT_MID_PREFIX);
        if (StringUtils.isNotBlank(moduleName)) {
            sb.append(MODULE_SEPARATOR).append(
                    moduleName.replace("/", File.separator));
        }
        return sb.toString();
    }

    public static boolean isValid(final String metadataIdentificationString) {
        return metadataIdentificationString.startsWith(PROJECT_MID_PREFIX);
    }

    private final Pom pom;

    /**
     * Constructor
     * 
     * @param pom the POM for this module of the project (required)
     */
    public ProjectMetadata(final Pom pom) {
        super(getProjectIdentifier(pom.getModuleName()));
        Validate.notNull(pom, "POM is required");
        this.pom = pom;
    }

    public String getModuleName() {
        return pom.getModuleName();
    }

    public Pom getPom() {
        return pom;
    }

    @Override
    public final String toString() {
        final ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("identifier", getId());
        builder.append("valid", isValid());
        builder.append("pom", pom);
        return builder.toString();
    }
}
