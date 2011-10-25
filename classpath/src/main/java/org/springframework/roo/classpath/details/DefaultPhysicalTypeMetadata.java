package org.springframework.roo.classpath.details;

import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.itd.ItdMetadataProvider;
import org.springframework.roo.metadata.AbstractMetadataItem;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.Assert;

/**
 * The default {@link PhysicalTypeMetadata} implementation.
 */
public class DefaultPhysicalTypeMetadata extends AbstractMetadataItem implements PhysicalTypeMetadata {

	// Fields
	private final MemberHoldingTypeDetails memberHoldingTypeDetails;
	private final String physicalLocationCanonicalPath;

	/**
	 * Constructor
	 *
	 * @param metadataId the ID to assign this {@link org.springframework.roo.metadata.MetadataItem} (must satisfy {@link PhysicalTypeIdentifier#isValid(String)})
	 * @param physicalLocationCanonicalPath the canonical path of the file containing this Java type (required)
	 * @param memberHoldingTypeDetails the members of this type (required)
	 */
	public DefaultPhysicalTypeMetadata(final String metadataId, final String physicalLocationCanonicalPath, final MemberHoldingTypeDetails memberHoldingTypeDetails) {
		super(metadataId);
		Assert.isTrue(PhysicalTypeIdentifier.isValid(metadataId), "Metadata id '" + metadataId + "' is not a valid physical type identifier");
		Assert.hasText(physicalLocationCanonicalPath, "Physical location canonical path required");
		Assert.notNull(memberHoldingTypeDetails, "Member holding type details required");
		this.memberHoldingTypeDetails = memberHoldingTypeDetails;
		this.physicalLocationCanonicalPath = physicalLocationCanonicalPath;
	}

	public MemberHoldingTypeDetails getMemberHoldingTypeDetails() {
		return memberHoldingTypeDetails;
	}

	public String getPhysicalLocationCanonicalPath() {
		return physicalLocationCanonicalPath;
	}

	public String getItdCanoncialPath(final ItdMetadataProvider metadataProvider) {
		// Delegate to the correctly spelled method
		return getItdCanonicalPath(metadataProvider);
	}

	public JavaType getItdJavaType(final ItdMetadataProvider metadataProvider) {
		Assert.notNull(metadataProvider, "Metadata provider required");
		return new JavaType(PhysicalTypeIdentifier.getJavaType(getId()).getFullyQualifiedTypeName() + "_Roo_" + metadataProvider.getItdUniquenessFilenameSuffix());
	}
	
	public JavaType getType() {
		return memberHoldingTypeDetails.getName();
	}
	
	@Override
	public String toString() {
		// Used for example by the "metadata for id" command
		return getClass().getSimpleName() + " for " + memberHoldingTypeDetails.getName();
	}

	public String getItdCanonicalPath(final ItdMetadataProvider metadataProvider) {
		Assert.notNull(metadataProvider, "Metadata provider required");
		final int dropFrom = this.physicalLocationCanonicalPath.lastIndexOf(".java");
		Assert.isTrue(dropFrom > -1, "Unexpected governor filename format '" + this.physicalLocationCanonicalPath + "'");
		return this.physicalLocationCanonicalPath.substring(0, dropFrom) + "_Roo_" + metadataProvider.getItdUniquenessFilenameSuffix() + ".aj";
	}
}
