package org.springframework.roo.classpath.itd;

import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.PhysicalTypeMetadataProvider;
import org.springframework.roo.metadata.MetadataProvider;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.project.Path;

/**
 * Indicates a {@link MetadataProvider} that supports ITDs.
 * <p>
 * ITD usage in ROO adopts some conventions to facilitate ease of use.
 * <p>
 * The first requirement is that metadata identification is by way of the
 * {@link PhysicalTypeIdentifierNamingUtils} if the type that will receive any
 * potential introduction. It is important to recognize that implementations may
 * not actually introduce members via a traditional ITD but instead may place
 * the methods directly in the physical type. This is a supported usage pattern
 * and is highly compatible with using the aforementioned metadata
 * identification approach.
 * <p>
 * The second requirement is that if an implementation wishes to create an ITD
 * it does so strictly in accordance with the following compilation unit file
 * placement and naming convention. The convention is that all ITD compilation
 * units must be placed in the same source directory as the
 * {@link PhysicalTypeIdentifierNamingUtils} used for metadata identification.
 * Secondly, the ITD compilation unit must adopt an identical filename as that
 * used by the {@link PhysicalTypeIdentifierNamingUtils}, except that the
 * extension must be ".aj" and there must be a suffix immediately following the
 * file name (but prior to the extension). The suffix is composed of "_Roo_"
 * plus an implementation-specific string, as returned by
 * {@link #getItdUniquenessFilenameSuffix()}. For example, consider a
 * {@link PhysicalTypeIdentifierNamingUtils} for com/foo/Bar.java within
 * {@link Path#SRC_MAIN_JAVA} and a result of
 * {@link #getItdUniquenessFilenameSuffix()} being "Jpa". This would indicate an
 * ITD filename within {@link Path#SRC_MAIN_JAVA} of "com/foo/Bar_Roo_Jpa.aj".
 * <p>
 * The third requirement is that implementations can assume
 * {@link ItdFileDeletionService} will automatically eliminate any unnecessary
 * ITDs that no longer have a {@link PhysicalTypeIdentifierNamingUtils} that
 * could potentially receive them. Conversely, if a
 * {@link PhysicalTypeIdentifierNamingUtils} does exist, it is required that the
 * implementation will delete any unnecessary ITDs if the ITD should no longer
 * exist and also monitor that ITD for changes.
 * <p>
 * A recommendation is that implementations listen to
 * {@link PhysicalTypeMetadataProvider} so as to be notified of any new
 * {@link PhysicalTypeMetadata} that becomes available. Implementations should
 * consider whether the {@link PhysicalTypeMetadata} represents an instance that
 * should have ITD-specific metadata created. If so, the implementation should
 * create a metadata instance and cause that instance to monitor the
 * {@link PhysicalTypeMetadata} directly. The {@link ItdMetadataProvider} should
 * instantiate an ITD metadata instance with both the
 * {@link PhysicalTypeMetadata} it is monitoring, plus
 * {@link org.springframework.roo.file.monitor.polling.PollingFileMonitorService}
 * for the .aj it should monitor (even if the .aj does not yet exist, because
 * the metadata will create it).
 * 
 * @author Ben Alex
 * @since 1.0
 */
public interface ItdMetadataProvider extends MetadataProvider {

    /**
     * Obtains an identifier that would be validly recognized by this
     * {@link ItdMetadataProvider} instance. The identifier must represent the
     * presented physical Java type identifier.
     * <p>
     * The presented physical Java type identifier need not presently exist in
     * the {@link MetadataService}. Implementations must not rely on the
     * metadata being available at this moment. Implementations by returning a
     * value from this method do not guarantee that metadata for the returned
     * identifier will subsequently made available. As such this method is a
     * basic conversion method and shouldn't perform any analysis.
     * 
     * @param physicalJavaTypeIdentifier to convert into a local metadata
     *            identifier (required)
     * @return an identifier acceptable to this provider (must not return null
     *         or an empty string)
     */
    String getIdForPhysicalJavaType(String physicalJavaTypeIdentifier);

    /**
     * Returns the suffix that makes filenames unique for this implementation.
     * This suffix is appended to the end of the
     * {@link PhysicalTypeIdentifierNamingUtils} filename + "_Roo_" portion.
     * This suffix should not contain any periods and as such does not represent
     * the filename's extension.
     * 
     * @return the filename suffix that makes ITDs produced by this
     *         implementation unique (cannot be null or an empty string)
     */
    String getItdUniquenessFilenameSuffix();
}
