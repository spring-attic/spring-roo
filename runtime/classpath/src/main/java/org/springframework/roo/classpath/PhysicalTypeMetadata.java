package org.springframework.roo.classpath;

import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.itd.ItdMetadataProvider;
import org.springframework.roo.classpath.itd.MemberHoldingTypeDetailsMetadataItem;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.metadata.MetadataProvider;
import org.springframework.roo.model.JavaType;

/**
 * The metadata for a Java type in the user's project. Excludes any members
 * introduced via an inter-type declaration (ITD) or other bytecode modification
 * technique.
 * 
 * @author Ben Alex
 * @since 1.0
 * @see PhysicalTypeMetadataProvider
 * @see MemberDetailsScanner
 */
public interface PhysicalTypeMetadata extends
        MemberHoldingTypeDetailsMetadataItem<ClassOrInterfaceTypeDetails> {

    /**
     * Obtains the canonical file path to where an ITD can be emitted for this
     * physical Java type.
     * 
     * @param metadataProvider so the
     *            {@link ItdMetadataProvider#getItdUniquenessFilenameSuffix()}
     *            can be queried (never null)
     * @return a full file path that can be used to produce an ITD (never null)
     * @deprecated use {@link #getItdCanonicalPath(ItdMetadataProvider)} instead
     *             (fixes typo)
     */
    @Deprecated
    String getItdCanoncialPath(ItdMetadataProvider metadataProvider);

    /**
     * Obtains the canonical file path to where an ITD can be emitted for this
     * physical Java type.
     * 
     * @param metadataProvider the {@link MetadataProvider} that produces the
     *            ITD in question (never null)
     * @return a full file path that can be used to produce an ITD (never null)
     * @since 1.2.0
     */
    String getItdCanonicalPath(ItdMetadataProvider metadataProvider);

    /**
     * Obtains the {@link JavaType} which represents an ITD for this physical
     * Java type.
     * 
     * @param metadataProvider so the
     *            {@link ItdMetadataProvider#getItdUniquenessFilenameSuffix()}
     *            can be queried (never null)
     * @return the {@link JavaType} applicable for this ITD (never null)
     */
    JavaType getItdJavaType(ItdMetadataProvider metadataProvider);

    /**
     * @return the location of the disk file containing this resource, in
     *         canonical name format (never null)
     */
    String getPhysicalLocationCanonicalPath();

    /**
     * Returns the Java type for this physical type
     * 
     * @return a non-<code>null</code> type
     * @since 1.2.0
     */
    JavaType getType();
}
