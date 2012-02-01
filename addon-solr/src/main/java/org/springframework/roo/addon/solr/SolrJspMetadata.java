package org.springframework.roo.addon.solr;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.metadata.AbstractMetadataItem;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;

/**
 * Metadata built from {@link SolrWebSearchMetadata}. A single
 * {@link SolrJspMetadata} represents all Solr JSPs for an associated
 * controller. The metadata identifier for a {@link SolrJspMetadata} is the
 * fully qualifier name of the controller, and the source {@link Path} of the
 * controller. This can be created using
 * {@link #createIdentifier(JavaType, Path)}.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
public class SolrJspMetadata extends AbstractMetadataItem {

    private static final String PROVIDES_TYPE_STRING = SolrJspMetadata.class
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

    private final SolrWebSearchMetadata solrWebSearchMetadata;

    public SolrJspMetadata(final String identifier,
            final SolrWebSearchMetadata solrWebSearchMetadata) {
        super(identifier);
        Validate.isTrue(isValid(identifier), "Metadata identification string '"
                + identifier + "' does not appear to be a valid");
        Validate.notNull(solrWebSearchMetadata,
                "Solr web search metadata required");
        this.solrWebSearchMetadata = solrWebSearchMetadata;
    }

    @Override
    public String toString() {
        final ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("identifier", getId());
        builder.append("valid", valid);
        builder.append("solr jsp scaffold metadata id",
                solrWebSearchMetadata.getId());
        return builder.toString();
    }
}
