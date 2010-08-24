package org.springframework.roo.metadata;

/**
 * Indicates a service that guarantees to authoritatively provide {@link MetadataItem}s
 * for a particular class of metadata identification strings.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public interface MetadataProvider {

	/**
	 * Indicates the {@link MetadataItem} the provider offers metadata for.
	 * The returned type is a {@link MetadataIdentificationUtils#isIdentifyingClass(String)}.
	 * 
	 * <p>
	 * The value returned by this method must remain identical for the entire lifecycle of a particular
	 * {@link MetadataProvider} instance. It cannot change once it has been returned.
	 * 
	 * @return the metadata identification class this provider is authoritative for (never null)
	 */
	String getProvidesType();
	
	/**
	 * Creates the requested {@link MetadataItem} if possible, returning null if the item cannot be
	 * created or found. 
	 * 
	 * <p>
	 * This method will throw an exception if the caller has provided an invalid input argument.
	 * This would be the case if the input argument is null, empty, does not return true from
	 * {@link MetadataIdentificationUtils#isIdentifyingInstance(String)}, or the requested metadata
	 * identifier is not of the same class as indicated by {@link #getProvidesType()}).
	 * 
	 * @param metadataIdentificationString to acquire (required and must be supported by this provider) 
	 * @return the metadata, or null if the identification was valid but the metadata is unavailable
	 */
	MetadataItem get(String metadataIdentificationString);
}
