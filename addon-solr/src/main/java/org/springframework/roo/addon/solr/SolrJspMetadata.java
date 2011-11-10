package org.springframework.roo.addon.solr;

import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.metadata.AbstractMetadataItem;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * Metadata built from {@link SolrWebSearchMetadata}. A single {@link SolrJspMetadata} represents all Solr JSPs for an associated controller.
 * The metadata identifier for a {@link SolrJspMetadata} is the fully qualifier name of the controller, and the source {@link Path}
 * of the controller. This can be created using {@link #createIdentifier(JavaType, Path)}.
 *
 * @author Stefan Schmidt
 * @since 1.1
 *
 */
public class SolrJspMetadata extends AbstractMetadataItem {

	// Constants
	private static final String PROVIDES_TYPE_STRING = SolrJspMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);

	// Fields
	private final SolrWebSearchMetadata solrWebSearchMetadata;

	public SolrJspMetadata(final String identifier, final SolrWebSearchMetadata solrWebSearchMetadata) {
		super(identifier);
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");
		Assert.notNull(solrWebSearchMetadata, "Solr web search metadata required");
		this.solrWebSearchMetadata = solrWebSearchMetadata;
	}

	@Override
	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("identifier", getId());
		tsc.append("valid", valid);
		tsc.append("solr jsp scaffold metadata id", solrWebSearchMetadata.getId());
		return tsc.toString();
	}

	public static String getMetadataIdentiferType() {
		return PROVIDES_TYPE;
	}
	
	public static String createIdentifier(final JavaType javaType, final LogicalPath path) {
		return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
	}

	public static JavaType getJavaType(final String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getJavaType(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static LogicalPath getPath(final String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static boolean isValid(final String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.isValid(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}
}
