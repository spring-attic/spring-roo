package org.springframework.roo.metadata;

/**
 * Represents an immutable piece of information about other information in the system.
 * 
 * <p>
 * Being immutable, the {@link MetadataItem} instance is only valid for the time at which
 * it was constructed. A {@link MetadataItem} should ensure it registers with the
 * {@link MetadataDependencyRegistry} any other metadata it depends on, and refer to that
 * type for details on how dependency notifications operate.
 * 
 * <p>
 * It is invalid for {@link MetadataItem} implementations to also implement
 * {@link MetadataCache} or {@link MetadataNotificationListener}, as both of these interfaces
 * relate to the maintenance of state information. Given that {@link MetadataItem} is an
 * immutable class, maintenance of such information is naturally unnecessary and instead
 * such maintenance should be performed by the {@link MetadataProvider} or {@link MetadataService}.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public interface MetadataItem {
	
	/**
	 * Obtains the metadata identification string, which must return true if presented to
	 * {@link MetadataIdentificationUtils#isIdentifyingInstance(String)}.
	 * 
	 * @return a string that identifies this metadata instance (never null, empty or non-compliant
	 * with {@link MetadataIdentificationUtils#isIdentifyingInstance(String)})
	 */
	String getId();
	
	/**
	 * A {@link MetadataItem} instance may not be successfully produced at the time of
	 * instantiation, perhaps due to the non-availability of other metadata or potentially
	 * an error in the information read by the {@link MetadataItem} class (for example, 
	 * metadata representing a Java type may assume the availability of an error-free
	 * source code file, but the source code file might contain a syntax error). A
	 * {@link MetadataItem} will indicate whether it is valid via this method.
	 * 
	 * @return whether this metadata is fully complete and valid
	 */
	boolean isValid();
	
}
