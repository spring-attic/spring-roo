package org.springframework.roo.addon.web.mvc.jsp;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.addon.web.mvc.controller.scaffold.WebScaffoldAnnotationValues;
import org.springframework.roo.addon.web.mvc.controller.scaffold.WebScaffoldMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.metadata.AbstractMetadataItem;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;

/**
 * Metadata built from {@link WebScaffoldMetadata}. A single {@link JspMetadata}
 * represents all JSPs for an associated controller. The metadata identifier for
 * a {@link JspMetadata} is the fully qualifier name of the controller, and the
 * source {@link Path} of the controller. This can be created using
 * {@link #createIdentifier(JavaType, Path)}.
 * 
 * @author Stefan Schmidt
 * @author Ben Alex
 * @since 1.0
 */
public class JspMetadata extends AbstractMetadataItem {

    private static final String PROVIDES_TYPE_STRING = JspMetadata.class
            .getName();
    private static final String PROVIDES_TYPE = MetadataIdentificationUtils
            .create(PROVIDES_TYPE_STRING);

    public static String createIdentifier(final JavaType javaType,
            final LogicalPath path) {
        return PhysicalTypeIdentifierNamingUtils.createIdentifier(
                PROVIDES_TYPE_STRING, javaType, path);
    }

    public static JavaType getJavaType(final String metadataIdentificationString) {
        return PhysicalTypeIdentifierNamingUtils.getJavaType(
                PROVIDES_TYPE_STRING, metadataIdentificationString);
    }

    public static String getMetadataIdentiferType() {
        return PROVIDES_TYPE;
    }

    public static LogicalPath getPath(final String metadataIdentificationString) {
        return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING,
                metadataIdentificationString);
    }

    public static boolean isValid(final String metadataIdentificationString) {
        return PhysicalTypeIdentifierNamingUtils.isValid(PROVIDES_TYPE_STRING,
                metadataIdentificationString);
    }

    private final WebScaffoldAnnotationValues annotationValues;

    private final WebScaffoldMetadata webScaffoldMetadata;

    public JspMetadata(final String identifier,
            final WebScaffoldMetadata webScaffoldMetadata) {
        super(identifier);
        Validate.isTrue(
                isValid(identifier),
                "Metadata identification string '%s' does not appear to be a valid",
                identifier);
        Validate.notNull(webScaffoldMetadata, "Web scaffold metadata required");

        this.webScaffoldMetadata = webScaffoldMetadata;
        annotationValues = webScaffoldMetadata.getAnnotationValues();
    }

    public WebScaffoldAnnotationValues getAnnotationValues() {
        return annotationValues;
    }

    @Override
    public String toString() {
        final ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("identifier", getId());
        builder.append("valid", valid);
        builder.append("web scaffold metadata id", webScaffoldMetadata.getId());
        return builder.toString();
    }
}
