package org.springframework.roo.classpath.details;

import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.itd.ItdMetadataProvider;
import org.springframework.roo.metadata.AbstractMetadataItem;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.Assert;

public class DefaultPhysicalTypeMetadata extends AbstractMetadataItem implements PhysicalTypeMetadata {

	private MemberHoldingTypeDetails memberHoldingTypeDetails;
	private String physicalLocationCanonicalPath;
	
	public DefaultPhysicalTypeMetadata(String metadataIdentificationString, String physicalLocationCanonicalPath, MemberHoldingTypeDetails memberHoldingTypeDetails) {
		super(metadataIdentificationString);
		Assert.isTrue(PhysicalTypeIdentifier.isValid(metadataIdentificationString), "Metadata identification string '" + metadataIdentificationString + "' does not appear to be a valid physical type identifier");
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
	
	public String getItdCanoncialPath(ItdMetadataProvider metadataProvider) {
		Assert.notNull(metadataProvider, "Metadata provider required");
		String governorFileIdentifier = this.getPhysicalLocationCanonicalPath();
		Assert.notNull(governorFileIdentifier, "Unable to determine file identifier for governor");
		int dropFrom = governorFileIdentifier.lastIndexOf(".java");
		Assert.isTrue(dropFrom > -1, "Unexpected governor filename format '" + governorFileIdentifier + "'");
		return governorFileIdentifier.substring(0, dropFrom) + "_Roo_" + metadataProvider.getItdUniquenessFilenameSuffix() + ".aj";
	}

	public JavaType getItdJavaType(ItdMetadataProvider metadataProvider) {
		Assert.notNull(metadataProvider, "Metadata provider required");
		Assert.notNull(metadataProvider, "Metadata provider required");
		return new JavaType(PhysicalTypeIdentifier.getJavaType(getId()).getFullyQualifiedTypeName() + "_Roo_" + metadataProvider.getItdUniquenessFilenameSuffix());
	}

}
