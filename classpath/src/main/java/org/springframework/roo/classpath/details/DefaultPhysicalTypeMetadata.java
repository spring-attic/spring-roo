package org.springframework.roo.classpath.details;

import org.apache.commons.lang3.Validate;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.itd.ItdMetadataProvider;
import org.springframework.roo.metadata.AbstractMetadataItem;
import org.springframework.roo.model.JavaType;

/**
 * The default {@link PhysicalTypeMetadata} implementation.
 */
public class DefaultPhysicalTypeMetadata extends AbstractMetadataItem implements
        PhysicalTypeMetadata {

    private final ClassOrInterfaceTypeDetails cid;
    private final String physicalLocationCanonicalPath;

    /**
     * Constructor
     * 
     * @param metadataIdentificationString the ID to assign this
     *            {@link org.springframework.roo.metadata.MetadataItem} (must
     *            satisfy {@link PhysicalTypeIdentifier#isValid(String)})
     * @param physicalLocationCanonicalPath the canonical path of the file
     *            containing this Java type (required)
     * @param cid the details of this type (required)
     */
    public DefaultPhysicalTypeMetadata(
            final String metadataIdentificationString,
            final String physicalLocationCanonicalPath,
            final ClassOrInterfaceTypeDetails cid) {
        super(metadataIdentificationString);
        Validate.isTrue(
                PhysicalTypeIdentifier.isValid(metadataIdentificationString),
                "Metadata id '%s' is not a valid physical type identifier",
                metadataIdentificationString);
        Validate.notBlank(physicalLocationCanonicalPath,
                "Physical location canonical path required");
        Validate.notNull(cid, "Class or interface type details required");
        this.cid = cid;
        this.physicalLocationCanonicalPath = physicalLocationCanonicalPath;
    }

    public String getItdCanoncialPath(final ItdMetadataProvider metadataProvider) {
        // Delegate to the correctly spelled method
        return getItdCanonicalPath(metadataProvider);
    }

    public String getItdCanonicalPath(final ItdMetadataProvider metadataProvider) {
        Validate.notNull(metadataProvider, "Metadata provider required");
        final int dropFrom = physicalLocationCanonicalPath.lastIndexOf(".java");
        Validate.isTrue(dropFrom > -1, "Unexpected governor filename format '"
                + physicalLocationCanonicalPath + "'");
        return physicalLocationCanonicalPath.substring(0, dropFrom) + "_Roo_"
                + metadataProvider.getItdUniquenessFilenameSuffix() + ".aj";
    }

    public JavaType getItdJavaType(final ItdMetadataProvider metadataProvider) {
        Validate.notNull(metadataProvider, "Metadata provider required");
        return new JavaType(PhysicalTypeIdentifier.getJavaType(getId())
                .getFullyQualifiedTypeName()
                + "_Roo_"
                + metadataProvider.getItdUniquenessFilenameSuffix());
    }

    public ClassOrInterfaceTypeDetails getMemberHoldingTypeDetails() {
        return cid;
    }

    public String getPhysicalLocationCanonicalPath() {
        return physicalLocationCanonicalPath;
    }

    public JavaType getType() {
        return cid.getName();
    }

    @Override
    public String toString() {
        // Used for example by the "metadata for id" command
        return getClass().getSimpleName() + " for " + cid.getName();
    }
}
